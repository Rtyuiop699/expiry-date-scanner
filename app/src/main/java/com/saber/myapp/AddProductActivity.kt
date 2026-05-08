package com.saber.myapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.saber.myapp.databinding.ActivityAddProductBinding
import java.io.File
import java.util.Calendar
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var databaseHelper: DatabaseHelper

    private var currentImagePath: String? = null

    companion object {
        private const val REQUEST_PRODUCT_CAMERA = 202
        private const val REQUEST_DATE_SCAN = 201
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        // 1. استقبال البيانات القادمة من Intent (مثلاً من الماسح الضوئي)
        val barcodeValue = intent.getStringExtra("BARCODE_EXTRA") ?: ""
        val nameValue = intent.getStringExtra("NAME_EXTRA") ?: ""
        val expiryValue = intent.getStringExtra("EXPIRY_EXTRA") ?: ""
        val imagePathValue = intent.getStringExtra("IMAGE_PATH_EXTRA")

        // 2. وضع البيانات في حقول النصوص (EditText)
        binding.editTextBarcode.setText(barcodeValue)
        binding.editTextProductName.setText(nameValue)
        binding.editTextDate.setText(expiryValue)

        // 3. تحميل الصورة إذا كانت موجودة
        if (!imagePathValue.isNullOrEmpty()) {
            currentImagePath = imagePathValue // تحديث المسار الحالي
            if (imagePathValue.startsWith("http")) {
                Glide.with(this)
                    .load(imagePathValue)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imageViewProduct)
            } else {
                val file = File(imagePathValue)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.imageViewProduct.setImageBitmap(bitmap)
                }
            }
        }
    
        

        // زر الكاميرا
        binding.btnCaptureImage.setOnClickListener {
            val intent = Intent(this, ProductCameraActivity::class.java)
            startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        // زر الماسح للتاريخ
        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, DateScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_DATE_SCAN)
        }

        // زر الحفظ
      //  binding.btnSaveProduct.setOnClickListener {
      //      saveProduct()
   //     }

        // زر يوجد باكت ✔️
        binding.btnHasPack.setOnClickListener {
            Toast.makeText(this, "تم تحديد أن المنتج يحتوي على باكت", Toast.LENGTH_SHORT).show()
        }

        // زر احسب
        binding.btnCalculate.setOnClickListener {
            calculateQuantity()
        }

        setupToolbar()
    }
private fun setupToolbar() {
    // 1. شحن المنيو يدوياً باستخدام اسم الملف الصحيح
    binding.topAppBar.inflateMenu(R.menu.addproductmenu) 

    // 2. تأكد من حذف سطر setSupportActionBar(binding.topAppBar) إذا كان موجوداً

    binding.topAppBar.setNavigationOnClickListener { finish() }

    binding.topAppBar.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            // استخدام الـ IDs الموجودة داخل ملف الـ XML الخاص بك
            R.id.action_save -> { 
                saveProduct()
                true 
            }
            R.id.action_print -> { 
                Toast.makeText(this, "طباعة", Toast.LENGTH_SHORT).show()
                true 
            }
            R.id.action_pdf -> { 
                Toast.makeText(this, "توليد ملف PDF", Toast.LENGTH_SHORT).show()
                true 
            }
            R.id.action_delete -> { 
                Toast.makeText(this, "حذف المنتج", Toast.LENGTH_SHORT).show()
                true 
            }
            R.id.action_barcode -> {
                // يمكنك إضافة أكشن هنا لفتح الكاميرا للباركود
                Toast.makeText(this, "فتح الماسح الضوئي", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }
}


private fun saveProduct() {
    val name = binding.editTextProductName.text.toString().trim()
    val rawDate = binding.editTextDate.text.toString().trim()
    val normalizedDate = normalizeDate(rawDate)
    val barcode = binding.editTextBarcode.text.toString().trim()

    if (name.isBlank()) {
        Toast.makeText(this, "يرجى إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
        return
    }
    if (rawDate.isBlank()) {
        Toast.makeText(this, "يرجى إدخال تاريخ الصلاحية", Toast.LENGTH_SHORT).show()
        return
    }
    if (normalizedDate == null) {
        Toast.makeText(this, "❌ صيغة التاريخ غير مفهومة", Toast.LENGTH_SHORT).show()
        return
    }
    if (currentImagePath == null) {
        Toast.makeText(this, "يرجى إضافة صورة", Toast.LENGTH_SHORT).show()
        return
    }

    // حساب الكمية من الحقول الجديدة
    val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
    val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
    val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0
    val quantity = if (carton > 0 && pack > 0 && piece > 0) carton * pack * piece else 1

    Toast.makeText(this, "✅ تم تحويل التاريخ إلى: $normalizedDate", Toast.LENGTH_LONG).show()

    // إنشاء المنتج وتمرير كل الباراميترات المطلوبة
    val product = Product(
        id = 0, // افتراضي
        barcode = barcode,
        name = name,
        expiryDate = normalizedDate,
        quantity = quantity,
        imagePath = currentImagePath!!
    )

    databaseHelper.addProduct(product)

    setResult(RESULT_OK)
    finish()
}

    private fun calculateQuantity() {
        val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
        val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
        val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0

        val result = carton * pack * piece
        binding.editResult.setText(result.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PRODUCT_CAMERA) {
                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)
                if (!imagePath.isNullOrEmpty()) {
                    currentImagePath = imagePath
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    binding.imageViewProduct.setImageBitmap(bitmap)
                    Toast.makeText(this, "تم تحديث الصورة", Toast.LENGTH_SHORT).show()
                }
            }
            if (requestCode == REQUEST_DATE_SCAN) {
                val date = data?.getStringExtra(DateScannerActivity.EXTRA_DATE)
                if (!date.isNullOrEmpty()) {
                    binding.editTextDate.setText(date)
                    Toast.makeText(this, "تم تحديث التاريخ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔁 دوال التاريخ (من الديالوج)
    private fun normalizeDate(input: String): String? {
        return extractDateFromText(input)
    }

    private fun extractDateFromText(text: String): String? {
        val cleanedText = fixCommonOCRMistakes(
            text.replace("\n", " ").replace(",", " ").trim()
        )
        // انسخ الـ Regex والدوال المساعدة من AddProductDialog هنا
        return null
    }

    private fun fixCommonOCRMistakes(text: String): String {
        return text.replace("O", "0").replace("I", "1").replace("S", "5")
    }

    // باقي الدوال: chooseBestDate, formatCalendar, isValidDateFromString, monthNameToNumber
}
