# base_pika_client.py
import pika
import json
import time
from config import RABBITMQ_HOST, EXCHANGE_NAME

class BasePikaClient:
    def __init__(self, client_name):
        self.client_name = client_name

    def _create_connection_and_channel(self):
        """Tworzy i zwraca nowe połączenie oraz kanał."""
        max_retries = 5
        retry_delay = 5
        for attempt in range(max_retries):
            try:
                connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
                channel = connection.channel()
                # Deklaracja exchange jest idempotentna
                channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type='topic')
                print(f"[{self.client_name}] Połączono z RabbitMQ i zadeklarowano exchange '{EXCHANGE_NAME}'.")
                return connection, channel
            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{self.client_name}] Nie można połączyć z RabbitMQ: {e}. Próba {attempt + 1}/{max_retries}. Ponawiam za {retry_delay}s...")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                else:
                    raise Exception(f"[{self.client_name}] Nie udało się połączyć z RabbitMQ po {max_retries} próbach.")
        # Ta linia nie powinna być osiągnięta jeśli pętla się zakończy
        raise Exception(f"[{self.client_name}] Nie udało się połączyć z RabbitMQ po {max_retries} próbach.")


    def declare_queue_and_bind_on_channel(self, channel, queue_name, binding_keys, exclusive=False, auto_delete=False, durable=True):
        """Deklaruje kolejkę i jej powiązania na dostarczonym kanale."""
        # Deklaracja kolejki jest idempotentna
        channel.queue_declare(queue=queue_name, exclusive=exclusive, auto_delete=auto_delete, durable=durable)

        if not isinstance(binding_keys, list):
            binding_keys = [binding_keys]

        for bk in binding_keys:
            # Powiązanie jest idempotentne
            channel.queue_bind(exchange=EXCHANGE_NAME, queue=queue_name, routing_key=bk)
            print(f"[{self.client_name}] Kolejka '{queue_name}' powiązana z '{EXCHANGE_NAME}' (na kanale {channel.channel_number}) kluczem '{bk}'.")
        return queue_name

    def publish_on_channel(self, channel, routing_key, message_body):
        """Publikuje wiadomość na dostarczonym kanale."""
        try:
            channel.basic_publish(
                exchange=EXCHANGE_NAME,
                routing_key=routing_key,
                body=json.dumps(message_body),
                properties=pika.BasicProperties(
                    delivery_mode=pika.spec.PERSISTENT_DELIVERY_MODE # Trwałość wiadomości
                )
            )
        except Exception as e:
            print(f"[{self.client_name}] Błąd podczas wysyłania wiadomości na kanale {channel.channel_number}: {e}")
            raise # Rzucamy błąd dalej, aby logika wywołująca mogła zareagować

    def simple_publish_message(self, routing_key, message_body):
        """
        Prosta metoda do opublikowania pojedynczej wiadomości.
        Tworzy własne, krótkotrwałe połączenie i kanał.
        """
        conn, chan = None, None
        try:
            conn, chan = self._create_connection_and_channel()
            self.publish_on_channel(chan, routing_key, message_body)
            print(f"[{self.client_name}] Wysłano (simple_publish) klucz '{routing_key}': {message_body}")
        finally:
            if chan and chan.is_open:
                chan.close()
            if conn and conn.is_open:
                conn.close()