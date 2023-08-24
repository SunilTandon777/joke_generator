package com.assignment.domain.use_cases

import com.assignment.data.model.JokeModel
import com.assignment.domain.repository.AppRepository

class UpdateJokeIntoDB(private val appRepository: AppRepository) {
    suspend fun invoke(jokeModel: JokeModel) = appRepository.insertJoke(jokeModel)
}