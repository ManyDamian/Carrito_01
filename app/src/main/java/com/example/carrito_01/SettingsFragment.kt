package com.example.carrito_01

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    private var profilePicPref: Preference? = null //Implementamos la preferencia  para guardar la imagen en sharedpreferences

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

        //imprimir estos valores para verificar
        Log.d("SettingsFragment", "Custom Colors: $enableCustomColors, H: $hue, S: $saturation, V: $value")

        profilePicPref = findPreference("profile_picture")
        profilePicPref?.setOnPreferenceClickListener {
            openGallery()
            true
        }

        // Cargar la imagen guardada
        loadProfileImage()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfileImage(imageUri: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit().putString("profile_picture_uri", imageUri).apply()
        Log.d("SettingsFragment", "Imagen guardada: $imageUri")
    }

    private fun loadProfileImage() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedUri = sharedPreferences.getString("profile_picture_uri", null)
        if (savedUri != null) {
            Log.d("SettingsFragment", "Imagen cargada: $savedUri")
        }
    }
}
