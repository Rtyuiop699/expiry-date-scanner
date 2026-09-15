package com.saber.myapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. زر العودة
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. إعداد القوائم المنسدلة (Spinners)
        setupLanguageSpinner()
        setupThemeSpinner()
        setupExportGlobalSpinner()
        setupExportOcrSpinner()

        // 3. جعل النقر على السطر بالكامل يفتح القائمة المنسدلة
        findViewById<View>(R.id.itemLanguage).setOnClickListener {
            findViewById<Spinner>(R.id.spinnerLanguage).performClick()
        }
        findViewById<View>(R.id.itemTheme).setOnClickListener {
            findViewById<Spinner>(R.id.spinnerTheme).performClick()
        }
        findViewById<View>(R.id.itemExportGlobal).setOnClickListener {
            findViewById<Spinner>(R.id.spinnerExportGlobal).performClick()
        }
        findViewById<View>(R.id.itemExportOcr).setOnClickListener {
            findViewById<Spinner>(R.id.spinnerExportOcr).performClick()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("العربية", "English")
        val spinner = findViewById<Spinner>(R.id.spinnerLanguage)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> setAppLocale("ar")
                    1 -> Toast.makeText(
                        this@SettingsActivity,
                        "English language support will be added soon.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupThemeSpinner() {
        val themes = arrayOf("فاتح", "داكن")
        val spinner = findViewById<Spinner>(R.id.spinnerTheme)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        spinner.adapter = adapter

        // ضبط التحديد الحالي بناءً على مظهر النظام/التطبيق
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            spinner.setSelection(1)
        } else {
            spinner.setSelection(0)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupExportGlobalSpinner() {
        val options = arrayOf("تشغيل", "إيقاف")
        val spinner = findViewById<Spinner>(R.id.spinnerExportGlobal)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.adapter = adapter
        spinner.setSelection(1) // افتراضياً: إيقاف

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val status = options[position]
                Toast.makeText(this@SettingsActivity, "تصدير المنتجات: $status", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupExportOcrSpinner() {
        val options = arrayOf("تشغيل", "إيقاف")
        val spinner = findViewById<Spinner>(R.id.spinnerExportOcr)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.adapter = adapter
        spinner.setSelection(1) // افتراضياً: إيقاف

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val status = options[position]
                Toast.makeText(this@SettingsActivity, "تصدير صور OCR: $status", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setAppLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
