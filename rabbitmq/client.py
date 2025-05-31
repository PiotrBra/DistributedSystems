# ekipa.py
from base_client import BasePikaClient
import uuid
import json
import time
import threading
from config import ORDER_ROUTING_KEY_PREFIX, CONFIRMATION_ROUTING_KEY_PREFIX, \
    ADMIN_BROADCAST_TEAMS_RK, ADMIN_BROADCAST_ALL_RK


class Team(BasePikaClient):
    def __init__(self, team_name):
        super().__init__(f"Ekipa_{team_name}")
        self.team_name = team_name
        self.team_queue_name = f"q_team_{self.team_name}"
        self._consuming_thread = None
        self._stop_event = threading.Event()

    def _consumer_loop(self):
        """Pętla konsumenta działająca w osobnym wątku."""
        consumer_tag = f"{self.client_name}_consumer"
        print(f"[{consumer_tag}] Wątek konsumenta uruchomiony.")

        conn, channel = None, None
        while not self._stop_event.is_set():
            try:
                conn, channel = self._create_connection_and_channel()
                binding_keys = [
                    f"{CONFIRMATION_ROUTING_KEY_PREFIX}{self.team_name}",
                    ADMIN_BROADCAST_TEAMS_RK,
                    ADMIN_BROADCAST_ALL_RK
                ]
                self.declare_queue_and_bind_on_channel(channel, self.team_queue_name, binding_keys, durable=True,
                                                       auto_delete=False)
                channel.basic_qos(prefetch_count=1)

                print(f"[{consumer_tag}] Rozpoczęto nasłuchiwanie na '{self.team_queue_name}'.")
                # Używamy iteratora zamiast callbacku w pętli, aby lepiej kontrolować zamykanie
                for method_frame, properties, body in channel.consume(self.team_queue_name, auto_ack=True,
                                                                      inactivity_timeout=1):
                    if self._stop_event.is_set():
                        break
                    if method_frame:  # Wiadomość otrzymana
                        self._callback_handler(channel, method_frame, properties, body)
                    # Jeśli inactivity_timeout, pętla się powtórzy, sprawdzając _stop_event

                if self._stop_event.is_set():  # Jeśli pętla przerwana przez stop_event
                    print(f"[{consumer_tag}] Otrzymano sygnał stop w pętli konsumenta.")
                    break  # Wyjdź z pętli while

            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{consumer_tag}] Błąd połączenia AMQP: {e}. Próba ponownego połączenia za 5s.")
            except Exception as e:
                print(f"[{consumer_tag}] Nieoczekiwany błąd w pętli konsumenta: {e}. Próba ponownego połączenia za 5s.")
            finally:
                if channel and channel.is_open:
                    channel.close()
                if conn and conn.is_open:
                    conn.close()

            if not self._stop_event.is_set():
                time.sleep(5)  # Czekaj przed ponowną próbą połączenia (jeśli nie zatrzymano)

        print(f"[{consumer_tag}] Wątek konsumenta zakończony.")

    def _callback_handler(self, ch, method, properties, body):
        message = json.loads(body.decode())
        routing_key = method.routing_key
        print(f"\n[{self.client_name}] Otrzymano (klucz: '{routing_key}'): {message}")

    def send_order(self, equipment_type):
        order_id = str(uuid.uuid4())
        message = {
            "team_name": self.team_name,
            "team_order_id": order_id,
            "equipment_type": equipment_type,
            "reply_to_rk": f"{CONFIRMATION_ROUTING_KEY_PREFIX}{self.team_name}"
        }
        routing_key = f"{ORDER_ROUTING_KEY_PREFIX}{equipment_type}"
        self.simple_publish_message(routing_key, message)  # Używa krótkotrwałego połączenia
        return order_id

    def start_listening(self):
        if self._consuming_thread and self._consuming_thread.is_alive():
            print(f"[{self.client_name}] Nasłuchiwanie już aktywne.")
            return
        self._stop_event.clear()
        self._consuming_thread = threading.Thread(target=self._consumer_loop)
        self._consuming_thread.daemon = True
        self._consuming_thread.start()
        print(f"[{self.client_name}] Inicjowanie nasłuchu na wiadomości...")

    def stop_listening(self):
        print(f"[{self.client_name}] Zatrzymywanie nasłuchu...")
        self._stop_event.set()
        if self._consuming_thread and self._consuming_thread.is_alive():
            self._consuming_thread.join(timeout=5)  # Czekaj na zakończenie wątku
        if self._consuming_thread and self._consuming_thread.is_alive():
            print(f"[{self.client_name}] Wątek konsumenta nie zakończył się w oczekiwanym czasie.")
        else:
            print(f"[{self.client_name}] Nasłuch zakończony.")


if __name__ == '__main__':
    team_name_input = input("Podaj nazwę ekipy (np. Alfa, GorskieSwiry): ")
    if not team_name_input:
        print("Nazwa ekipy nie może być pusta.")
        exit()

    team = Team(team_name_input)
    team.start_listening()

    print(f"Ekipa '{team.team_name}' uruchomiona. Wpisz 'zamow <typ_sprzetu>' lub 'exit'.")

    try:
        while True:
            command = input(f"{team.team_name}> ")
            if command.lower().startswith("zamow "):
                parts = command.split(" ", 1)
                if len(parts) > 1 and parts[1].strip():
                    team.send_order(parts[1].strip())
                else:
                    print("Podaj typ sprzętu, np. 'zamow tlen'")
            elif command.lower() == 'exit':
                break
            elif command.strip() == "":
                continue
            else:
                print("Nieznana komenda.")
    except KeyboardInterrupt:
        print(f"\n[{team.client_name}] Przerwanie przez użytkownika...")
    finally:
        team.stop_listening()
        print(f"Ekipa {team.team_name} zakończyła działanie.")