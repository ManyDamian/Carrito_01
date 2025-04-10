package com.example.carrito_01

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File

class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryScreen()
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val imagesDir = File(context.filesDir, "capturas")
    val imageList = remember { mutableStateListOf<File>() }

    // Cargar imágenes
    LaunchedEffect(imagesDir) {
        if (imagesDir.exists()) {
            imageList.clear()
            imageList.addAll(imagesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList())
        }
    }

    var expandedImageIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(imageList, key = { it.name }) { imageFile ->
                Image(
                    painter = rememberAsyncImagePainter(imageFile),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clickable {
                            expandedImageIndex = imageList.indexOf(imageFile)
                        }
                )
            }
        }

        // Vista expandida
        expandedImageIndex?.let { index ->
            val image = imageList[index]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                expandedImageIndex = null
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(image),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .pointerInput(imageList) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    if (dragAmount > 30 && index > 0) {
                                        expandedImageIndex = index - 1
                                    } else if (dragAmount < -30 && index < imageList.size - 1) {
                                        expandedImageIndex = index + 1
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            image.delete()
                            imageList.removeAt(index)
                            expandedImageIndex = null
                        }) {
                            Text("Eliminar")
                        }

                        Button(onClick = {
                            saveToGallery(context, image)
                        }) {
                            Text("Guardar")
                        }

                        Button(onClick = {
                            shareImage(context, image)
                        }) {
                            Text("Compartir")
                        }
                    }
                }
            }
        }
    }
}

fun saveToGallery(context: Context, file: File) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ESP32Capturas")
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out ->
            file.inputStream().copyTo(out)
        }
    }
}

fun shareImage(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Compartir imagen"))
}


// ============= PREVIEW DE LAYOUT ==============

@Preview(showBackground = true, widthDp = 960, heightDp = 432) //para una resolución 2400 x 1080, los dp son 960 x 432
@Composable
fun PreviewG() {

    GalleryScreen()
}

