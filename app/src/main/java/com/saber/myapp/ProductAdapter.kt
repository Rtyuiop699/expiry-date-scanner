package com.saber.myapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.Filter
import android.widget.Filterable

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>(), Filterable {

    private var filteredProducts: MutableList<Product> = products.toMutableList()

    // ✅ تحديث القائمة عند تحميل المنتجات من قاعدة البيانات
    fun setProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        filteredProducts.clear()
        filteredProducts.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun getProductAt(position: Int): Product {
        return filteredProducts[position]
    }

    fun removeAt(position: Int) {
        val item = filteredProducts[position]
        filteredProducts.removeAt(position)
        products.remove(item)
        notifyItemRemoved(position)
    }

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

        holder.nameView.text = product.name
        holder.expiryView.text = product.expiryDate
        holder.barcodeView.text = "Barcode: ${product.barcode}"

        val path = product.imagePath

        when {
            // 📁 صورة محلية
            !path.isNullOrEmpty() && !path.startsWith("http") -> {
                val file = java.io.File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    holder.imageView.setImageBitmap(bitmap)
                } else {
                    holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            // 🌐 صورة من الإنترنت
            !path.isNullOrEmpty() && path.startsWith("http") -> {
                com.bumptech.glide.Glide.with(holder.itemView.context)
                    .load(path)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.imageView)
            }

            // ❌ لا يوجد صورة
            else -> {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount() = filteredProducts.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim()
                val results = if (query.isNullOrEmpty()) {
                    products
                } else {
                    products.filter { it.name.lowercase().startsWith(query) }
                }
                val filterResults = FilterResults()
                filterResults.values = results
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredProducts = if (constraint.isNullOrEmpty()) {
                    products.toMutableList()
                } else {
                    (results?.values as? List<Product>)?.toMutableList() ?: mutableListOf()
                }
                notifyDataSetChanged()
            }
        }
    }
}
