package com.assignment.domain.use_cases

import com.assignment.domain.repository.AppRepository

class FetchLocalJokes(private val appRepository: AppRepository) {
    suspend fun getJokesFromDB() = appRepository.getJokesFromDB()
}