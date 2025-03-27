package com.example.carrito_01

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.preference.PreferenceManager
import okio.FileNotFoundException

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)

        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        toolbar?.title=""
        setSupportActionBar(toolbar)

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // Cargar la animación desde el archivo XML
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_button)

        // Aplicar la animación cuando se presionan los botones
        btnPlay.setOnClickListener {
            it.startAnimation(scaleAnimation)  // Iniciar animación
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnSettings.setOnClickListener {
            it.startAnimation(scaleAnimation)  // Iniciar animación
            startActivity(Intent(this, SettingsActivity::class.java))
        }


    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu() // Esto forzará la recreación del menú
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.custom_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val imageUriString = sharedPreferences.getString("profile_picture_uri", null)

        if (imageUriString != null) {
            try {
                val imageUri = Uri.parse(imageUriString)
                val inputStream = contentResolver.openInputStream(imageUri)
                var bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    bitmap = cropToSquare(bitmap)
                    bitmap = scaleBitmap(bitmap, 150) // Ajusta el tamaño del icono
                    val roundedDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                    roundedDrawable.isCircular = true
                    menu?.findItem(R.id.navicon)?.icon = roundedDrawable
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.navicon) {
            // Obtener el nombre de usuario guardado en SharedPreferences
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val username = sharedPreferences.getString("username", "Usuario")  // "Usuario" es el valor por defecto

            Toast.makeText(this, "Bienvenido $username !!!", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scaleBitmap(bitmap: Bitmap, newSize: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newSize, newSize, true)
    }


    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        val xOffset = (width - size) / 2
        val yOffset = (height - size) / 2

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
    }
}
