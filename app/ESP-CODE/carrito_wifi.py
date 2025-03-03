import network
import usocket as socket
import uasyncio as asyncio
import ubinascii
import struct
import hashlib  # Importar hashlib

from machine import Pin, PWM

import time

# Pines de los servos
servo1 = PWM(Pin(2), freq=50)   # Servo para mover izquierda/derecha
servo2 = PWM(Pin(4), freq=50)   # Servo para mover arriba/abajo
servo3 = PWM(Pin(13), freq=50)  # Servo para abrir/cerrar la mano

# Función para mover servos
def move_servo(servo, angle):
    duty = int((angle / 180) * 75 + 40)
    duty = max(40, min(duty, 115))  # Asegura que el valor esté dentro del rango
    servo.duty(duty)

# Mover los servos a posición inicial
move_servo(servo1, 90)  # Posición media para izquierda/derecha
move_servo(servo2, 0)  # Posición media para arriba/abajo
move_servo(servo3, 90)  # Mano abierta

# Conexión Wi-Fi
ssid = 'Totalplay-D1AA'  # Cambia por el nombre de tu red Wi-Fi
password = 'TOTAL_PLAYc99'  # Cambia por la contraseña de tu red

station = network.WLAN(network.STA_IF)
station.active(True)
station.connect(ssid, password)

# Esperar hasta que se conecte
while not station.isconnected():
    time.sleep(1)
    
ip = station.ifconfig()[0]

print(f"Conectado, IP: {ip}")

# Estado inicial de los servos
servo1_angle = 90  # Posición inicial media
servo2_angle = 90  # Posición inicial media
servo3_angle = 90  # Mano abierta

def car_functions(msg):
    if msg == "LED_ON":
        return "Encendiendo LED"
    elif msg == "LED_OFF":
        return "Apagando LED"
    
    elif msg == "s1_open":
        move_servo(servo1, 180)
        return "Abriendo servo 1"
    elif msg == "s1_close":
        move_servo(servo1, 90)
        return "Cerrando servo 1"
    elif msg == "s2_open":
        move_servo(servo2, 180)
        return "Abriendo servo 2"
    elif msg == "s2_close":
        move_servo(servo2, 90)
        return "Cerrando servo 2"

# Función para manejar la conexión WebSocket
async def handle_client(reader, writer):
    try:
        # Leer el handshake de WebSocket
        request = await reader.read(1024)
        headers = request.decode().split("\r\n")
        
        # Obtener clave de WebSocket
        websocket_key = None
        for header in headers:
            if "Sec-WebSocket-Key" in header:
                websocket_key = header.split(": ")[1]
                break
        
        if not websocket_key:
            writer.close()
            await writer.wait_closed()
            return

        # Generar respuesta de WebSocket
        hash_obj = hashlib.sha1(websocket_key.encode() + b"258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
        accept_key = ubinascii.b2a_base64(hash_obj.digest()).decode().strip()
        response = (
            "HTTP/1.1 101 Switching Protocols\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Accept: {accept_key}\r\n\r\n"
        )

        # Enviar respuesta
        writer.write(response.encode())
        await writer.drain()

        print("Cliente WebSocket conectado")

        while True:
            header = await reader.read(2)
            if not header:
                break
            
            payload_len = header[1] & 127
            if payload_len == 126:
                payload_len = struct.unpack(">H", await reader.read(2))[0]
            elif payload_len == 127:
                payload_len = struct.unpack(">Q", await reader.read(8))[0]

            mask_key = await reader.read(4)
            payload = bytearray(await reader.read(payload_len))

            for i in range(payload_len):
                payload[i] ^= mask_key[i % 4]

            message = payload.decode()
            print(f"Mensaje recibido: {message}")

            # Ejecutar función según el mensaje recibido
            
            w_response = car_functions(message)

            # Responder al cliente
            response_msg = f"Comando recibido: {w_response}"
            writer.write(b"\x81" + bytes([len(response_msg)]) + response_msg.encode())
            await writer.drain()

    except Exception as e:
        print(f"Error: {e}")

    finally:
        writer.close()
        await writer.wait_closed()

# Servidor WebSocket sin serve_forever()
async def start_server():
    server = await asyncio.start_server(handle_client, ip, 80)
    print("Servidor WebSocket corriendo en:", ip)
    
    # Mantener el servidor corriendo manualmente
    while True:
        await asyncio.sleep(1)

asyncio.run(start_server())

    
"""
    if 'GET /move_servo' in request:
        if '?' in request:
            params = request.split(' ')[1].split('?')[1].split('&')
            for param in params:
                name, value = param.split('=')
                if name == 'servo1':
                    if value == 'left':
                        servo1_angle = max(0, servo1_angle - 20)  # Mover a la izquierda más rápido
                    elif value == 'right':
                        servo1_angle = min(180, servo1_angle + 20)  # Mover a la derecha más rápido
                    move_servo(servo1, servo1_angle)
                elif name == 'servo2':
                    if value == 'up':
                        servo2_angle = max(0, servo2_angle - 20)  # Mover hacia arriba más rápido
                    elif value == 'down':
                        servo2_angle = min(180, servo2_angle + 20)  # Mover hacia abajo más rápido
                    move_servo(servo2, servo2_angle)
                elif name == 'servo3':
                    if value == 'close':
                        servo3_angle = 0  # Cerrar mano
                    elif value == 'open':
                        servo3_angle = 90  # Abrir mano
                    move_servo(servo3, servo3_angle)

    response = web_page()
    cl.send('HTTP/1.1 200 OK\n')
    cl.send('Content-Type: text/html\n')
    cl.send('Connection: close\n\n')
    cl.sendall(response)
    cl.close()
"""
