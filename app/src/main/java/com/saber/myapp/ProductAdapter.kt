package com.saber.myapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>(), Filterable {

    private var filteredProducts: MutableList<Product> = products.toMutableList()

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
        val product = filteredProducts[position]

        // الاسم
        holder.nameView.text = product.name

        // التاريخ تحت الاسم بخط أسود
        holder.expiryView.text = product.expiryDate
        holder.expiryView.setTextColor(holder.itemView.context.getColor(android.R.color.black))

        // الباركود تحت التاريخ
        holder.barcodeView.text = "Barcode: ${product.barcode}"

        // الصورة في الجهة اليمنى باستخدام Glide
        Glide.with(holder.itemView.context)
            .load(product.imagePath)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(holder.imageView)

        // الضغط على العنصر لفتح نافذة التعديل
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount() = filteredProducts.size

    // ✅ دعم البحث
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val results = if (query.isEmpty()) {
                    products
                } else {
                    products.filter {
                        it.name.lowercase().contains(query) ||
                        it.barcode.lowercase().contains(query)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = results
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredProducts = (results?.values as? List<Product>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}
