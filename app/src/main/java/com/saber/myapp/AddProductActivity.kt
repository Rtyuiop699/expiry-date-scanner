package com.saber.myapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saber.myapp.databinding.ActivityAddProductBinding

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        // 1. استلام الباركود وعرضه في الحقل المخصص
        val barcode = intent.getStringExtra("BARCODE_EXTRA") ?: ""
        binding.editTextBarcode.setText(barcode)

        setupToolbar()

        // 2. التعامل مع أزرار الصور الموجودة في الـ XML
        binding.btnCaptureImage.setOnClickListener {
            Toast.makeText(this, "سيتم تفعيل الكاميرا قريباً", Toast.LENGTH_SHORT).show()
        }

        binding.btnChooseImage.setOnClickListener {
            Toast.makeText(this, "سيتم تفعيل اختيار الصور قريباً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        // التعامل مع زر الحفظ الموجود في "المنيو" العلوي
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // افترضت أن ID زر الحفظ في ملف addproductmenu هو btnSaveAction
                // تأكد من فتحه ومطابقة الـ ID
                R.id.btnSaveAction -> {
                    saveProduct()
                    true
                }
                else -> false
            }
        }
    }

    private fun saveProduct() {
        val name = binding.editTextProductName.text.toString()
        val barcode = binding.editTextBarcode.text.toString()

        if (name.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
            return
        }

        // منطق الحفظ في قاعدة البيانات
        // val newProduct = Product(barcode = barcode, name = name, ...)
        // databaseHelper.addProduct(newProduct)

        setResult(RESULT_OK)
        finish()
    }
}
