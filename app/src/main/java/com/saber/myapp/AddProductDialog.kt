package com.saber.myapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import java.io.File

class AddProductDialog(
    private val context: Context,
    private val barcodeValue: String,
    private val nameValue: String,
    private val expiryValue: String,
    private val imagePathValue: String?,
    private val callback: (name: String, expiryDate: String, imagePath: String) -> Unit
) : Dialog(context) {

    private lateinit var editName: EditText
    private lateinit var editDate: EditText
    private lateinit var editBarcode: EditText
    private lateinit var imageView: ImageView
    private lateinit var btnCapture: Button
    private lateinit var btnSave: Button

    private var currentImagePath: String? = null

    companion object {
    private const val REQUEST_PRODUCT_CAMERA = 202
    private const val REQUEST_DATE_SCAN = 201
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_product)

        editName = findViewById(R.id.editTextProductName)
        editDate = findViewById(R.id.editTextProductDate)
        editBarcode = findViewById(R.id.editTextBarcode)
        imageView = findViewById(R.id.imageViewProduct)
        btnCapture = findViewById(R.id.btnCaptureImage)
        btnSave = findViewById(R.id.btnSaveProduct)
        val btnScanDate = findViewById<ImageButton>(R.id.btnScanDate)

        // تعبئة البيانات
        editBarcode.setText(barcodeValue)
        editName.setText(nameValue)
        editDate.setText(expiryValue)

        // ✅ عرض الصورة (إنترنت أو محلية)
        if (!imagePathValue.isNullOrEmpty()) {

            if (imagePathValue.startsWith("http")) {
                Glide.with(context)
                    .load(imagePathValue)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)

                currentImagePath = imagePathValue

            } else {
                val file = File(imagePathValue)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                    currentImagePath = imagePathValue
                }
            }
        }

        // 📸 التقاط صورة جديدة
        btnCapture.setOnClickListener {
            val intent = Intent(context, ProductCameraActivity::class.java)
            (context as ComponentActivity).startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }
        btnScanDate.setOnClickListener {
    val activity = context as? ComponentActivity
    if (activity != null) {
        val intent = Intent(context, DateScannerActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_DATE_SCAN)
    } else {
        Toast.makeText(context, "خطأ في فتح الماسح", Toast.LENGTH_SHORT).show()
    }
}

        // 💾 حفظ المنتج
        btnSave.setOnClickListener {

            val name = editName.text.toString().trim()
            val date = editDate.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(context, "يرجى إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentImagePath == null) {
                Toast.makeText(context, "يرجى إضافة صورة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✔️ أهم نقطة: سيتم حفظ آخر صورة تم اختيارها
            callback(name, date, currentImagePath!!)
            dismiss()
        }
    }

    // 🔁 استقبال نتيجة الكاميرا
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == android.app.Activity.RESULT_OK) {

            if (requestCode == REQUEST_PRODUCT_CAMERA) {

                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)

                if (!imagePath.isNullOrEmpty()) {

                    currentImagePath = imagePath

                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    imageView.setImageBitmap(bitmap)

                    Toast.makeText(context, "تم تحديث الصورة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
