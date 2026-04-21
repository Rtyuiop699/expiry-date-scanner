package com.saber.myapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
        holder.expiryDateDisplay(holder, product.expiryDate)

        // الباركود تحت التاريخ
        holder.barcodeView.text = "Barcode: ${product.barcode}"

        // تحسين عرض الصورة باستخدام Glide لدعم الروابط والملفات المحلية
        Glide.with(holder.itemView.context)
            .load(product.imagePath) // Glide سيميز تلقائياً بين URL والمسار المحلي
            .placeholder(android.R.drawable.ic_menu_gallery) // صورة مؤقتة أثناء التحميل
            .error(android.R.drawable.ic_menu_report_image) // صورة تظهر عند الخطأ
            .centerCrop() // لضمان مظهر متناسق للصور
            .into(holder.imageView)

        // الضغط على العنصر لفتح نافذة التعديل
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    // دالة مساعدة للحفاظ على تنسيق اللون
    private fun ProductViewHolder.expiryDateDisplay(holder: ProductViewHolder, date: String) {
        this.expiryView.text = date
        this.expiryView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
    }

    override fun getItemCount() = products.size
}
