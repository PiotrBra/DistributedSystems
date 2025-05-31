import pika
from base_client import BasePikaClient
import json
import time
import threading
from config import ADMIN_MONITOR_QUEUE, ADMIN_MONITOR_BINDING_KEY, \
    ADMIN_BROADCAST_TEAMS_RK, ADMIN_BROADCAST_SUPPLIERS_RK, ADMIN_BROADCAST_ALL_RK


class Administrator(BasePikaClient):
    def __init__(self):
        super().__init__("Administrator")
        self._consuming_thread = None
        self._stop_event = threading.Event()

    def _monitor_loop(self):
        """Pętla monitorująca działająca w osobnym wątku."""
        consumer_tag = f"{self.client_name}_monitor"
        print(f"[{consumer_tag}] Wątek monitora uruchomiony.")

        conn, channel = None, None
        while not self._stop_event.is_set():
            try:
                conn, channel = self._create_connection_and_channel()
                self.declare_queue_and_bind_on_channel(channel, ADMIN_MONITOR_QUEUE, ADMIN_MONITOR_BINDING_KEY,
                                                       durable=True, auto_delete=False)
                channel.basic_qos(prefetch_count=1)

                print(f"[{consumer_tag}] Rozpoczęto nasłuchiwanie na '{ADMIN_MONITOR_QUEUE}'.")
                for method_frame, properties, body in channel.consume(ADMIN_MONITOR_QUEUE, auto_ack=True,
                                                                      inactivity_timeout=1):
                    if self._stop_event.is_set():
                        break
                    if method_frame:
                        self._monitor_callback_handler(channel, method_frame, properties, body)

                if self._stop_event.is_set():
                    print(f"[{consumer_tag}] Otrzymano sygnał stop w pętli monitora.")
                    break

            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{consumer_tag}] Błąd połączenia AMQP: {e}. Próba ponownego połączenia za 5s.")
            except Exception as e:
                print(f"[{consumer_tag}] Nieoczekiwany błąd w pętli monitora: {e}. Próba ponownego połączenia za 5s.")
            finally:
                if channel and channel.is_open:
                    channel.close()
                if conn and conn.is_open:
                    conn.close()

            if not self._stop_event.is_set():
                time.sleep(5)

        print(f"[{consumer_tag}] Wątek monitora zakończony.")

    def _monitor_callback_handler(self, ch, method, properties, body):
        try:
            message = json.loads(body.decode())
            print(f"\n[{self.client_name}-MONITOR] Otrzymano (klucz: '{method.routing_key}'): {message}")
        except json.JSONDecodeError:
            print(f"\n[{self.client_name}-MONITOR] Otrzymano nie-JSON (klucz: '{method.routing_key}'): {body.decode()}")

    def _broadcast(self, routing_key, content, msg_type):
        message = {"sender": "Admin", "type": msg_type, "content": content, "timestamp": time.time()}
        self.simple_publish_message(routing_key, message)  # Używa krótkotrwałego połączenia

    def broadcast_to_teams(self, content):
        self._broadcast(ADMIN_BROADCAST_TEAMS_RK, content, "TO_TEAMS")

    def broadcast_to_suppliers(self, content):
        self._broadcast(ADMIN_BROADCAST_SUPPLIERS_RK, content, "TO_SUPPLIERS")

    def broadcast_to_all(self, content):
        self._broadcast(ADMIN_BROADCAST_ALL_RK, content, "TO_ALL")

    def start_monitoring(self):
        if self._consuming_thread and self._consuming_thread.is_alive():
            print(f"[{self.client_name}] Monitorowanie już aktywne.")
            return
        self._stop_event.clear()
        self._consuming_thread = threading.Thread(target=self._monitor_loop)
        self._consuming_thread.daemon = True
        self._consuming_thread.start()
        print(f"[{self.client_name}] Inicjowanie monitorowania systemu...")

    def stop_monitoring(self):
        print(f"[{self.client_name}] Zatrzymywanie monitorowania...")
        self._stop_event.set()
        if self._consuming_thread and self._consuming_thread.is_alive():
            self._consuming_thread.join(timeout=5)
        if self._consuming_thread and self._consuming_thread.is_alive():
            print(f"[{self.client_name}] Wątek monitora nie zakończył się w oczekiwanym czasie.")
        else:
            print(f"[{self.client_name}] Monitorowanie zakończone.")


if __name__ == '__main__':
    admin = Administrator()
    admin.start_monitoring()

    print("Administrator uruchomiony. Monitoruje system.")
    print("Komendy: teams <msg>, suppliers <msg>, all <msg>, exit")

    try:
        while True:
            command_input = input("Admin> ")
            if command_input.lower() == 'exit':
                break
            parts = command_input.split(" ", 1)
            cmd = parts[0].lower()

            message_content = parts[1] if len(parts) > 1 else ""
            if not message_content.strip() and cmd in ["teams", "suppliers", "all"]:
                print("Brak treści wiadomości dla komendy broadcast.")
                continue

            if cmd == "teams":
                admin.broadcast_to_teams(message_content)
            elif cmd == "suppliers":
                admin.broadcast_to_suppliers(message_content)
            elif cmd == "all":
                admin.broadcast_to_all(message_content)
            elif cmd.strip() == "":
                continue
            else:
                print("Nieznana komenda.")
    except KeyboardInterrupt:
        print(f"\n[{admin.client_name}] Przerwanie przez użytkownika...")
    finally:
        admin.stop_monitoring()
        print("Administrator zakończył działanie.")