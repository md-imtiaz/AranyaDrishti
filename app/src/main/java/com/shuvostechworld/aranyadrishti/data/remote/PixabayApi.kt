package com.shuvostechworld.aranyadrishti.data.remote

import com.shuvostechworld.aranyadrishti.data.model.PixabayResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApi {
    @GET("api/")
    suspend fun searchImages(
        @Query("key") key: String,
        @Query("q") query: String = "wildlife conservation",
        @Query("image_type") type: String = "photo",
        @Query("orientation") orientation: String = "horizontal"
    ): Response<PixabayResponse>
}

object RetrofitClient {
    private const val BASE_URL = "https://pixabay.com/"

    val pixabayApi: PixabayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PixabayApi::class.java)
    }
}
