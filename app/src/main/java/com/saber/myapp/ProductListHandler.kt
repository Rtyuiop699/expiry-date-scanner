package com.saber.myapp

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductListHandler(
    private val recyclerView: RecyclerView,
    private val onProductClicked: (Product) -> Unit,
    private val onProductLongClicked: (View, Product) -> Unit
) {

    private var adapter: ProductAdapter? = null

    // =========================================================
    // إعداد القائمة
    // =========================================================

    fun setup(products: MutableList<Product>) {

        if (adapter == null) {

            adapter = ProductAdapter(

                products,

                // الضغط العادي
                { product ->

                    onProductClicked(product)
                },

                // الضغط المطول
                { view, product ->

                    onProductLongClicked(
                        view,
                        product
                    )
                }
            )

            recyclerView.layoutManager =
                LinearLayoutManager(
                    recyclerView.context
                )

            recyclerView.adapter =
                adapter

        } else {

            // تحديث البيانات بدون إنشاء Adapter جديد
            adapter?.setProducts(
                products
            )
        }
    }

    // =========================================================
    // تفعيل / إيقاف وضع التحديد المتعدد
    // =========================================================

    fun setSelectionMode(
        enabled: Boolean
    ) {

        adapter?.setSelectionMode(
            enabled
        )
    }

    // =========================================================
    // إلغاء جميع التحديدات
    // =========================================================

    fun clearSelection() {

        adapter?.clearSelection()
    }

    // =========================================================
    // الحصول على المنتجات المحددة
    // =========================================================

    fun getSelectedProducts(): List<Product> {

        return adapter?.getSelectedProducts()
            ?: emptyList()
    }

    // =========================================================
    // تحديث القائمة
    // =========================================================

    fun refreshData() {

        adapter?.notifyDataSetChanged()
    }
}
