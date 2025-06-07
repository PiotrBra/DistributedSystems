from base_client import BasePikaClient
import uuid
import json
import time
import threading
from config import ORDER_ROUTING_KEY_PREFIX, SUPPLIER_CAPABILITIES, \
    ADMIN_BROADCAST_SUPPLIERS_RK, ADMIN_BROADCAST_ALL_RK, ALL_EQUIPMENT_TYPES


class Supplier(BasePikaClient):
    def __init__(self, supplier_name):
        super().__init__(f"Dostawca_{supplier_name}")
        self.supplier_name = supplier_name
        self.capabilities = SUPPLIER_CAPABILITIES.get(supplier_name, [])
        if not self.capabilities:
            print(f"[{self.client_name}] UWAGA: Brak zdefiniowanych specjalizacji dla dostawcy '{supplier_name}'.")

        self._consumer_threads = []
        self._stop_event = threading.Event()
        self._initial_shared_queue_setup()  # Wywołaj raz przy inicjalizacji

    def _initial_shared_queue_setup(self):
        """
        Wstępna deklaracja kolejek współdzielonych
        oraz kolejki admina dla tego dostawcy.
        To zapewnia, że kolejki istnieją, zanim konsumenci zaczną się do nich podłączać.
        Konsumenci również będą je deklarować na swoich kanałach.
        """
        print(f"[{self.client_name}] Wstępna konfiguracja kolejek...")
        conn, temp_channel = None, None
        try:
            conn, temp_channel = self._create_connection_and_channel()
            # Kolejki zamówień - deklarujemy wszystkie możliwe, konsumpcja wg capabilities
            for equip_type in ALL_EQUIPMENT_TYPES:
                queue_name = f"q_orders_{equip_type}"
                self.declare_queue_and_bind_on_channel(temp_channel, queue_name,
                                                       f"{ORDER_ROUTING_KEY_PREFIX}{equip_type}", durable=True,
                                                       auto_delete=False)

            # Kolejka admina dla tego dostawcy
            admin_queue_name = f"q_supplier_{self.supplier_name}_admin"
            admin_binding_keys = [ADMIN_BROADCAST_SUPPLIERS_RK, ADMIN_BROADCAST_ALL_RK]
            self.declare_queue_and_bind_on_channel(temp_channel, admin_queue_name, admin_binding_keys, durable=True,
                                                   auto_delete=False)
            print(f"[{self.client_name}] Wstępna konfiguracja kolejek zakończona.")
        except Exception as e:
            # Ten błąd jest krytyczny dla startu, ale pozwalamy kontynuować, żeby zobaczyć błędy konsumentów
            print(
                f"[{self.client_name}] KRYTYCZNY BŁĄD podczas wstępnej konfiguracji kolejek: {e}. Dostawca może nie działać poprawnie.")
        finally:
            if temp_channel and temp_channel.is_open:
                temp_channel.close()
            if conn and conn.is_open:
                conn.close()

    def _threaded_consumer_target(self, queue_name, callback_method, is_admin_queue=False):
        """Docelowa funkcja dla wątku konsumenckiego."""
        consumer_id_prefix = f"{self.client_name}_consumer_{queue_name}"
        print(f"[{consumer_id_prefix}] Wątek uruchomiony.")

        conn, channel = None, None
        while not self._stop_event.is_set():  # Pętla dla ponownych prób połączenia
            try:
                conn, channel = self._create_connection_and_channel()
                consumer_tag_for_logs = f"{consumer_id_prefix}_ch{channel.channel_number}"

                # Deklaracja i powiązanie kolejki na nowym kanale (idempotentne)
                if is_admin_queue:
                    binding_keys = [ADMIN_BROADCAST_SUPPLIERS_RK, ADMIN_BROADCAST_ALL_RK]
                    self.declare_queue_and_bind_on_channel(channel, queue_name, binding_keys, durable=True,
                                                           auto_delete=False)
                else:  # Kolejka zamówień
                    equip_type = queue_name.split('_')[-1]  # np. 'tlen' z 'q_orders_tlen'
                    binding_key = f"{ORDER_ROUTING_KEY_PREFIX}{equip_type}"
                    self.declare_queue_and_bind_on_channel(channel, queue_name, binding_key, durable=True,
                                                           auto_delete=False)

                channel.basic_qos(prefetch_count=1)  # Fair dispatch
                print(f"[{consumer_tag_for_logs}] Rozpoczęto nasłuchiwanie na '{queue_name}'.")

                # Użycie channel.consume() jako iteratora pozwala na sprawdzanie stop_event
                for method_frame, properties, body in channel.consume(queue_name, auto_ack=True, inactivity_timeout=1):
                    if self._stop_event.is_set():
                        break  # Przerwij wewnętrzną pętlę konsumpcji
                    if method_frame:  # Jeśli jest wiadomość
                        # Wywołaj odpowiedni callback, przekazując kanał (ch), metodę, właściwości, ciało
                        callback_method(channel, method_frame, properties, body)

                if self._stop_event.is_set():  # Jeśli pętla konsumpcji przerwana przez stop_event
                    print(f"[{consumer_tag_for_logs}] Otrzymano sygnał stop.")
                    break  # Wyjdź z pętli `while not self._stop_event.is_set()`

            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{consumer_id_prefix}] Błąd połączenia AMQP: {e}.")
            except Exception as e:
                print(f"[{consumer_id_prefix}] Nieoczekiwany błąd: {e} (Typ: {type(e)}).")
            finally:
                if channel and channel.is_open:
                    channel.close()
                if conn and conn.is_open:
                    conn.close()

            if not self._stop_event.is_set():
                print(f"[{consumer_id_prefix}] Próba ponownego połączenia za 5 sekund...")
                time.sleep(5)
            else:
                break

        print(f"[{consumer_id_prefix}] Wątek zakończony.")

    def _order_callback(self, ch, method, properties, body):
        order_data = json.loads(body.decode())
        print(
            f"\n[{self.client_name}] Otrzymano zamówienie (klucz: '{method.routing_key}' z kolejki '{method.exchange}->{method.routing_key}'): {order_data}")
        supplier_order_id = str(uuid.uuid4())
        confirmation_message = {
            "team_name": order_data["team_name"],
            "team_order_id": order_data["team_order_id"],
            "equipment_type": order_data["equipment_type"],
            "supplier_name": self.supplier_name,
            "supplier_order_id": supplier_order_id,
            "status": "confirmed"
        }
        reply_routing_key = order_data.get("reply_to_rk")
        if reply_routing_key:
            try:
                self.publish_on_channel(ch, reply_routing_key, confirmation_message)
                print(
                    f"[{self.client_name}] Wysłano potwierdzenie dla {order_data['team_order_id']} do {reply_routing_key}")
            except Exception as e_pub:
                print(
                    f"[{self.client_name}] BŁĄD podczas wysyłania potwierdzenia dla {order_data['team_order_id']}: {e_pub}")
        else:
            print(f"[{self.client_name}] BŁĄD: Brak 'reply_to_rk' w zamówieniu: {order_data}")

    def _admin_callback(self, ch, method, properties, body):
        message = json.loads(body.decode())
        print(f"\n[{self.client_name}] Otrzymano wiadomość admin (klucz: '{method.routing_key}'): {message}")

    def start_consuming(self):
        print(f"[{self.client_name}] Dostawca '{self.supplier_name}' obsługuje: {self.capabilities}")

        # Wątki dla kolejek zamówień wg specjalizacji
        for equip_type in self.capabilities:
            if equip_type not in ALL_EQUIPMENT_TYPES:  # Sprawdzenie, czy typ jest znany
                print(
                    f"[{self.client_name}] UWAGA: Typ sprzętu '{equip_type}' nie jest na liście ALL_EQUIPMENT_TYPES. Pomijam.")
                continue
            queue_name = f"q_orders_{equip_type}"
            thread = threading.Thread(target=self._threaded_consumer_target,
                                      args=(queue_name, self._order_callback, False))
            thread.daemon = True
            self._consumer_threads.append(thread)
            thread.start()

        # Wątek dla kolejki administracyjnej
        admin_queue_name = f"q_supplier_{self.supplier_name}_admin"
        admin_thread = threading.Thread(target=self._threaded_consumer_target,
                                        args=(admin_queue_name, self._admin_callback, True))
        admin_thread.daemon = True
        self._consumer_threads.append(admin_thread)
        admin_thread.start()

        print(f"[{self.client_name}] Wszystkie ({len(self._consumer_threads)}) wątki konsumentów zostały uruchomione.")

    def stop_consuming(self):
        print(f"[{self.client_name}] Zatrzymywanie wszystkich konsumentów...")
        self._stop_event.set()  # Sygnał dla wszystkich wątków, aby zakończyły działanie
        for t in self._consumer_threads:
            if t.is_alive():
                t.join(timeout=7)  # Dajemy wątkom trochę więcej czasu na zamknięcie

        alive_threads_count = sum(1 for t in self._consumer_threads if t.is_alive())
        if alive_threads_count > 0:
            print(f"[{self.client_name}] UWAGA: {alive_threads_count} wątków konsumentów nie zakończyło się poprawnie.")
        else:
            print(f"[{self.client_name}] Wszystkie wątki konsumentów zatrzymane.")


