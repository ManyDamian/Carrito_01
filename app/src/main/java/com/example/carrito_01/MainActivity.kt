package com.example.carrito_01

import android.Manifest
import android.annotation.SuppressLint

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import android.webkit.WebViewClient
import okhttp3.*


//========== CLASE WEBSOCKET =========== CONEXION AL ESP32 POR UN WEBSOCKET
class WebSocketClient(url: String) {
    private val client = OkHttpClient()
    private val request = Request.Builder().url(url).build()
    private var webSocket: WebSocket? = null  // Guardar la conexión

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            println("✅ Conectado al WebSocket")
            webSocket = ws // Guardar la conexión activa
        }

        override fun onMessage(ws: WebSocket, text: String) {
            println("📩 Mensaje recibido: $text")
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            println("❌ Error en WebSocket: ${t.message}")
        }
    }

    fun connect() {
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendMessage(message: String) {
        webSocket?.send(message) ?: println("⚠ No hay conexión WebSocket")
    }

    fun close() {
        webSocket?.close(1000, "Cierre normal")
    }
}


// Asignamos el wsClient como objeto público
val conIP = "192.168.100.71" //IP DEL SOCKET
val wsClient = WebSocketClient("ws://$conIP:80")

//========== CLASE PRINCIPAL =============
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Solicitar permiso de cámara en tiempo de ejecución
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    // Manejo si el permiso no es otorgado
                    finish()
                }
            }

        // Verificar permiso
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        // CONEXIÓN CON EL WEBSOCKET
        wsClient.connect()

        setContent {

            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            // Escondemos la barra de navegacion.
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())


            //CameraScreen()
            ESP32CamScreen(conIP)
        }
    }
}

