package com.saber.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saber.myapp.databinding.ActivityaddproductBinding

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityaddproductBinding

    // تعريف الثابت الذي يطلبه النظام
    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ربط الواجهة
        binding = ActivityaddproductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // مثال للوصول للمتغير الذي كان يسبب خطأ
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        
        setupToolbar()
    }

    private fun setupToolbar() {
        // بما أن الأيقونة الآن اسمها icarrowback بدون فواصل
        binding.toolbar.setNavigationIcon(R.drawable.icarrowback)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}
