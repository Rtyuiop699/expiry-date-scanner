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

        // 1. استقبال الباركود القادم من MainActivity
        val barcode = intent.getStringExtra("BARCODE_EXTRA") ?: ""
        
        // يمكنك تعيين الباركود في حقل نصي إذا كان لديك واحد في التصميم
        // binding.etBarcode.setText(barcode)

        setupToolbar()

        // 2. منطق زر الحفظ (أضف هذا في دالة الحفظ لديك)
        binding.btnSave.setOnClickListener {
            saveProduct(barcode)
        }
    }

    private fun saveProduct(barcode: String) {
        // هنا تجمع البيانات من الواجهة (اسم المنتج، التاريخ، الخ)
        // val name = binding.etProductName.text.toString()
        
        // بعد الحفظ في قاعدة البيانات:
        // databaseHelper.addProduct(...)

        // 3. إرسال إشارة النجاح للـ MainActivity لإخبارها بتحديث القائمة
        setResult(RESULT_OK)
        finish() // العودة للخلف
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationIcon(R.drawable.icarrowback)
        binding.topAppBar.setNavigationOnClickListener {
            finish() // استخدام finish أفضل من onBackPressed هنا
        }
    }
}
