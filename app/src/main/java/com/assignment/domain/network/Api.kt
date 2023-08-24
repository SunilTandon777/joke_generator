package com.assignment.domain.network

import com.assignment.data.model.JokeModel
import retrofit2.Response
import retrofit2.http.GET

interface Api {
    @GET("/api?format=json")
    suspend fun getData(): Response<JokeModel>
}