if __name__ == '__main__':
    supplier_name_input = input(f"Podaj nazwę dostawcy (dostępni: {', '.join(SUPPLIER_CAPABILITIES.keys())}): ")
    if supplier_name_input not in SUPPLIER_CAPABILITIES:
        print(f"Nieznana nazwa dostawcy. Dostępni: {', '.join(SUPPLIER_CAPABILITIES.keys())}")
        exit()

    supplier = Supplier(supplier_name_input)
    supplier.start_consuming()

    try:
        # Główny wątek czeka, aż wątki konsumentów zakończą działanie
        # lub można tu dodać logikę np. sprawdzania statusu wątków
        while any(t.is_alive() for t in supplier._consumer_threads):
            time.sleep(1)  # Krótka pauza, aby nie obciążać CPU
            # Można by tu dodać warunek zakończenia pętli, jeśli wszystkie wątki umarły z innego powodu
            if not any(t.is_alive() for t in supplier._consumer_threads) and not supplier._stop_event.is_set():
                print(
                    f"[{supplier.client_name}] Wszystkie wątki konsumentów nieoczekiwanie zakończyły działanie. Zamykanie...")
                break
    except KeyboardInterrupt:
        print(f"\n[{supplier.client_name}] Przerwanie przez użytkownika (Ctrl+C)...")
    finally:
        supplier.stop_consuming()
        print(f"[{supplier.client_name}] Dostawca zakończył działanie.")