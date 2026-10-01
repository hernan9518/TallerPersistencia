package com.example.persistencia.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val cliente = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: TareaSugeridaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TareaSugeridaApiService::class.java)
    }
}