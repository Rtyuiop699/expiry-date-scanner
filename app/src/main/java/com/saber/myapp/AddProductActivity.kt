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

        // استقبال البيانات من Intent
        val barcodeValue = intent.getStringExtra("BARCODE_EXTRA") ?: ""
        val nameValue = intent.getStringExtra("NAME_EXTRA") ?: ""
        val expiryValue = intent.getStringExtra("EXPIRY_EXTRA") ?: ""
        val imagePathValue = intent.getStringExtra("IMAGE_PATH_EXTRA")

        binding.editTextBarcode.setText(barcodeValue)
        binding.editTextProductName.setText(nameValue)
        binding.editTextDate.setText(expiryValue)

        // تحميل الصورة إذا موجودة
        if (!imagePathValue.isNullOrEmpty()) {
            if (imagePathValue.startsWith("http")) {
                Glide.with(this)
                    .load(imagePathValue)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imageViewProduct)
                currentImagePath = imagePathValue
            } else {
                val file = File(imagePathValue)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.imageViewProduct.setImageBitmap(bitmap)
                    currentImagePath = imagePathValue
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
        binding.btnSaveProduct.setOnClickListener {
            saveProduct()
        }

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
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnSaveAction -> { saveProduct(); true }
                R.id.btnPrint -> { Toast.makeText(this, "طباعة", Toast.LENGTH_SHORT).show(); true }
                R.id.btnPdf -> { Toast.makeText(this, "PDF", Toast.LENGTH_SHORT).show(); true }
                R.id.btnDelete -> { Toast.makeText(this, "حذف", Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
    }

    private fun saveProduct() {
        val name = binding.editTextProductName.text.toString().trim()
        val rawDate = binding.editTextDate.text.toString().trim()
        val normalizedDate = normalizeDate(rawDate)
        val barcode = binding.editTextBarcode.text.toString()

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

        Toast.makeText(this, "✅ تم تحويل التاريخ إلى: $normalizedDate", Toast.LENGTH_LONG).show()

        // حفظ في قاعدة البيانات
        databaseHelper.addProduct(Product(barcode, name, normalizedDate, currentImagePath!!))

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
