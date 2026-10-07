package com.saber.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saber.myapp.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnReviewPrivacy.setOnClickListener {
            startActivity(
                android.content.Intent(this, ConsentActivity::class.java)
                    .putExtra("review_mode", true)
            )
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
