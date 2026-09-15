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

    // متغيرات لتتبع بداية التفعيل ومنع التكرار اللانهائي
    private var isLanguageInitial = true
    private var isThemeInitial = true
    private var isExportGlobalInitial = true
    private var isExportOcrInitial = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. زر العودة
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. إعداد القوائم المنسدلة
        setupLanguageSpinner()
        setupThemeSpinner()
        setupExportGlobalSpinner()
        setupExportOcrSpinner()

        // 3. فتح القائمة عند الضغط على الصف كاملاً
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
                if (isLanguageInitial) {
                    isLanguageInitial = false
                    return
                }
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

        // ضبط الاختيار المبدئي حسب الوضع الحالي
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            spinner.setSelection(1, false)
        } else {
            spinner.setSelection(0, false)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isThemeInitial) {
                    isThemeInitial = false
                    return
                }
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
        spinner.setSelection(1, false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isExportGlobalInitial) {
                    isExportGlobalInitial = false
                    return
                }
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
        spinner.setSelection(1, false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isExportOcrInitial) {
                    isExportOcrInitial = false
                    return
                }
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
