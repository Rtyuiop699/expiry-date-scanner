package com.saber.myapp

data class Product(
    val id: Int = 0,
    val barcode: String,
    val name: String,
    val expiryDate: String,
    val quantity: Int = 1,
    val imagePath: String
)
