package com.saber.myapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // اختيار اللغة
        findViewById<View>(R.id.itemLanguage).setOnClickListener {
            showLanguageDialog()
        }

        // اختيار المظهر
        findViewById<View>(R.id.itemTheme).setOnClickListener {
            showThemeDialog()
        }

        // تصدير المنتجات
        findViewById<View>(R.id.itemExportGlobal).setOnClickListener {
            Toast.makeText(
                this,
                "تصدير المنتجات",
                Toast.LENGTH_SHORT
            ).show()
        }

        // تصدير صور OCR
        findViewById<View>(R.id.itemExportOcr).setOnClickListener {
            Toast.makeText(
                this,
                "تصدير صور OCR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("فاتح", "داكن")

        AlertDialog.Builder(this)
            .setTitle("اختر المظهر")
            .setItems(themes) { _, which ->
                when (which) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("عربي", "English")

        AlertDialog.Builder(this)
            .setTitle("اختر اللغة / Select Language")
            .setItems(languages) { _, which ->
                when (which) {
                    0 -> setAppLocale("ar")
                    1 -> Toast.makeText(
                        this,
                        "English language support will be added soon.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun setAppLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