// === PANTALLA DE LA TRANSMISION CON LA INTERFAZ ENCIMA====
@Composable
fun ESP32CamScreen(ip: String) {
    val streamUrl = "http://$ip:8080/stream"

    Box(modifier = Modifier.fillMaxSize()) { //Fondo con la vista de la cámara

        MJPEGStream(streamUrl)

        ButtonRow() //Interfaz tactil de los botones
    }


}
// ====== CONEXIÓN A LA TRANSMISION ======
@Composable
fun MJPEGStream(url: String) {

    AndroidView(
        factory = { context ->
            WebView(context).apply {

                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                webViewClient = WebViewClient()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ======= RESTO DE CÓDIGO ====
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) { //Fondo con la vista de la cámara

        AndroidView(
            factory = { AndroidViewContext ->
                val previewView = PreviewView(AndroidViewContext)
                val cameraProvider = cameraProviderFuture.get()

                val preview = CameraPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        ButtonRow() //Interfaz tactil de los botones
    }
}



@Composable
fun ButtonRow() {
    // Estado inicial de los botones
    var boton_1 by remember { mutableStateOf(R.drawable.ud_0) }
    var boton_2 by remember { mutableStateOf(R.drawable.lr_0) }

    // Estados para la animación del sprite
    var spriteActivo by remember { mutableStateOf(0) }
    var animando by remember { mutableStateOf("no") }
    var frameIndex by remember { mutableStateOf(0) }
    //Ajustes del sprite animado
    val spriteFront = listOf(R.drawable.f_04, R.drawable.f_03, R.drawable.f_02, R.drawable.f_01)
    val spriteTurn = listOf(R.drawable.t_04, R.drawable.t_03, R.drawable.t_02, R.drawable.t_01)
    val frames = if (spriteActivo == 0) spriteFront else spriteTurn
    var turning by remember { mutableStateOf(false) }

    turning = (spriteActivo == 2) //turning se vuelve true sólo si el sprite activo es el 2 (derecha)

    // Lógica para la animación en bucle
    LaunchedEffect(animando) {
        while (animando != "no") {

            frameIndex = if (animando == "si") {
                (frameIndex + 1) % frames.size // Avanza
            } else if (animando == "rev") {
                (frameIndex - 1 + frames.size) % frames.size // Retrocede
            } else {
                frameIndex
            }

            delay(83) // delay de la animación (1 fotograma cada 83ms = 12fps +/-)
        }
    }


    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = minOf(maxHeight * 0.55f, maxWidth / 4) //Tamaño de los botones "tocables"
        val uiSize = minOf(maxHeight * 0.60f, maxWidth / 4) //Tamaño de los elementos gráficos
        val charSize = minOf(maxHeight * 0.45f, maxWidth / 3) //Tamaño del personaje

        //Los botones se mantienen con un tamaño que ronda desde el 55% del alto de la pantalla
        // hasta el máximo de ancho dividido entre 4 (3 botones y el espacio entre ellos),
        // de forma que mantengan su relación cuadrada
        // y que los botones sigan cabiendo los 3 en pantallas menos anchas.

        // === PERSONAJE ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) //Centrado al fondo
                .padding(15.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            //placeholder
            Image(
                painter = painterResource(id = frames[frameIndex]),
                contentDescription = null,
                modifier = Modifier.size(charSize)
                    .graphicsLayer { scaleX = if (turning) -1f else 1f } // Voltea la imagen si 'volteado' es true
            )

        }

        //=== Interfaz de elementos gráficos (UI) ===

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(45.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // BOTON 1
            Image(
                painter = painterResource(id = boton_1),
                contentDescription = null,
                modifier = Modifier.size(uiSize)
            )
            // SPRITE
            // BOTON 2
            Image(
                painter = painterResource(id = boton_2),
                contentDescription = null,
                modifier = Modifier.size(uiSize)
            )
        }

        //=== Interfaz de botones de interacción ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(45.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(){       //Los custombuttoms se complicaron un poco, pero en resumen: texto, { funcion onpress( {logica boton},{logica animacion} }, { funcion onrelease (lomismo)} }, tamaño de boton, modo de boton
                CustomButton("FW", { acelerar(imgSetter = { xclr -> boton_1 = xclr }, animSetter = {anim -> animando = anim}) },
                    { frenar(imgSetter= { stp -> boton_1 = stp }, animSetter = {anim -> animando = anim} )},
                    buttonSize,"Ver")

                CustomButton("BW", { reversa(imgSetter =  { rev -> boton_1 = rev } , animSetter = {anim -> animando = anim} )},
                    { frenar(imgSetter= { stp -> boton_1 = stp }, animSetter = {anim -> animando = anim} ) },
                    buttonSize,"Ver")
            }

            Row (horizontalArrangement = Arrangement.SpaceBetween){
                CustomButton("<=", { giroIzq( imgSetter = {lft -> boton_2 = lft} , sprSetter = {lft -> spriteActivo = lft} )},
                    { soltarVolante(imgSetter =  {svt -> boton_2 = svt} , sprSetter = {frn -> spriteActivo = frn} )},
                    buttonSize,"Hor")
                CustomButton("=>", { giroDer ( imgSetter = {rgt -> boton_2 = rgt} , sprSetter = {rgt -> spriteActivo = rgt} ) },
                    { soltarVolante(imgSetter =  {svt -> boton_2 = svt} , sprSetter = {frn -> spriteActivo = frn} ) },
                    buttonSize,"Hor")

            }

        }

        //=== BARRA DE ESTADO ===
        Box(
            modifier = Modifier
                .size(width = maxWidth*.7f, height = 140.dp)
                .align(Alignment.TopCenter)
                .padding(25.dp),
            contentAlignment =  Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                drawRoundRect(
                    color = Color.hsl(0f,0f,1.0f,0.5f),
                    cornerRadius = CornerRadius(100f,100f)

                )
            }

            Row (horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 25.dp)
                        .fillMaxWidth()
                        .align(Alignment.Center) ){
                Text(
                    //Texto para la información que vamos a tener
                    text = "0.5 m/s",
                    color = Color.Black,
                    fontSize = 40.sp
                )

                Image(
                    painter = painterResource(R.drawable.baseline_camera_alt_24),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )

                Text(
                    //Texto para la información que vamos a tener
                    text = "ping: 02ms",
                    color = Color.Black,
                    fontSize = 25.sp,
                    modifier= Modifier.padding(vertical = 10.dp)
                )

            }

        }


    }
}


