import socket
import threading

# Ustawienia
HOST = '0.0.0.0'
PORT = 5000  # UDP/TCP

# Lista klientów: tuple (socket_tcp, adres_tcp, nick, udp_addr)
clients = []
clients_lock = threading.Lock()


# --- Obsługa TCP ---

def broadcast_tcp(message, sender_nick):
    """
    Rozsyła wiadomość TCP do wszystkich klientów.
    """
    with clients_lock:
        for client, _, _, _ in clients:
            try:
                client.sendall(f"{sender_nick}: {message}".encode('utf-8'))
            except Exception as e:
                print(f"[TCP] Błąd przy wysyłaniu do klienta: {e}")


def handle_client(conn, addr):
    """
    Obsługuje pojedynczego klienta – odbiera najpierw nick,
    następnie informację o porcie UDP używanym przez klienta oraz wiadomości.
    """
    try:
        conn.sendall("Podaj swój nick: ".encode('utf-8'))
        nick = conn.recv(1024).decode('utf-8').strip()
        if not nick:
            nick = str(addr)
        # Odbieramy informację o porcie UDP
        udp_port_str = conn.recv(1024).decode('utf-8').strip()
        try:
            udp_port = int(udp_port_str)
        except ValueError:
            udp_port = addr[1]
        udp_addr = (addr[0], udp_port)

        with clients_lock:
            clients.append((conn, addr, nick, udp_addr))
        print(f"[TCP] {nick} {addr} (UDP: {udp_addr}) dołączył do czatu.")

        # Obsługa wiadomości TCP
        while True:
            data = conn.recv(1024)
            if not data:
                break
            message = data.decode('utf-8').strip()
            print(f"[TCP] {nick} napisał: {message}")
            broadcast_tcp(message, nick)
    except Exception as e:
        print(f"[TCP] Błąd przy obsłudze klienta {addr}: {e}")
    finally:
        with clients_lock:
            try:
                clients.remove((conn, addr, nick, udp_addr))
            except ValueError:
                pass
        conn.close()
        print(f"[TCP] {nick} {addr} opuścił czat.")


def tcp_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((HOST, PORT))
    server_socket.listen(5)
    print(f"[TCP] Serwer nasłuchuje na porcie {PORT}...")
    while True:
        try:
            conn, addr = server_socket.accept()
            thread = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            thread.start()
        except Exception as e:
            print(f"[TCP] Błąd przy akceptowaniu połączenia: {e}")


# --- Obsługa UDP ---

def broadcast_udp(message, sender_addr, udp_sock):
    """
    Rozsyła wiadomość UDP do wszystkich klientów.
    Jeśli chcemy uniknąć wysyłki z powrotem do nadawcy – można dodać filtr.
    W tej wersji wiadomość jest wysyłana do wszystkich.
    """
    with clients_lock:
        for _, _, nick, client_udp_addr in clients:
            # Opcjonalnie można pominąć nadawcę, jeśli adresy są zgodne:
            if client_udp_addr == sender_addr:
                continue
            try:
                print(f"[UDP] Wysyłam do {nick} na {client_udp_addr}")
                udp_sock.sendto(message, client_udp_addr)
            except Exception as e:
                print(f"[UDP] Błąd przy wysyłaniu do {nick} ({client_udp_addr}): {e}")


def udp_listener():
    udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_sock.bind((HOST, PORT))
    print(f"[UDP] Serwer nasłuchuje na porcie {PORT}...")
    while True:
        try:
            data, addr = udp_sock.recvfrom(1024)
            print(f"[UDP] Odebrano od {addr}: {data.decode('utf-8')}")
            broadcast_udp(data, addr, udp_sock)
        except Exception as e:
            print(f"[UDP] Błąd odbioru: {e}")


def main():
    udp_thread = threading.Thread(target=udp_listener, daemon=True)
    udp_thread.start()
    tcp_server()


if __name__ == "__main__":
    main()
