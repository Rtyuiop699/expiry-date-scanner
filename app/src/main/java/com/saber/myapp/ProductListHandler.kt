package com.saber.myapp

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductListHandler(
    private val recyclerView: RecyclerView,
    private val onProductClicked: (Product) -> Unit,
    private val onProductLongClicked: (Product) -> Unit
) {

    private var adapter: ProductAdapter? = null

    // =========================================================
    // إعداد القائمة
    // =========================================================

    fun setup(products: MutableList<Product>) {

        adapter = ProductAdapter(
            products = products,

            onItemClick = { product ->
                onProductClicked(product)
            },

            onItemLongClick = { product ->
                onProductLongClicked(product)
            }
        )

        recyclerView.layoutManager =
            LinearLayoutManager(
                recyclerView.context
            )

        recyclerView.adapter = adapter
    }

    // =========================================================
    // تحديث البيانات
    // =========================================================

    fun refreshData() {
        adapter?.notifyDataSetChanged()
    }

    // =========================================================
    // الحصول على View الخاص بمنتج معين
    // =========================================================

    fun getItemView(product: Product): View? {

        val currentAdapter =
            adapter ?: return null

        val count =
            currentAdapter.itemCount

        for (position in 0 until count) {

            val currentProduct =
                currentAdapter.getProductAt(position)

            if (currentProduct.id == product.id) {

                return recyclerView
                    .layoutManager
                    ?.findViewByPosition(position)
            }
        }

        return null
    }
}
