package com.saber.myapp

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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
    private val onItemLongClick: (View, Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>(),
    Filterable {

    private var filteredProducts: MutableList<Product> =
        products.toMutableList()

    // =========================================================
    // وضع التحديد المتعدد
    // =========================================================

    private var selectionMode = false

    // نخزن ID المنتجات المحددة
    private val selectedProductIds =
        mutableSetOf<Int>()


    // =========================================================
    // ViewHolder
    // =========================================================

    class ProductViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

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

        // مربع التحديد
        val checkBox: CheckBox =
            itemView.findViewById(
                R.id.checkBoxSelect
            )
    }


    // =========================================================
    // تحديث القائمة
    // =========================================================

    fun setProducts(
        newProducts: List<Product>
    ) {

        products.clear()
        products.addAll(newProducts)

        filteredProducts.clear()
        filteredProducts.addAll(newProducts)

        // إزالة IDs لم تعد موجودة
        val validIds =
            newProducts
                .map { it.id }
                .toSet()

        selectedProductIds.retainAll(
            validIds
        )

        notifyDataSetChanged()
    }


    // =========================================================
    // تفعيل / إيقاف وضع التحديد
    // =========================================================

    fun setSelectionMode(
        enabled: Boolean
    ) {

        selectionMode = enabled

        if (!enabled) {

            selectedProductIds.clear()
        }

        notifyDataSetChanged()
    }


    // =========================================================
    // إلغاء جميع التحديدات
    // =========================================================

    fun clearSelection() {

        selectedProductIds.clear()

        notifyDataSetChanged()
    }


    // =========================================================
    // الحصول على المنتجات المحددة
    // =========================================================

    fun getSelectedProducts(): List<Product> {

        return products.filter { product ->

            selectedProductIds.contains(
                product.id
            )
        }
    }


    // =========================================================
    // تغيير حالة تحديد منتج
    // =========================================================

    private fun toggleSelection(
        product: Product
    ) {

        if (
            selectedProductIds.contains(
                product.id
            )
        ) {

            selectedProductIds.remove(
                product.id
            )

        } else {

            selectedProductIds.add(
                product.id
            )
        }

        notifyDataSetChanged()
    }


    // =========================================================
    // الحصول على منتج حسب الموضع
    // =========================================================

    fun getProductAt(
        position: Int
    ): Product {

        return filteredProducts[position]
    }


    // =========================================================
    // حذف عنصر من القائمة
    // =========================================================

    fun removeAt(
        position: Int
    ) {

        val item =
            filteredProducts[position]

        filteredProducts.removeAt(
            position
        )

        products.remove(item)

        selectedProductIds.remove(
            item.id
        )

        notifyItemRemoved(
            position
        )
    }


    // =========================================================
    // إنشاء العنصر
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view =
            LayoutInflater.from(
                parent.context
            ).inflate(
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


        // =====================================================
        // اسم المنتج
        // =====================================================

        holder.nameView.text =
            product.name


        // =====================================================
        // تاريخ الانتهاء
        // =====================================================

        holder.expiryView.text =
            product.expiryDate


        // =====================================================
        // حساب الأيام المتبقية
        // =====================================================

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


        // =====================================================
        // الباركود
        // =====================================================

        holder.barcodeView.text =
            "Barcode: ${product.barcode}"


        // =====================================================
        // صورة المنتج
        // =====================================================

        val path =
            product.imagePath

        when {

            // -------------------------------------------------
            // صورة محلية
            // -------------------------------------------------

            !path.isNullOrEmpty() &&
                    !path.startsWith("http") -> {

                val file =
                    java.io.File(path)

                if (file.exists()) {

                    val bitmap =
                        BitmapFactory.decodeFile(
                            file.absolutePath
                        )

                    holder.imageView
                        .setImageBitmap(
                            bitmap
                        )

                } else {

                    holder.imageView
                        .setImageResource(
                            android.R.drawable
                                .ic_menu_report_image
                        )
                }
            }


            // -------------------------------------------------
            // صورة من الإنترنت
            // -------------------------------------------------

            !path.isNullOrEmpty() &&
                    path.startsWith("http") -> {

                com.bumptech.glide.Glide
                    .with(
                        holder.itemView.context
                    )
                    .load(path)
                    .placeholder(
                        android.R.drawable
                            .progress_horizontal
                    )
                    .error(
                        android.R.drawable
                            .ic_menu_report_image
                    )
                    .into(
                        holder.imageView
                    )
            }


            // -------------------------------------------------
            // لا توجد صورة
            // -------------------------------------------------

            else -> {

                holder.imageView
                    .setImageResource(
                        android.R.drawable
                            .ic_menu_report_image
                    )
            }
        }


        // =====================================================
        // وضع التحديد
        // =====================================================

        val isSelected =
            selectedProductIds.contains(
                product.id
            )


        if (selectionMode) {

            holder.checkBox.visibility =
                View.VISIBLE

            holder.checkBox.isChecked =
                isSelected

        } else {

            holder.checkBox.visibility =
                View.GONE

            holder.checkBox.isChecked =
                false
        }


        // =====================================================
        // خلفية المنتج المحدد
        // =====================================================

        if (
            selectionMode &&
            isSelected
        ) {

            holder.itemView.setBackgroundColor(
                Color.parseColor(
                    "#FFF0F0"
                )
            )

        } else {

            holder.itemView.setBackgroundColor(
                Color.TRANSPARENT
            )
        }


        // =====================================================
        // الضغط على مربع التحديد
        // =====================================================

        holder.checkBox.setOnClickListener {

            if (selectionMode) {

                toggleSelection(
                    product
                )
            }
        }


        // =====================================================
        // الضغط العادي على المنتج
        // =====================================================

        holder.itemView.setOnClickListener {

            if (selectionMode) {

                toggleSelection(
                    product
                )

            } else {

                onItemClick(
                    product
                )
            }
        }


        // =====================================================
        // الضغط المطول
        // =====================================================

        holder.itemView.setOnLongClickListener { view ->

            if (selectionMode) {

                // أثناء التحديد:
                // الضغط المطول يحدد / يلغي التحديد
                toggleSelection(
                    product
                )

                true

            } else {

                // الوضع الطبيعي:
                // يبقى الـBalloon كما كان
                onItemLongClick(
                    view,
                    product
                )

                true
            }
        }
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
                                .contains(
                                    query
                                ) ||

                            it.barcode
                                .lowercase()
                                .contains(
                                    query
                                )
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
                    if (
                        constraint
                            .isNullOrEmpty()
                    ) {

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
    }
