package com.saber.myapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,brands,quantity,image_url,image_small_url,nutrition_grades,categories,countries,ingredients,additives_n"
    ): Call<ProductApiResponse>
}
