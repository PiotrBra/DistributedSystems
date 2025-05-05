import grpc
import weather_pb2 as event_subscription_pb2
import weather_pb2_grpc as event_subscription_pb2_grpc
import uuid
import threading

SERVER_ADDRESS = 'localhost:50051'
CLIENT_ID = f'Client-Python-{uuid.uuid4()}'

# Store active subscriptions: city -> {id, stream, thread}
active_subscriptions = {}


def subscribe_weather(stub, city):
    """
    Starts a subscription for weather updates in the given city.
    Returns the subscription ID.
    """
    subscription_id = f'{CLIENT_ID}-WEATHER_UPDATE-{uuid.uuid4()}'
    criteria = event_subscription_pb2.SubscriptionCriteria(target_identifier=city)
    request = event_subscription_pb2.SubscriptionRequest(
        client_subscription_id=subscription_id,
        event_type=event_subscription_pb2.EventType.WEATHER_UPDATE,
        criteria=criteria
    )
    print(f'[{CLIENT_ID}] Subscribing to weather for {city} (ID: {subscription_id})')
    response_stream = stub.Subscribe(request)

    # Thread to listen for notifications
    def listen():
        try:
            for notification in response_stream:
                handle_notification(notification)
        except grpc.RpcError as e:
            print(f'[{CLIENT_ID}] Stream for {city} ended: {e.code()} - {e.details()}')

    thread = threading.Thread(target=listen, daemon=True)
    thread.start()

    # Store subscription
    active_subscriptions[city] = {
        'id': subscription_id,
        'stream': response_stream,
        'thread': thread
    }
    return subscription_id


def unsubscribe_weather(stub, city):
    """
    Cancels subscription for the given city.
    Calls Unsubscribe RPC and cleans up.
    """
    info = active_subscriptions.get(city)
    if not info:
        print(f'[{CLIENT_ID}] Brak aktywnej subskrypcji dla {city}.')
        return False

    subscription_id = info['id']
    request = event_subscription_pb2.UnsubscriptionRequest(
        client_subscription_id=subscription_id
    )
    try:
        response = stub.Unsubscribe(request)
        if response.success:
            print(f'[{CLIENT_ID}] Unsubscribed from {city} (ID: {subscription_id}): {response.message}')
        else:
            print(f'[{CLIENT_ID}] Unsubscribe failed for {city} (ID: {subscription_id}): {response.message}')
    except grpc.RpcError as e:
        print(f'[{CLIENT_ID}] Unsubscribe RPC error for {city} (ID: {subscription_id}): {e.code()} - {e.details()}')

    # Clean up local resources
    del active_subscriptions[city]
    # The listening thread will exit when stream is closed by server
    return True


def handle_notification(notification):
    """
    Process a single EventNotification from server.
    """
    if notification.event_type == event_subscription_pb2.EventType.WEATHER_UPDATE:
        w = notification.weather_update
        print(f"[{CLIENT_ID}] Pogoda dla {w.city}:")
        print(f"  Temp: {w.current_temperature_celsius:.1f}°C, Warunki: {w.current_condition}, Wiatr: {w.wind_speed_kph} km/h")
        if w.forecast:
            print("  Prognoza:")
            for f in w.forecast:
                print(f"    {f.day_description}: {f.condition}, {f.max_temperature_celsius:.1f}/{f.min_temperature_celsius:.1f}°C")
    elif notification.event_type == event_subscription_pb2.EventType.CONCERT_ALERT:
        c = notification.concert_alert
        print(f"[{CLIENT_ID}] Concert alert: {c.artist} @ {c.venue}, {c.city}")
    elif notification.event_type == event_subscription_pb2.EventType.NEWS_FLASH:
        n = notification.news_flash
        print(f"[{CLIENT_ID}] News flash: {n.headline} ({n.source})")
    else:
        print(f"[{CLIENT_ID}] Unknown notification: {notification}")


def run_client_interactive():
    with grpc.insecure_channel(SERVER_ADDRESS) as channel:
        stub = event_subscription_pb2_grpc.EventSubscriptionServiceStub(channel)

        print(f'[{CLIENT_ID}] Witaj! Dostępne polecenia: sub [miasto], unsub [miasto], list, exit.')

        while True:
            parts = input('> ').strip().split()
            if not parts:
                continue
            cmd = parts[0].lower()

            if cmd == 'sub' and len(parts) > 1:
                city = ' '.join(parts[1:])
                if city in active_subscriptions:
                    print(f'[{CLIENT_ID}] Już subskrybujesz {city}.')
                else:
                    subscribe_weather(stub, city)

            elif cmd == 'unsub' and len(parts) > 1:
                city = ' '.join(parts[1:])
                unsubscribe_weather(stub, city)

            elif cmd == 'list':
                if active_subscriptions:
                    print('Aktywne subskrypcje: ' + ', '.join(active_subscriptions.keys()))
                else:
                    print('Brak aktywnych subskrypcji.')

            elif cmd == 'exit':
                print(f'[{CLIENT_ID}] Kończę.')
                break

            else:
                print('Nieznane polecenie.')


if __name__ == '__main__':
    run_client_interactive()
