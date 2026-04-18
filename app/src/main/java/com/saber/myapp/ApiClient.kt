package com.saber.myapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getClient(): Retrofit = retrofit

    val instance: OpenFoodFactsApi by lazy {
        retrofit.create(OpenFoodFactsApi::class.java)
    }
}
