package com.saber.myapp

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.activity.ComponentActivity
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
    private lateinit var btnScanDate: ImageButton

    private var currentImagePath: String? = null

    companion object {
        private const val REQUEST_DATE_SCAN = 201
        private const val REQUEST_PRODUCT_CAMERA = 202
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
        btnScanDate = findViewById(R.id.btnScanDate)

        editBarcode.setText(barcodeValue)
        editName.setText(nameValue)
        editDate.setText(expiryValue)

        if (!imagePathValue.isNullOrEmpty()) {
            val file = File(imagePathValue)
            if (file.exists()) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                currentImagePath = imagePathValue
            }
        }

        btnScanDate.setOnClickListener {
            val intent = Intent(context, DateScannerActivity::class.java)
            (context as ComponentActivity).startActivityForResult(intent, REQUEST_DATE_SCAN)
        }

        btnCapture.setOnClickListener {
            val intent = Intent(context, ProductCameraActivity::class.java)
            (context as ComponentActivity).startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            val date = editDate.text.toString().trim()

            if (name.isEmpty() || date.isEmpty() || currentImagePath.isNullOrEmpty()) {
                Toast.makeText(context, "يرجى تعبئة كل الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            callback(name, date, currentImagePath!!)
            dismiss()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_DATE_SCAN -> {
                val date = data?.getStringExtra(DateScannerActivity.EXTRA_DATE)
                if (date != null) {
                    editDate.setText(date)
                }
            }

            REQUEST_PRODUCT_CAMERA -> {
                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)
                if (imagePath != null) {
                    currentImagePath = imagePath
                    imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
                }
            }
        }
    }
}
