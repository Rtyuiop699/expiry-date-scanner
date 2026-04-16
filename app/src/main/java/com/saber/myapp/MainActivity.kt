package com.saber.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.appbar.MaterialToolbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 100

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fab: FloatingActionButton

    private val productList = mutableListOf<Product>()
    private var currentDialog: AddProductDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.inflateMenu(R.menu.toolbar_menu)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.btnSearch -> {
                    Toast.makeText(this, "بحث", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.btnHelp -> {
                    Toast.makeText(this, "مساعدة", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.btnSettings -> {
                    Toast.makeText(this, "إعدادات", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        databaseHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        loadProductsFromDatabase()
    }

    private fun checkCameraPermissionAndOpenScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openScanner()
        }
    }

    private fun openScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("قرّب الباركود من الكاميرا")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitScanActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun loadProductsFromDatabase() {
        productList.clear()
        productList.addAll(databaseHelper.getAllProducts())
        adapter.notifyDataSetChanged()
    }
}
