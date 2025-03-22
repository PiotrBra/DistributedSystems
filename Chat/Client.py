import socket
import threading

# Ustawienia
SERVER_HOST = '127.0.0.1'
PORT = 5000  # UDP/TCP
MULTICAST_GROUP = '224.1.1.1'
MULTICAST_PORT = 5001

def receive_tcp(sock):
    """
    Odbiera wiadomości TCP z serwera.
    """
    while True:
        try:
            data = sock.recv(1024)
            if not data:
                print("[TCP] Połączenie z serwerem zakończone.")
                break
            print(data.decode('utf-8'))
        except Exception as e:
            print(f"[TCP] Błąd odbioru: {e}")
            break


def receive_udp(udp_sock):
    """
    Odbiera wiadomości UDP.
    """
    while True:
        try:
            data, addr = udp_sock.recvfrom(1024)
            print(f"[UDP] {data.decode('utf-8')}")
        except Exception as e:
            print(f"[UDP] Błąd odbioru: {e}")
            break


def receive_multicast(multicast_sock):
    """
    Odbiera wiadomości multicast.
    """
    while True:
        try:
            data, addr = multicast_sock.recvfrom(1024)
            print(f"[Multicast] {data.decode('utf-8')}")
        except Exception as e:
            print(f"[Multicast] Błąd odbioru: {e}")
            break


def main():
    # Połączenie TCP
    tcp_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        tcp_sock.connect((SERVER_HOST, PORT))
    except Exception as e:
        print(f"[TCP] Błąd łączenia z serwerem: {e}")
        return

    # Odbieramy prośbę o nick
    prompt = tcp_sock.recv(1024).decode('utf-8')
    print(prompt, end='')
    nick = input().strip()
    tcp_sock.sendall(nick.encode('utf-8'))

    # Utwórz UDP socket
    udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        udp_sock.bind(('', PORT))
    except Exception as e:
        # Jeśli nie uda się związać z tym portem, użyjemy portu przydzielonego dynamicznie
        print(f"[UDP] Nie udało się zbindować portu {PORT}: {e}. Użyjemy innego portu.")
        udp_sock.bind(('', 0))
    local_udp_port = udp_sock.getsockname()[1]
    print(f"[UDP] Lokalny port UDP: {local_udp_port}")
    # Wysyłamy serwerowi nasz lokalny port UDP (po nicku)
    tcp_sock.sendall(str(local_udp_port).encode('utf-8'))

    # Utwórz multicast socket
    multicast_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    multicast_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    multicast_sock.bind(('', MULTICAST_PORT))
    mreq = socket.inet_aton(MULTICAST_GROUP) + socket.inet_aton('0.0.0.0')
    multicast_sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

    # Wątki odbierające wiadomości TCP, UDP i multicast
    threading.Thread(target=receive_tcp, args=(tcp_sock,), daemon=True).start()
    threading.Thread(target=receive_udp, args=(udp_sock,), daemon=True).start()
    threading.Thread(target=receive_multicast, args=(multicast_sock,), daemon=True).start()

    print("Wpisz wiadomość aby wysłać przez TCP.")
    print("Wpisz 'U' na początku wiadomości aby wysłać wiadomość przez UDP (np. ASCII Art).")
    print("Wpisz 'M' na początku wiadomości aby wysłać wiadomość przez multicast.")

    while True:
        try:
            msg = input()
            if msg.startswith('U'):
                udp_msg = msg[1:].strip()
                udp_sock.sendto(udp_msg.encode('utf-8'), (SERVER_HOST, PORT))
            elif msg.startswith('M'):
                multicast_msg = msg[1:].strip()
                multicast_sock.sendto(multicast_msg.encode('utf-8'), (MULTICAST_GROUP, MULTICAST_PORT))
            else:
                tcp_sock.sendall(msg.encode('utf-8'))
        except Exception as e:
            print(f"Błąd przy wysyłaniu: {e}")
            break

    tcp_sock.close()
    udp_sock.close()
    multicast_sock.close()


if __name__ == "__main__":
    main()