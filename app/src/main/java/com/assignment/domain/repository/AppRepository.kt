package com.assignment.domain.repository

import com.assignment.data.model.JokeModel
import com.assignment.domain.data_source.LocalSource
import com.assignment.domain.data_source.RemoteSource

class
AppRepository(
    private val localSource: LocalSource,
    private val remoteSource: RemoteSource
) {
    suspend fun getJokesFromDB() = localSource.getJokesFromDB()
    suspend fun getJokesFromRemote() = remoteSource.getJokesFromRemote()
    suspend fun insertJoke(jokeModel: JokeModel) =
        localSource.insertJoke(jokeModel.toDataEntity())
}