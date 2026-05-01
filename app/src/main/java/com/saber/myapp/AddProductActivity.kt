package com.saber.myapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class AddProductActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editDate: EditText
    private lateinit var editBarcode: EditText
    private lateinit var imageView: ImageView
    private lateinit var btnCapture: Button

    private var currentImagePath: String? = null

    companion object {
        private const val REQUESTPRODUCTCAMERA = 202
        private const val REQUESTDATESCAN = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityaddproduct)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    saveProduct()
                    true
                }
                R.id.action_print -> {
                    Toast.makeText(this, "طباعة المنتج", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        editName = findViewById(R.id.editTextProductName)
        editDate = findViewById(R.id.editTextProductDate)
        editBarcode = findViewById(R.id.editTextBarcode)
        imageView = findViewById(R.id.imageViewProduct)
        btnCapture = findViewById(R.id.btnCaptureImage)

        // استلام البيانات من MainActivity
        val barcodeValue = intent.getStringExtra("barcode") ?: ""
        val nameValue = intent.getStringExtra("name") ?: ""
        val expiryValue = intent.getStringExtra("expiryDate") ?: ""
        val imagePathValue = intent.getStringExtra("imagePath")

        editBarcode.setText(barcodeValue)
        editName.setText(nameValue)
        editDate.setText(expiryValue)

        if (!imagePathValue.isNullOrEmpty()) {
            if (imagePathValue.startsWith("http")) {
                Glide.with(this).load(imagePathValue).into(imageView)
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

        btnCapture.setOnClickListener {
            val intent = Intent(this, ProductCameraActivity::class.java)
            startActivityForResult(intent, REQUESTPRODUCTCAMERA)
        }
    }

    private fun saveProduct() {
        val name = editName.text.toString().trim()
        val date = editDate.text.toString().trim()
        val barcode = editBarcode.text.toString().trim()

        if (name.isBlank() || date.isBlank() || currentImagePath == null) {
            Toast.makeText(this, "يرجى إدخال جميع البيانات", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent().apply {
            putExtra("barcode", barcode)
            putExtra("name", name)
            putExtra("expiryDate", date)
            putExtra("imagePath", currentImagePath)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUESTPRODUCTCAMERA) {
                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRAIMAGEPATH)
                if (!imagePath.isNullOrEmpty()) {
                    currentImagePath = imagePath
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
}
