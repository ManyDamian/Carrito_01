package com.example.carrito_01

import android.Manifest

import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

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

    Box(modifier = Modifier.fillMaxSize()) {
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

        ButtonRow()
    }
}

@Composable
fun ButtonRow() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = minOf(maxHeight * 0.55f, maxWidth / 4)
        //Los botones se mantienen con un tamaño que ronda desde el 55% del alto de la pantalla
        // hasta el máximo de ancho dividido entre 4 (3 botones y el espacio entre ellos),
        // de forma que mantengan su relación cuadrada
        // y que los botones sigan cabiendo los 3 en pantallas menos anchas.

        Row(
            modifier = Modifier
                .fillMaxWidth().align(Alignment.Center)
                .padding(30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(){
                CustomButton("FW", { acelerar() }, { frenar() }, buttonSize,"Ver")
                CustomButton("BW", { reversa() }, { frenar() }, buttonSize,"Ver")
            }

            Row (horizontalArrangement = Arrangement.SpaceBetween){
                CustomButton("<=", { giroIzq() }, { soltarVolante() }, buttonSize,"Hor")
                CustomButton("=>", { giroDer() }, { soltarVolante() }, buttonSize,"Hor")

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
        modifier = modifier.size(width = size*h_ratio, height = size*v_ratio).pointerInput(Unit) {
            detectTapGestures(
                onPress= {
                    isPressed = true
                    onPress()
                    tryAwaitRelease()
                    isPressed = false
                    onRelease()
                }
            )
        }, contentAlignment =  Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = if (isPressed) Color.Gray else Color.Blue,
                cornerRadius = CornerRadius(150f,150f)

            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 40.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// Funciones incompletas, serán enlazadas a las funciones del ESP32
fun acelerar() { Log.d("ACTION","Acelerando...") }
fun frenar() { Log.d("ACTION","Frenando...") }
fun reversa() { Log.d("ACTION","De reversa...") }
fun giroIzq() { Log.d("ACTION","Girando a la izquierda ...") }
fun giroDer() { Log.d("ACTION","Girando a la derecha ...") }
fun soltarVolante() { Log.d("ACTION","Yendo derecho ...") }