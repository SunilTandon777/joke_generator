package com.assignment.domain.data_source

import com.assignment.data.model.JokeModel
import com.assignment.domain.network.Api
import retrofit2.Response

class RemoteSource(private val api: Api) {
    suspend fun getJokesFromRemote(): Response<JokeModel> {
        return api.getData()
    }
}