import network
import socket
import machine
import time
import usocket
import camera

# 🚗 Pines del TB6612FNG
MOTOR_A1 = machine.Pin(13, machine.Pin.OUT)
MOTOR_A2 = machine.Pin(2, machine.Pin.OUT)
PWM_A = machine.PWM(machine.Pin(12))
MOTOR_B1 = machine.Pin(15, machine.Pin.OUT)
MOTOR_B2 = machine.Pin(14, machine.Pin.OUT)
PWM_B = machine.PWM(machine.Pin(12))

PWM_A.freq(5000)
PWM_B.freq(5000)

# 📡 Conectar WiFi
SSID = "TP-Link_A1C7"
PASSWORD = "61307315"

wifi = network.WLAN(network.STA_IF)
wifi.active(True)
wifi.connect(SSID, PASSWORD)

while not wifi.isconnected():
    time.sleep(0.5)
    
print("Conectado! IP:", wifi.ifconfig()[0])

# 🖥️ Página HTML
html = """<!DOCTYPE html>
<html>
<head>
<title>Carro ESP32-CAM</title>
<style>
  body { text-align: center; font-family: Arial; }
  .btn { font-size: 20px; padding: 15px; margin: 5px; }
</style>
</head>
<body>
<h1>Carro ESP32-CAM</h1>
<img src="/video" width="320" height="240">
<br>
<button class="btn" onclick="fetch('/move?dir=F')">arriba</button><br>
<button class="btn" onclick="fetch('/move?dir=L')">izquierda</button>
<button class="btn" onclick="fetch('/move?dir=R')">derecha</button><br>
<button class="btn" onclick="fetch('/move?dir=B')">abajo</button>
<button class="btn" onclick="fetch('/move?dir=S')">alto️</button>
</body>
</html>
"""

# 🚗 Funciones para mover el carrito
def adelante():
    MOTOR_A1.on()
    MOTOR_A2.off()
    PWM_A.duty(800)

def atras():
    MOTOR_A1.off()
    MOTOR_A2.on()
    PWM_A.duty(800)

def izquierda():
    MOTOR_B1.on()
    MOTOR_B2.off()
    PWM_B.duty(512)

def derecha():
    MOTOR_B1.off()
    MOTOR_B2.on()
    PWM_B.duty(512)

def detener():
    MOTOR_A1.off()
    MOTOR_A2.off()
    MOTOR_B1.off()
    MOTOR_B2.off()
    PWM_A.duty(0)
    PWM_B.duty(0)

# 📷 Configuración de la cámara
def init_camera():
    try:
        camera.deinit()  # Intentar desactivar la cámara primero
    except:
        pass  # Si no estaba activada, ignorar el error

    try:
        camera.init(0, format=camera.JPEG)
        camera.framesize(camera.FRAME_QVGA)  # Resolución 320x240
        camera.quality(35)  # Ajustar calidad
        return True
    except Exception as e:
        print("Error al inicializar la cámara:", e)
        return False


# 🌐 Servidor web
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(('', 80))
server.listen(5)

if init_camera():
    while True:
        conn, addr = server.accept()
        request = conn.recv(1024).decode()
        
        if "GET / " in request:
            response = html
        elif "GET /move?dir=" in request:
            if "dir=F" in request:
                adelante()
            elif "dir=B" in request:
                atras()
            elif "dir=L" in request:
                izquierda()
            elif "dir=R" in request:
                derecha()
            else:
                detener()
            response = "OK"
        elif "GET /video" in request:
            conn.send('HTTP/1.1 200 OK\r\n')
            conn.send('Content-Type: multipart/x-mixed-replace; boundary=frame\r\n')
            conn.send('\r\n')
            while True:
                buf = camera.capture()
                conn.send('--frame\r\n')
                conn.send('Content-Type: image/jpeg\r\n')
                conn.send('Content-Length: %d\r\n' % len(buf))
                conn.send('\r\n')
                conn.send(buf)
                conn.send('\r\n')
        
        conn.send("HTTP/1.1 200 OK\n\n" + response)
        conn.close()
else:
    print("Error en la inicialización de la cámara")
