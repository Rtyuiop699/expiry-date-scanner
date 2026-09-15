package com.saber.myapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
        val languages = arrayOf("العربية", "English")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteLanguage)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> setAppLocale("ar")
                1 -> Toast.makeText(
                    this,
                    "English language support will be added soon.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupThemeMenu() {
        val themes = arrayOf("فاتح", "داكن")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteTheme)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, themes)
        autoComplete.setAdapter(adapter)

        // تحديد الخيار المعروض حالياً بدون تنفيذ أي حدث
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        autoComplete.setText(if (isDarkMode) "داكن" else "فاتح", false)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun setupExportGlobalMenu() {
        val options = arrayOf("تشغيل", "إيقاف")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteExportGlobal)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val status = options[position]
            Toast.makeText(this, "تصدير المنتجات: $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExportOcrMenu() {
        val options = arrayOf("تشغيل", "إيقاف")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteExportOcr)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val status = options[position]
            Toast.makeText(this, "تصدير صور OCR: $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAppLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
