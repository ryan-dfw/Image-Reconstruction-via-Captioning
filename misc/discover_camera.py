import socket

def discover_camera():
    # SSDP request to find cameras with the ScalarWebAPI
    ssdp_request = (
        'M-SEARCH * HTTP/1.1\r\n'
        'HOST: 239.255.255.250:1900\r\n'
        'MAN: "ssdp:discover"\r\n'
        'MX: 1\r\n'
        'ST: urn:schemas-sony-com:service:ScalarWebAPI:1\r\n'
        'USER-AGENT: Python/3.x\r\n\r\n'
    )

    # Create a UDP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.settimeout(5)  # Set timeout for responses
    sock.sendto(ssdp_request.encode('utf-8'), ("239.255.255.250", 1900))  # Multicast address for SSDP

    print("Searching for cameras...")
    try:
        while True:
            data, addr = sock.recvfrom(1024)  # Buffer size is 1024 bytes
            print(f"Response from {addr}:\n{data.decode('utf-8')}")
    except socket.timeout:
        print("Search complete. No more responses.")
    finally:
        sock.close()

if __name__ == "__main__":
    discover_camera()
