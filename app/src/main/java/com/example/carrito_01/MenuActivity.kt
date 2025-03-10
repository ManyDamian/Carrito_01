package com.example.carrito_01

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

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
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.getItemId() == R.id.navicon){
            Toast.makeText(this, "👻👻👻", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }
}
