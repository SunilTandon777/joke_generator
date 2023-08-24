package com.assignment.di.di

import androidx.room.Room
import com.assignment.domain.use_cases.FetchLocalJokes
import com.assignment.domain.use_cases.FetchJokesFromApi
import com.assignment.domain.use_cases.UpdateJokeIntoDB
import com.assignment.domain.repository.AppRepository
import com.assignment.domain.data_source.LocalSource
import com.assignment.domain.data_source.RemoteSource
import com.assignment.domain.network.Api
import com.assignment.room.RoomDB
import com.assignment.presentation.utils.Constants
import com.assignment.presentation.viewmodel.JokesViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<Api> {
        Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }
    single {
        Room.databaseBuilder(androidContext(), RoomDB::class.java, Constants.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    }
    factory { LocalSource(get()) }
    factory { RemoteSource(get()) }
    factory { AppRepository(get(), get()) }
    factory { FetchLocalJokes(get()) }
    factory { FetchJokesFromApi(get()) }
    factory { UpdateJokeIntoDB(get()) }
    viewModel { JokesViewModel(get(), get(), get()) }
}

