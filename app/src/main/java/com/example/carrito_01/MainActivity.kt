package com.example.carrito_01

import android.Manifest
import android.annotation.SuppressLint

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

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

        setContent {
            CameraScreen()
        }
    }
}

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
    var spriteActivo by remember { mutableStateOf(1) }
    var animando by remember { mutableStateOf("no") }
    var frameIndex by remember { mutableStateOf(0) }
    //Ajustes del sprite animado
    val spriteFront = listOf(R.drawable.f_04, R.drawable.f_03, R.drawable.f_02, R.drawable.f_01)
    val spriteTurn = listOf(R.drawable.t_04, R.drawable.t_03, R.drawable.t_02, R.drawable.t_01)
    val frames = if (spriteActivo == 1) spriteFront else spriteTurn

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
            )

        }

        //=== Interfaz de elementos gráficos (UI) ===

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(30.dp),
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
                .align(Alignment.Center)
                .padding(30.dp),
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
                CustomButton("<=", { giroIzq {lft -> boton_2 = lft} },
                    { soltarVolante {svt -> boton_2 = svt} },
                    buttonSize,"Hor")
                CustomButton("=>", { giroDer {rgt -> boton_2 = rgt} },
                    { soltarVolante {svt -> boton_2 = svt} },
                    buttonSize,"Hor")

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
    var isPressed by remember { mutableStateOf(false)}
    val h_ratio = if (button_mode == "Hor") 0.5f else 1.0f
    val v_ratio = if (button_mode == "Ver") 0.5f else 1.0f


    Box (
        modifier = modifier
            .size(width = size * h_ratio, height = size * v_ratio)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { //Secuencia de precionado
                        isPressed = true    //Se presiona el boton
                        onPress()           //Se ejecuta función onpress
                        tryAwaitRelease()   //Espera a que el botón se suelte
                        isPressed = false   //El botón deja de presionarse
                        onRelease()         //Ejecuta función onrelease
                    }
                )
            }, contentAlignment =  Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            drawRoundRect(
                color = Color.Transparent //Los botones son invisibles, porque la UI es la que se verá
                //color = if (isPressed) Color.Gray else Color.Blue, //Colores de Testing
                //cornerRadius = CornerRadius(150f,150f)

            )
        }
        Text(
            //El texto sólo es visible para motivos de testing, con la UI ya no es necesario
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
    Log.d("ACTION","Acelerando...")
}

fun frenar(imgSetter: (Int)->Unit, animSetter: (String) -> Unit) {
    imgSetter(R.drawable.ud_0)
    animSetter("no") //Desactivamos la animación del personaje
    Log.d("ACTION","Frenando...")
}
fun reversa(imgSetter: (Int)->Unit, animSetter: (String) -> Unit) {
    imgSetter(R.drawable.ud_2)
    animSetter("rev") //Activamos la animación del personaje, pero en reversa
    Log.d("ACTION","De reversa...")
}

// BOTON 2
fun giroIzq(imgSetter: (Int)->Unit) {
    imgSetter(R.drawable.lr_2)
    Log.d("ACTION","Girando a la izquierda ...")
}

fun giroDer(imgSetter: (Int)->Unit) {
    imgSetter(R.drawable.lr_1)
    Log.d("ACTION","Girando a la derecha ...")
}

fun soltarVolante(imgSetter: (Int)->Unit) {
    imgSetter(R.drawable.lr_0)
    Log.d("ACTION","Yendo derecho ...")
}