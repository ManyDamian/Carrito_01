package com.example.carrito_01

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<Pair<Int, File>?>(null) }

    // Cargar imágenes
    LaunchedEffect(imagesDir) {
        if (imagesDir.exists()) {
            imageList.clear()
            imageList.addAll(imagesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList())
        }
    }

    var expandedImageIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "GALERÍA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            if (imageList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No hay elementos en la galería.",
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
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
            }
        }

        // Vista expandida (sin cambios)
        expandedImageIndex?.let { index ->
            val image = imageList[index]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            expandedImageIndex = null
                        })
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
                                detectHorizontalDragGestures { _, dragAmount ->
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
                            imageToDelete = index to image
                            showDeleteDialog = true
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

                    if (showDeleteDialog && imageToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("¿Eliminar imagen?") },
                            text = { Text("¿Estás seguro de que deseas eliminar esta imagen?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val (i, img) = imageToDelete!!
                                    if (img.delete()) {
                                        imageList.removeAt(i)
                                        expandedImageIndex = null
                                        Toast.makeText(context, "Imagen eliminada", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al eliminar imagen", Toast.LENGTH_SHORT).show()
                                    }
                                    showDeleteDialog = false
                                    imageToDelete = null
                                }) {
                                    Text("Sí")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    imageToDelete = null
                                }) {
                                    Text("No")
                                }
                            }
                        )
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
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CarritoMacuin")
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out ->
            file.inputStream().copyTo(out)
            Toast.makeText(context, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
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

@Preview(showBackground = true, widthDp = 432, heightDp = 960) //para una resolución 2400 x 1080, los dp son 960 x 432
@Composable
fun PreviewG() {

    GalleryScreen()
}

