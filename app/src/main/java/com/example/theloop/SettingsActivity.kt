package com.example.theloop

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.theloop.utils.AppConstants

class SettingsActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        radioGroup = findViewById(R.id.temp_unit_radio_group)
        val versionText = findViewById<TextView>(R.id.version_text)

        setupTemperatureUnits()

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Version Unknown"
        }
    }

    private fun setupTemperatureUnits() {
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val currentUnit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT)

        val unitsDisplay = resources.getStringArray(R.array.temp_units_display)
        val unitsValues = resources.getStringArray(R.array.temp_units_values)

        for (i in unitsDisplay.indices) {
            val radioButton = RadioButton(this)
            radioButton.text = unitsDisplay[i]
            radioButton.tag = unitsValues[i]
            radioButton.id = View.generateViewId() // Use index as ID
            radioGroup.addView(radioButton)

            if (unitsValues[i] == currentUnit) {
                radioButton.isChecked = true
            }
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButton = group.findViewById<RadioButton>(checkedId)
            val selectedValue = radioButton.tag as String
            prefs.edit().putString(AppConstants.KEY_TEMP_UNIT, selectedValue).apply()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
