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
    }

    // =========================================================
    // تحديث البيانات
    // =========================================================

    fun refreshData() {

        adapter?.notifyDataSetChanged()
    }
}
