package com.aaronbrecher.neverlate.ui.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.Menu
import android.view.MenuInflater
import com.aaronbrecher.neverlate.R

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)

        for (index in 0 until preferenceScreen.preferenceCount) {
            val preference = preferenceScreen.getPreference(index)
            if (preference !is CheckBoxPreference) {
                preference.createDefaultValueIfNull()
                setPreferenceSummary(preference,
                        preferenceScreen.sharedPreferences.getString(preference.key, "")!!)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val preference = findPreference(key)
        if (null != preference && preference !is CheckBoxPreference) {
            setPreferenceSummary(preference, sharedPreferences.getString(key, "")!!)
        }
    }

    override fun onStop() {
        super.onStop()
        /* Unregister the preference change listener */
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
        /* Register the preference change listener */
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    private fun setPreferenceSummary(preference: Preference, value: Any) {
        val stringValue = value.toString()
        if (preference is ListPreference) {
            /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                preference.summary = preference.entries[prefIndex]
            }
        } else {
            preference.summary = stringValue
        }
    }

    private fun Preference.createDefaultValueIfNull() {
        if (preferenceScreen.sharedPreferences.getString(this.key, "") == "") {
            if (this.key == getString(R.string.pref_units_key)) {
                preferenceManager.sharedPreferences.edit().putString(
                        getString(R.string.pref_units_key), getDefaultUnitsByLocale()
                ).apply()
            }
        }
    }

    private fun getDefaultUnitsByLocale(): String {
        var country: String;
        // Android N and higher supports multiple locales, we base default units on the main locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            country = resources.configuration.locales[0].country
        } else {
            country = resources.configuration.locale.country
        }

        return when (country.toUpperCase()) {
            "US", "GB", "MM", "LR" -> getString(R.string.pref_units_imperial)
            else -> getString(R.string.pref_units_metric)
        }
    }
}
