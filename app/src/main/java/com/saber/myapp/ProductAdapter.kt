package com.saber.myapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ProductAdapter(
    private val products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewProduct)
        val nameView: TextView = itemView.findViewById(R.id.textViewName)
        val expiryView: TextView = itemView.findViewById(R.id.textViewExpiry)
        val barcodeView: TextView = itemView.findViewById(R.id.textViewBarcode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // الاسم
        holder.nameView.text = product.name

        // التاريخ تحت الاسم بخط أسود
        holder.expiryView.text = product.expiryDate
        holder.expiryView.setTextColor(holder.itemView.context.getColor(android.R.color.black))

        // الباركود تحت التاريخ
        holder.barcodeView.text = "Barcode: ${product.barcode}"

        // الصورة
        val file = product.imagePath?.let { path -> File(path) }
        if (file != null && file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // الضغط على العنصر لفتح نافذة التعديل
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount() = products.size
}