// ============= PREVIEW DE LAYOUT ==============

@Preview(showBackground = true, widthDp = 960, heightDp = 432) //para una resolución 2400 x 1080, los dp son 960 x 432
@Composable
fun PreviewButtonRow() {

    ButtonRow()
}

// ========= BOTON PERSONALIZADO Y FUNCIONES DE BOTON ==============

@SuppressLint("ResourceAsColor")
@Composable
fun CustomButton( //Estos botones personalizados reciben los siguientes parámetros:
    text: String, // Texto, función "al presionar", función "al soltar" y tamaño
    onPress: () -> Unit,
    onRelease: () -> Unit,
    size: Dp,
    button_mode: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false)} //Variable para saber si se está presioando el botón

    val h_ratio = if (button_mode == "Hor") 0.5f else 1.0f //Esto ayuda a comodar los botones en filas y columnas manteniendo un tamaño equitativo en un cuadrado.
    val v_ratio = if (button_mode == "Ver") 0.5f else 1.0f //Es decir, podremos tener 2 botones horizontales en una fila, o 2 botones verticales en una columna, haciendo que ambos juntos formen un cuadrado perfecto


    Box (
        modifier = modifier
            .size(width = size * h_ratio, height = size * v_ratio)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }

                )
            }

        , contentAlignment =  Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            drawRoundRect(
                color = Color.Transparent //Los botones son invisibles, porque la UI es la que se verá
                //color = if (isPressed) Color.Gray else Color.Blue, //Colores de Testing
                //cornerRadius = CornerRadius(150f,150f)

            )
        }
        Text(
            //El texto sólo era visible para motivos de testing, con la UI ya no es necesario
            /*
            text = text,
            color = Color.White,
            fontSize = 40.sp,
            modifier = Modifier.align(Alignment.Center) */
            ""
        )
    }
}

// Funciones incompletas, serán enlazadas a las funciones del ESP32
// BOTON 1
fun acelerar(imgSetter: (Int)->Unit, animSetter: (String) -> Unit)  {
    imgSetter(R.drawable.ud_1) //Cambiamos el estado del boton
    animSetter("si") //Activamos la animación del personaje

    wsClient.sendMessage("ACELERA")
    Log.d("ACTION","Acelerando...")
}

fun frenar(imgSetter: (Int)->Unit, animSetter: (String) -> Unit) {
    imgSetter(R.drawable.ud_0)
    animSetter("no") //Desactivamos la animación del personaje

    wsClient.sendMessage("FRENA")
    Log.d("ACTION","Frenando...")
}
fun reversa(imgSetter: (Int)->Unit, animSetter: (String) -> Unit) {
    imgSetter(R.drawable.ud_2)
    animSetter("rev") //Activamos la animación del personaje, pero en reversa

    wsClient.sendMessage("REVERSA")
    Log.d("ACTION","De reversa...")
}

// BOTON 2
fun giroIzq(imgSetter: (Int)->Unit, sprSetter: (Int) -> Unit) {
    imgSetter(R.drawable.lr_2)
    sprSetter(1) //Valores de sprite activo: 0 FRENTE, 1 IZQ, 2 DER

    wsClient.sendMessage("IZQUIERDA")
    Log.d("ACTION","Girando a la izquierda ...")
}

fun giroDer(imgSetter: (Int)->Unit, sprSetter: (Int) -> Unit) {
    imgSetter(R.drawable.lr_1)
    sprSetter(2) //Valores de sprite activo: 0 FRENTE, 1 IZQ, 2 DER

    wsClient.sendMessage("DERECHA")
    Log.d("ACTION","Girando a la derecha ...")
}

fun soltarVolante(imgSetter: (Int)->Unit, sprSetter: (Int) -> Unit) {
    imgSetter(R.drawable.lr_0)
    sprSetter(0) //Valores de sprite activo: 0 FRENTE, 1 IZQ, 2 DER

    wsClient.sendMessage("SOLTAR")
    Log.d("ACTION","Yendo derecho ...")
}

