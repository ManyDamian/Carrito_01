package com.example.carrito_01

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    private var profilePicPref: Preference? = null //Implementamos la preferencia  para guardar la imagen en sharedpreferences
    private var namepref: Preference? = null

    // Lanzador para abrir la galería
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                saveProfileImage(imageUri.toString())
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext()) //Cargamos el manager de preferencias
        //Valores por defecto
        val enableCustomColors = sharedPreferences.getBoolean("enable_custom_colors", false)
        val hue = sharedPreferences.getInt("color_hue", 100)
        val saturation = sharedPreferences.getInt("color_saturation", 50)
        val value = sharedPreferences.getInt("color_value", 50)

        val username = sharedPreferences.getString("username", "Usuario")  // "Usuario" es el valor por defecto

        //imprimir estos valores para verificar
        Log.d("SettingsFragment", "Custom Colors: $enableCustomColors, H: $hue, S: $saturation, V: $value")

        namepref = findPreference("username")

        profilePicPref = findPreference("profile_picture")
        profilePicPref?.setOnPreferenceClickListener {
            openGallery()
            true
        }

        if (username != "Usuario") {

            namepref?.summary = "$username"
        } else {
            namepref?.summary = "Introduce tu nombre"
        }



        // Cargar la imagen guardada
        loadProfileImage()

        //COLORES
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "enable_custom_colors" || key == "color_hue" || key == "color_saturation" || key == "color_value") {
                updateColors()
            }
        }

        updateColors() // Aplicar colores al inicio
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }


    private fun saveProfileImage(imageUri: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit().putString("profile_picture_uri", imageUri).apply()

        try {
            val uri = Uri.parse(imageUri)
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error al obtener permisos de URI", e)
        }

        Log.d("SettingsFragment", "Imagen guardada: $imageUri")
        profilePicPref?.summary = "Imagen seleccionada"
        Toast.makeText(requireContext(), "Imagen guardada correctamente", Toast.LENGTH_SHORT).show()
    }


    private fun loadProfileImage() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedUri = sharedPreferences.getString("profile_picture_uri", null)
        if (savedUri != null) {
            Log.d("SettingsFragment", "Imagen cargada: $savedUri")

            // Si ya hay una imagen guardada, actualizar el summary
            profilePicPref?.summary = "Imagen seleccionada"
        } else {
            profilePicPref?.summary = "No hay imagen seleccionada"
        }
    }

    //COLORES

    private fun updateColors() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val enableCustomColors = sharedPreferences.getBoolean("enable_custom_colors", false)

        if (enableCustomColors) {
            val hue = sharedPreferences.getInt("color_hue", 100).toFloat()
            val saturation = sharedPreferences.getInt("color_saturation", 50) / 100f
            val value = sharedPreferences.getInt("color_value", 50) / 100f

            val color = Color.HSVToColor(floatArrayOf(hue, saturation, value))

            // Aplicar el color a los elementos de la UI
            activity?.window?.decorView?.setBackgroundColor(color) // Cambia el fondo de la actividad

            // También podrías cambiar dinámicamente los estilos de los elementos
            // Aquí tendrías que acceder a cada Preference y modificarlo manualmente.
        }
    }
}
