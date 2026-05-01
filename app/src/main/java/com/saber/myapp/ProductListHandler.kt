package com.saber.myapp

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductListHandler(
    private val recyclerView: RecyclerView,
    private val onProductClicked: (Product) -> Unit
) {
    private var adapter: ProductAdapter? = null

    fun setup(products: List<Product>) {
        adapter = ProductAdapter(products) { product ->
            onProductClicked(product)
        }
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter
    }

    fun refreshData() {
        adapter?.notifyDataSetChanged()
    }
}
