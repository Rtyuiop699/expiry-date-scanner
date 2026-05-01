package com.saber.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saber.myapp.databinding.ActivityAddProductBinding

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // استخدام الاسم الصحيح المولّد من activity_add_product.xml
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
    }

    private fun setupToolbar() {
        // تأكد أن الأيقونة icarrowback موجودة في drawable
        binding.toolbar.setNavigationIcon(R.drawable.icarrowback)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}
