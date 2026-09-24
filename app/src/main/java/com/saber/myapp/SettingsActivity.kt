package com.saber.myapp

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // زر العودة
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupLanguageMenu()
        setupThemeMenu()
        setupExportGlobalMenu()
        setupExportOcrMenu()
    }

    private fun setupLanguageMenu() {
        val languages = arrayOf(getString(R.string.language_arabic), getString(R.string.language_english))
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteLanguage)
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            // إغلاق القائمة المنسدلة وإلغاء التركيز
            autoComplete.dismissDropDown()
            autoComplete.clearFocus()

            when (position) {
                0 -> setAppLocale("ar")
                1 -> setAppLocale("en")
            }
        }
    }
private fun setupThemeMenu() {
val themes = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark))
val autoComplete =
findViewById<AutoCompleteTextView>(R.id.autoCompleteTheme)

val adapter = NoFilterAdapter(
    this,
    android.R.layout.simple_spinner_dropdown_item,
    themes
)

autoComplete.setAdapter(adapter)

// قراءة الوضع المحفوظ
val preferences = getSharedPreferences(
    "app_settings",
    MODE_PRIVATE
)

val isDarkMode = preferences.getBoolean(
    "dark_mode",
    false
)

// عرض الوضع الحالي
autoComplete.setText(
    if (isDarkMode) getString(R.string.theme_dark) else getString(R.string.theme_light),
    false
)

autoComplete.setOnItemClickListener { _, _, position, _ ->

    // إغلاق القائمة المنسدلة
    autoComplete.dismissDropDown()

    when (position) {

        0 -> {
            // حفظ الوضع الفاتح
            preferences.edit()
                .putBoolean("dark_mode", false)
                .apply()

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        1 -> {
            // حفظ الوضع الداكن
            preferences.edit()
                .putBoolean("dark_mode", true)
                .apply()

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        }
    }
}

}
    
    private fun setupExportGlobalMenu() {
        val options = arrayOf(getString(R.string.enabled), getString(R.string.disabled))
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteExportGlobal)
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            // إغلاق القائمة المنسدلة وإلغاء التركيز
            autoComplete.dismissDropDown()
            autoComplete.clearFocus()

            val status = options[position]
            Toast.makeText(this, getString(R.string.export_products_status, status), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExportOcrMenu() {
        val options = arrayOf(getString(R.string.enabled), getString(R.string.disabled))
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteExportOcr)
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            // إغلاق القائمة المنسدلة وإلغاء التركيز
            autoComplete.dismissDropDown()
            autoComplete.clearFocus()

            val status = options[position]
            Toast.makeText(this, getString(R.string.export_ocr_status, status), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAppLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * كلاس مخصص لمنع الفلترة الذكية للـ AutoCompleteTextView
     */
    private inner class NoFilterAdapter<T>(
        context: Context,
        resource: Int,
        private val items: Array<T>
    ) : ArrayAdapter<T>(context, resource, items) {

        private val noFilter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = items
                results.count = items.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }

        override fun getFilter(): Filter {
            return noFilter
        }
    }
}
