package com.saber.myapp

import com.google.gson.annotations.SerializedName

data class ProductApiResponse(
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String,
    val code: String,
    val product: ProductDetails?
)

data class ProductDetails(
    @SerializedName("product_name")
    val productName: String?,
    val brands: String?,
    val quantity: String?,
    val categories: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("image_small_url")
    val imageSmallUrl: String?,
    @SerializedName("nutrition_grades")
    val nutritionGrades: String?,
    val countries: String?,
    val ingredients: String?,
    @SerializedName("additives_n")
    val additivesCount: Int?
)
