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

        // استقبال الباركود
        val barcode = intent.getStringExtra("BARCODE_EXTRA") ?: ""
        binding.editTextBarcode.setText(barcode)

        setupToolbar()

        // أزرار الصور
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

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
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

        // هنا يمكنك إضافة منطق الحفظ الفعلي في قاعدة البيانات
        // databaseHelper.addProduct(Product(barcode, name, "", ""))

        setResult(RESULT_OK)
        finish()
    }
}
