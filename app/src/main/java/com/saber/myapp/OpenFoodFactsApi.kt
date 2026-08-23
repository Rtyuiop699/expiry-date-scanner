package com.saber.myapp

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// 1. ده الـ Interface بتاعك - صح
interface OpenFoodFactsService {
    @GET("api/v0/product/{barcode}.json")
    fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,brands,quantity,image_url,image_small_url"
    ): Call<ProductApiResponse>
}

// 2. ده الـ Object اللي بنكلم منه - ده الناقص
object OpenFoodFactsApi {

    private const val BASE_URL = "https://world.openfoodfacts.org/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(OpenFoodFactsService::class.java)

    // دي الدالة اللي AddProductActivity بتستدعيها
    fun getProduct(barcode: String, callback: (ProductApiResponse?) -> Unit) {
        service.getProduct(barcode).enqueue(object : Callback<ProductApiResponse> {
            override fun onResponse(call: Call<ProductApiResponse>, response: Response<ProductApiResponse>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                callback(null)
            }
        })
    }
}
