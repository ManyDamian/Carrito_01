import network
import usocket as socket
import uasyncio as asyncio
import ubinascii
import struct
import hashlib  # Importar hashlib
import machine
import camera
import time

# Esperar un poco antes de conectar
time.sleep(2)

def init_camera(max_retries=5, retry_delay=1):
    for i in range(max_retries):
        try:
            print(f"📷 Intento {i+1}/{max_retries} de inicializar cámara...")
            
            camera.init(0, format=camera.JPEG)
            camera.framesize(camera.FRAME_QVGA)  # Baja calidad para ahorrar RAM
            camera.quality(20)
            camera.speffect(camera.EFFECT_NONE)  # Sin efectos
            camera.whitebalance(camera.WB_NONE)  # Sin balance de blancos automático
            camera.contrast(0)  # Contraste normal
            camera.brightness(0)  # Brillo normal
            camera.saturation(0)  # Saturación normal
            print("✅ Cámara inicializada correctamente.")
            return True
        except Exception as e:
            print(f"⚠️ Error en la cámara: {e}")
            camera.deinit()  # Apagar y volver a intentar
            time.sleep(retry_delay)

    print("❌ No se pudo inicializar la cámara.")
    return False


# 🚗 Pines del TB6612FNG
MOTOR_A1 = machine.Pin(13, machine.Pin.OUT)
MOTOR_A2 = machine.Pin(2, machine.Pin.OUT)
PWM_A = machine.PWM(machine.Pin(12))
MOTOR_B1 = machine.Pin(15, machine.Pin.OUT)
MOTOR_B2 = machine.Pin(14, machine.Pin.OUT)
PWM_B = machine.PWM(machine.Pin(12))

PWM_A.freq(5000)
PWM_B.freq(5000)

# Conexión Wi-Fi
#ssid = "TP-Link_A1C7"
#password = "61307315"

ssid = 'Totalplay-D1AA'  # Cambia por el nombre de tu red Wi-Fi
password = 'TOTAL_PLAYc99'  # Cambia por la contraseña de tu red

station = network.WLAN(network.STA_IF)
station.active(True)
time.sleep(1)

# Intentar conectarse con reintentos
max_retries = 10
for i in range(max_retries):
    print(f"🔄 Intento {i+1}/{max_retries} de conexión a WiFi...")
    station.connect(ssid, password)
    
    for _ in range(5):  # Esperar hasta 5 segundos en cada intento
        if station.isconnected():
            print("✅ Conectado a WiFi")
            break
        time.sleep(1)
    
    if station.isconnected():
        break
else:
    print("❌ No se pudo conectar a WiFi.")
    
ip = station.ifconfig()[0]

print(f"Conectado, IP: {ip}")



# 🚗 Funciones para mover el carrito
def adelante():
    MOTOR_A1.on()
    MOTOR_A2.off()
    PWM_A.duty(800)

def atras():
    MOTOR_A1.off()
    MOTOR_A2.on()
    PWM_A.duty(800)
    
def frenar():
    MOTOR_A1.off()
    MOTOR_A2.off()
    PWM_A.duty(0)

def izquierda():
    MOTOR_B1.on()
    MOTOR_B2.off()
    PWM_B.duty(512)

def derecha():
    MOTOR_B1.off()
    MOTOR_B2.on()
    PWM_B.duty(512)

def soltar():    
    MOTOR_B1.off()
    MOTOR_B2.off()    
    PWM_B.duty(0)

# === SETTING INICIAL ===
soltar()
frenar()

# ==== FUNCIONES WEBSOCKET =======
def car_functions(msg):
    if msg == "LED_ON":
        return "Encendiendo LED"
    elif msg == "LED_OFF":
        return "Apagando LED"
    
    elif msg == "ACELERA":
        adelante()
        return "Acelerando ..."
    elif msg == "FRENA":
        frenar()
        return "Frenando ..."
    elif msg == "REVERSA":
        atras()
        return "De reversa ..."
    elif msg == "IZQUIERDA":
        izquierda()
        return "Giro a la izquierda..."
    elif msg == "DERECHA":
        derecha()
        return "Giro a la derecha ..."
    elif msg == "SOLTAR":
        soltar()
        return "Soltando volante ..."


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

# Servidor de Streaming de Video
async def stream_handler(reader, writer):
    try:
        request = await reader.read(1024)
        headers = request.decode().split("\r\n")
        
        if "GET /stream" not in headers[0]:
            writer.close()
            await writer.wait_closed()
            return
        
        response = (
            "HTTP/1.1 200 OK\r\n"
            "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n"
        )
        writer.write(response.encode())
        await writer.drain()

        print("Cliente conectado al streaming")

        while True:
            frame = camera.capture()
            if frame:
                writer.write(
                    b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame + b"\r\n"
                )
                await writer.drain()

    except Exception as e:
        print(f"Error en streaming: {e}")

    finally:
        writer.close()
        await writer.wait_closed()



# Servidor WebSocket sin serve_forever()
async def start_server():
    try:
        asyncio.create_task(asyncio.start_server(handle_client, ip, 80))
        print("✅ WebSocket escuchando en ws://{}:80".format(ip))
    except Exception as e:
        print("❌ Error iniciando WebSocket:", e)

    try:
        asyncio.create_task(asyncio.start_server(stream_handler, ip, 8080))
        print("✅ Streaming escuchando en http://{}:8080".format(ip))
    except Exception as e:
        print("❌ Error iniciando Streaming:", e)

    while True:
        await asyncio.sleep(1)

asyncio.run(start_server())

