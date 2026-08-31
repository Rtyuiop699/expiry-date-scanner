package com.saber.myapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onItemClick: (Product) -> Unit,
    private val onItemLongClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>(),
    Filterable {

    private var filteredProducts: MutableList<Product> =
        products.toMutableList()

    // =========================================================
    // تحديث القائمة
    // =========================================================

    fun setProducts(newProducts: List<Product>) {

        products.clear()
        products.addAll(newProducts)

        filteredProducts.clear()
        filteredProducts.addAll(newProducts)

        notifyDataSetChanged()
    }

    // =========================================================
    // الحصول على المنتج حسب الموقع
    // =========================================================

    fun getProductAt(position: Int): Product {
        return filteredProducts[position]
    }

    // =========================================================
    // حذف عنصر
    // =========================================================

    fun removeAt(position: Int) {

        val item =
            filteredProducts[position]

        filteredProducts.removeAt(position)

        products.remove(item)

        notifyItemRemoved(position)
    }

    // =========================================================
    // ViewHolder
    // =========================================================

    class ProductViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView =
            itemView.findViewById(
                R.id.imageViewProduct
            )

        val nameView: TextView =
            itemView.findViewById(
                R.id.textViewName
            )

        val expiryView: TextView =
            itemView.findViewById(
                R.id.textViewExpiry
            )

        val remainingView: TextView =
            itemView.findViewById(
                R.id.textViewRemaining
            )

        val barcodeView: TextView =
            itemView.findViewById(
                R.id.textViewBarcode
            )
    }

    // =========================================================
    // إنشاء ViewHolder
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_product,
                    parent,
                    false
                )

        return ProductViewHolder(view)
    }

    // =========================================================
    // ربط البيانات
    // =========================================================

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int
    ) {

        val product =
            filteredProducts[position]

        // -----------------------------------------------------
        // اسم المنتج
        // -----------------------------------------------------

        holder.nameView.text =
            product.name

        // -----------------------------------------------------
        // تاريخ الانتهاء
        // -----------------------------------------------------

        holder.expiryView.text =
            product.expiryDate

        // -----------------------------------------------------
        // حساب الأيام المتبقية
        // -----------------------------------------------------

        try {

            val expiryDate =
                LocalDate.parse(
                    product.expiryDate
                )

            val today =
                LocalDate.now()

            val daysRemaining =
                ChronoUnit.DAYS.between(
                    today,
                    expiryDate
                )

            when {

                daysRemaining > 1 -> {

                    holder.remainingView.text =
                        "متبقي $daysRemaining يوم"
                }

                daysRemaining == 1L -> {

                    holder.remainingView.text =
                        "متبقي يوم واحد"
                }

                daysRemaining == 0L -> {

                    holder.remainingView.text =
                        "ينتهي اليوم"
                }

                else -> {

                    val expiredDays =
                        -daysRemaining

                    if (expiredDays == 1L) {

                        holder.remainingView.text =
                            "منتهي منذ يوم واحد"

                    } else {

                        holder.remainingView.text =
                            "منتهي منذ $expiredDays يوم"
                    }
                }
            }

        } catch (e: Exception) {

            holder.remainingView.text =
                "تاريخ غير صالح"
        }

        // -----------------------------------------------------
        // الباركود
        // -----------------------------------------------------

        holder.barcodeView.text =
            "Barcode: ${product.barcode}"

        // -----------------------------------------------------
        // صورة المنتج
        // -----------------------------------------------------

        val path =
            product.imagePath

        when {

            // صورة محلية
                     // صورة محلية
            !path.isNullOrEmpty() && !path.startsWith("http") -> {
                val file = java.io.File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    holder.imageView.setImageBitmap(bitmap)
                } else {
                    holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            // صورة من الإنترنت
            !path.isNullOrEmpty() && path.startsWith("http") -> {
                com.bumptech.glide.Glide
                    .with(holder.itemView.context)
                    .load(path)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.imageView)
            }

            // لا توجد صورة
            else -> {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        // =====================================================
        // الضغط العادي
        // =====================================================

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }

        // =====================================================
        // الضغط المطول (النافذة المنبثقة بالسهم)
        // =====================================================

        holder.itemView.setOnLongClickListener { view ->
            val context = view.context

            val balloon = com.skydoves.balloon.Balloon.Builder(context)
                .setLayout(R.layout.layout_popup_menu)
                .setArrowSize(10)
                .setArrowOrientation(com.skydoves.balloon.ArrowOrientation.BOTTOM)
                .setArrowPositionRules(com.skydoves.balloon.ArrowPositionRules.ALIGN_ANCHOR)
                .setCornerRadius(14f)
                .setBackgroundColor(android.graphics.Color.WHITE)
                .setElevation(8)
                .setDismissWhenClicked(true)
                .setBalloonAnimation(com.skydoves.balloon.BalloonAnimation.FADE)
                .build()

            val viewMenu = balloon.getContentView()

            // 1. تعديل
            viewMenu.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnActionEdit)
                ?.setOnClickListener {
                    // يمكنك استدعاء دالة التعديل هنا أو استخدام الـ listener الخاص بك
                    onItemLongClick(product)
                    balloon.dismiss()
                }

            // 2. PDF
            viewMenu.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnActionPdf)
                ?.setOnClickListener {
                    balloon.dismiss()
                }

            // 3. طباعة
            viewMenu.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnActionPrint)
                ?.setOnClickListener {
                    balloon.dismiss()
                }

            // 4. حذف
            viewMenu.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnActionDelete)
                ?.setOnClickListener {
                    balloon.dismiss()
                }

            // إظهار النافذة المنبثقة فوق العنصر المضغوط
            balloon.showAlignTop(view)

            true
        }
        

    // =========================================================
    // عدد العناصر
    // =========================================================

    override fun getItemCount(): Int {

        return filteredProducts.size
    }

    // =========================================================
    // البحث
    // =========================================================

    override fun getFilter(): Filter {

        return object : Filter() {

            override fun performFiltering(
                constraint: CharSequence?
            ): FilterResults {

                val query =
                    constraint
                        ?.toString()
                        ?.lowercase()
                        ?.trim()

                val results =

                    if (query.isNullOrEmpty()) {

                        products

                    } else {

                        products.filter {

                            it.name
                                .lowercase()
                                .startsWith(query)
                        }
                    }

                val filterResults =
                    FilterResults()

                filterResults.values =
                    results

                return filterResults
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {

                filteredProducts =

                    if (constraint.isNullOrEmpty()) {

                        products.toMutableList()

                    } else {

                        (
                            results?.values
                                as? List<Product>
                            )
                            ?.toMutableList()
                            ?: mutableListOf()
                    }

                notifyDataSetChanged()
            }
        }
    }

