package com.example.carrito_01

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)

        val profilePicPref = findPreference<Preference>("profile_picture")
        profilePicPref?.setOnPreferenceClickListener {
            // Aquí puedes abrir la galería o mostrar opciones
            true
        }
    }
}
