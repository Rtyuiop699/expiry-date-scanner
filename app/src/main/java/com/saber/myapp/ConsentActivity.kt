package com.saber.myapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

class ConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        val checkPrivacy = findViewById<MaterialCheckBox>(R.id.checkPrivacy)
        val checkAi = findViewById<MaterialCheckBox>(R.id.checkAi)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)
        val txtLink = findViewById<TextView>(R.id.txtPolicyLink)

        txtLink.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://your-github-link.com/privacy.html")
                )
            )
        }

        checkPrivacy.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
            btnContinue.alpha = if (isChecked) 1f else 0.5f
        }

        btnContinue.setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("privacy_accepted", true)
                .putBoolean("ai_allowed", checkAi.isChecked)
                .putBoolean("first_run", false)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
