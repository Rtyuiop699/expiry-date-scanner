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
        val languages = arrayOf("العربية", "English")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteLanguage)
        
        // استخدام المحول المخصص لمنع الفلترة
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
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
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        autoComplete.setAdapter(adapter)

        // ضبط النص المعروض حالياً بدون تفعيل الفلترة (false)
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
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val status = options[position]
            Toast.makeText(this, "تصدير المنتجات: $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExportOcrMenu() {
        val options = arrayOf("تشغيل", "إيقاف")
        val autoComplete = findViewById<AutoCompleteTextView>(R.id.autoCompleteExportOcr)
        
        val adapter = NoFilterAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
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

    /**
     * كلاس مخصص لمنع الفلترة الذكية للـ AutoCompleteTextView
     * يضمن إظهار كل عناصر القائمة دائماً حتى بعد اختيار أحد الخيارات
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
