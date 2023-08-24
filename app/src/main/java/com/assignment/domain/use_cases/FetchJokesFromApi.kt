package com.assignment.domain.use_cases

import com.assignment.domain.repository.AppRepository

class FetchJokesFromApi(private val appRepository: AppRepository) {
    suspend operator fun invoke() = appRepository.getJokesFromRemote()
}