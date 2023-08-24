package com.assignment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assignment.domain.use_cases.FetchJokesFromApi
import com.assignment.domain.use_cases.FetchLocalJokes
import com.assignment.domain.use_cases.UpdateJokeIntoDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JokesViewModel(
    private val getFetchLocalJokes: FetchLocalJokes,
    private val fetchJokesFromApi: FetchJokesFromApi,
    private val updateJokeIntoDB: UpdateJokeIntoDB
) : ViewModel() {

    suspend fun getJokesFromDB() = getFetchLocalJokes.getJokesFromDB()

    fun getRemoteJoke() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = fetchJokesFromApi.invoke()
                if (response.isSuccessful) {
                    response.body()?.let { dataModel ->
                        dataModel.timeStamp = System.currentTimeMillis()
                        updateJokeIntoDB.invoke(dataModel)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}