package com.example.carrito_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.custom_menu, menu)
        /*
        // Obtener la imagen guardada en SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val imageUriString = sharedPreferences.getString("profile_picture_uri", null)

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    val roundedDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                    roundedDrawable.isCircular = true

                    // Cambiar el ícono del menú
                    menu?.findItem(R.id.navicon)?.icon = roundedDrawable
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
        */
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.getItemId() == R.id.navicon){

            Toast.makeText(this, "Hola 👻👻👻", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }
}
