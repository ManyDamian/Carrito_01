package com.example.carrito_01

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import okhttp3.*
import android.content.Context as Context1
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}


//========== CLASE WEBSOCKET =========== CONEXION AL ESP32 POR UN WEBSOCKET
class WebSocketClient(url: String) {
    private val client = OkHttpClient()
    private val request = Request.Builder().url(url).build()
    private var webSocket: WebSocket? = null  // Guardar la conexión

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            println("✅ Conectado al WebSocket")
            webSocket = ws // Guardar la conexión activa
            showNotification(MyApplication.instance.applicationContext, "Conexión esablecida", "Web Socket: $ws")
        }

        override fun onMessage(ws: WebSocket, text: String) {
            println("📩 Mensaje recibido: $text")
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            println("❌ Error en WebSocket: ${t.message}")
            showNotification(MyApplication.instance.applicationContext, "Error en la conexión", "Asegurese que el carrito esté funcionando y conectado a internet. Intente de nuevo.")
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
val conIP = "192.168.1.127" //IP DEL SOCKET
val wsClient = WebSocketClient("ws://$conIP:80")



//========== CLASE PRINCIPAL =============
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showNotification(this, "Permiso concedido", "Ahora puedes recibir notificaciones.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
fun ButtonRow() {
    // === RECURSOS DE LA INTERFAZ ===
    // == SONIDOS ==
        // Contextos para evitar problemas con la preview
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    // Lista de archivos de audio en raw
    val bocinaList = listOf(R.raw.horn_1, R.raw.horn_2, R.raw.horn_3, R.raw.horn_4, R.raw.horn_5)
    var bocina by remember { mutableStateOf<MediaPlayer?>(null) }
    // La bocina tomará un sonido aleatorio de la lista al iniciar el player, mientras tanto, queda null
    var isPlaying by remember { mutableStateOf(false) } // Estado para deshabilitar el botón mientras suena

    // == VISUALES ==
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
    var char_rumble by remember { mutableStateOf(((-6..0).random())) } //Vibración del personaje sobre el piso

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
            char_rumble = (-6..0).random() //Cambiar valor del agitado del personaje
            delay(83) // delay de la animación (1 fotograma cada 83ms = 12fps +/-)
        }
        char_rumble = 0
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
                .padding((15+char_rumble).dp),
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
        // Obtener SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
        val imageUriString = sharedPreferences.getString("profile_picture_uri", null)
        val enablePP = sharedPreferences.getBoolean("enable_profilepic", false)

        // Convertir a URI si no es null
        val imageUri = imageUriString?.let { Uri.parse(it) }

        // Imagen predeterminada en caso de que no haya una seleccionada
        val defaultImage = R.drawable.icon_95 // Reemplaza con tu imagen predeterminada
        val finalImage = if (enablePP && imageUri != null) imageUri else defaultImage

        val imageModifier = Modifier
            .size(100.dp)
            .then(if (enablePP) Modifier.clip(RoundedCornerShape(16.dp)) else Modifier)

        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(15.dp)){

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(finalImage) // Si `imageUri` es null, usa la predeterminada
                        .crossfade(true) // Efecto de transición suave
                        .build()
                ),
                contentDescription = null,
                modifier = imageModifier,
                    contentScale = if (enablePP) ContentScale.Crop else ContentScale.Fit // Recorte la imagen al centro (1:1)
            )
        }


        Box(
            modifier = Modifier
                .size(width = maxWidth*.7f, height = 120.dp)
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


                Image(
                    painter = painterResource(R.drawable.baseline_camera_alt_24),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )

                Text(
                    //Texto para la información que vamos a tener
                    text = "0.5 m/s",
                    color = Color.Black,
                    fontSize = 40.sp
                )

                Text(
                    //Texto para la información que vamos a tener
                    text = "ping: 02ms",
                    color = Color.Black,
                    fontSize = 25.sp,
                    modifier= Modifier.padding(vertical = 10.dp)
                )

                // IconButton for Start Action
                IconButton(
                    onClick = {
                        if (!isPreview && !isPlaying) {
                            bocina?.release() // Liberar el reproductor anterior
                            val selectedSound = bocinaList.random()

                            bocina = MediaPlayer.create(context, selectedSound).apply {
                                setOnCompletionListener {
                                    isPlaying = false // Habilitar el botón cuando termine el audio
                                }
                                start()
                                isPlaying = true // Deshabilitar el botón mientras suena el audio
                            }
                        }
                    },
                    enabled = !isPlaying // Deshabilita el botón mientras el audio suena
                ) {
                    Icon(
                        painter = painterResource(R.drawable.speaker_icon),
                        contentDescription = "",
                        Modifier.size(50.dp)
                    )
                }

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
    // === SONIDOS PARA LOS BOTONES, Y CONTEXTO PARA VIBRACIÓN Y MEDIA PLAYER
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val sound_press = R.raw.b_press         //Sonido de presionar botón
    val sound_release = R.raw.b_release     //Sonido de soltar botón

    val vibrator = context.getSystemService(Context1.VIBRATOR_SERVICE) as Vibrator
    var isPressed by remember { mutableStateOf(false)} //Variable para saber si se está presioando el botón

    val h_ratio = if (button_mode == "Hor") 0.5f else 1.0f //Esto ayuda a comodar los botones en filas y columnas manteniendo un tamaño equitativo en un cuadrado.
    val v_ratio = if (button_mode == "Ver") 0.5f else 1.0f //Es decir, podremos tener 2 botones horizontales en una fila, o 2 botones verticales en una columna, haciendo que ambos juntos formen un cuadrado perfecto
    var isEnabled by remember { mutableStateOf(true) }

    fun soundplay(sres: Int) {
        if (!isPreview) {
            val mediaPlayer = MediaPlayer.create(context, sres)
            mediaPlayer.setOnCompletionListener { it.release() } // Libera recursos cuando termina
            mediaPlayer.start()
        }
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    Box (
        modifier = modifier
            .size(width = size * h_ratio, height = size * v_ratio)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!isEnabled) return@detectTapGestures  // Bloquea el toque si está deshabilitado

                        isEnabled = false  // Deshabilita el botón temporalmente
                        isPressed = true
                        onPress()
                        soundplay(sound_press)

                        tryAwaitRelease()

                        isPressed = false
                        onRelease()
                        soundplay(sound_release)
                        // Esperar unos ms antes de volver a habilitar el botón
                            delay(100)
                            isEnabled = true

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

// NOTIFICACIONES

fun showNotification(context: Context1, title: String, message: String) {
    val channelId = "my_channel_id"
    val notificationId = 1

    // Crear el canal de notificación en Android 8+ (Oreo, API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "My Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel Description"
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context1.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Crear la notificación
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true) // Se cierra al tocarla
        .build()

    // Mostrar la notificación
    with(NotificationManagerCompat.from(context)) {
        // Verificar permisos en Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notify(notificationId, notification)
        }
    }
}