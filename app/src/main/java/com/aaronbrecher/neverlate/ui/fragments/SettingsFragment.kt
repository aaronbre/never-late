package com.aaronbrecher.neverlate.ui.fragments

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.*
import androidx.preference.*
import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.utils.BackgroundUtils
import com.aaronbrecher.neverlate.models.Calendar
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appExecutors: AppExecutors

    private val mCalenders: MutableLiveData<List<Calendar>> = MutableLiveData()


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        NeverLateApp.app.appComponent.inject(this)
        addPreferencesFromResource(R.xml.prefs)
        for (index in 0 until preferenceScreen.preferenceCount) {
            val preference = preferenceScreen.getPreference(index)
            if (preference !is CheckBoxPreference && preference !is MultiSelectListPreference) {
                if (preference.key == getString(R.string.prefs_speed_key)) {
                    val unitPref : Preference = preferenceScreen.findPreference(getString(R.string.pref_units_key))
                    changeSpeedPrefsToUnitSystem(preferenceScreen.sharedPreferences.getString(unitPref.key, getString(R.string.pref_units_metric))!!)
                }
                preference.createDefaultValueIfNull()
                setPreferenceSummary(preference,
                        preferenceScreen.sharedPreferences.getString(preference.key, "")!!)
            }
        }

        getCalendars()
        /**
         * Observer for the when the calendar lookup is complete, will set the preferences
         * entries and values to mirror the calendars name and ID
         */
        mCalenders.observe(this, Observer {
            if (it == null) return@Observer
            val calendarPrefs = preferenceScreen.findPreference(getString(R.string.prefs_calendars_key)) as MultiSelectListPreference
            val entries = ArrayList<String>()
            val values = ArrayList<String>()
            it.forEach {
                entries.add(it.name)
                values.add(it.id.toString())
            }
            calendarPrefs.entries = entries.toTypedArray()
            calendarPrefs.entryValues = values.toTypedArray()
            setPreferenceSummary(calendarPrefs, getCalendarSummary())
        })
    }

    /**
     * Gets the calendar summary, due to the calendars being saved by Id in shared prefs
     * need to convert that to the names
     */
    private fun getCalendarSummary(): String {
        val calendarPref = preferenceScreen.findPreference(getString(R.string.prefs_calendars_key)) as MultiSelectListPreference
        val entries = calendarPref.entries
        val values = calendarPref.entryValues
        val savedCalendars = preferenceScreen.sharedPreferences.getStringSet(calendarPref.key, null)
        if (savedCalendars == null || savedCalendars.isEmpty()) return ""
        val builder = StringBuilder("")
        //get the name of the calendar by finding the index of the id in the values list
        savedCalendars.forEach {
            val index = values.indexOf(it)
            if (index != -1 && index < entries.size) {
                builder.append(entries[index]).append("\n")
            }
        }
        return builder.toString()
    }

    /**
     * Get a list of all the users calendars to display in the
     * choose calendar preference. To work with async nature will need to
     * save it to an observed LiveData object
     */
    @SuppressLint("MissingPermission")
    private fun getCalendars() {
        appExecutors.diskIO().execute {
            val projection = arrayOf(CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val cr = context!!.contentResolver
            val cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
            val calendarList = ArrayList<Calendar>()
            if (cursor == null) return@execute
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID))
                calendarList.add(Calendar(name, id))
            }
            cursor.close()
            appExecutors.mainThread().execute { mCalenders.value = calendarList }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        //only deal with changes in the preference screen not other prefs
        val preferenceScreenKeys = setOf(getString(R.string.prefs_alerts_key),
                getString(R.string.prefs_calendars_key),
                getString(R.string.prefs_speed_key),
                getString(R.string.pref_units_key))
        if (!preferenceScreenKeys.contains(key)) return

        val preference : Preference = findPreference(key)
        if (preference is MultiSelectListPreference) {
            setPreferenceSummary(preference, getCalendarSummary())
            //if additional calendars where added (or removed) need to refresh the list
            val jobDispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            jobDispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(jobDispatcher))
        } else {
            val preferenceValue = sharedPreferences.getString(key, "")!!
            if (null != preference && preference !is CheckBoxPreference) {
                setPreferenceSummary(preference, preferenceValue)
            }
            if (key == getString(R.string.pref_units_key)) {
                //if units changed need to update the speed values to new unit system
                changeSpeedPrefsToUnitSystem(preferenceValue)
            }
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
            if (this.key == getString(R.string.prefs_alerts_key)) {
                preferenceManager.sharedPreferences.edit().putString(
                        getString(R.string.prefs_alerts_key), "10"
                ).apply()
            }
        }
    }

    private fun getDefaultUnitsByLocale(): String {
        var country: String
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

    private fun changeSpeedPrefsToUnitSystem(system: String) {
        val defaultSpeedPreference = findPreference(getString(R.string.prefs_speed_key)) as ListPreference
        var index = defaultSpeedPreference.findIndexOfValue(defaultSpeedPreference.value)
        if (index < 0) index = 2
        if (system == getString(R.string.pref_units_metric)) {
            //use metric entries
            defaultSpeedPreference.setEntries(R.array.pref_speed_options)
            defaultSpeedPreference.setEntryValues(R.array.pref_speed_values)
            defaultSpeedPreference.setValueIndex(index)
        } else if (system == getString(R.string.pref_units_imperial)) {
            //use imperial entries
            defaultSpeedPreference.setEntries(R.array.pref_speed_options_miles)
            defaultSpeedPreference.setEntryValues(R.array.pref_speed_values_miles)
            defaultSpeedPreference.setValueIndex(index)
        }
        setPreferenceSummary(defaultSpeedPreference,
                preferenceScreen.sharedPreferences.getString(defaultSpeedPreference.key, "")!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.settings_title)
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}