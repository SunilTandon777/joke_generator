package com.assignment.domain.data_source

import androidx.lifecycle.LiveData
import com.assignment.room.RoomDB
import com.assignment.room.JokeModelEntity

class LocalSource(private val roomDB: RoomDB) {
    suspend fun getJokesFromDB(): LiveData<List<JokeModelEntity>> {
        return roomDB.getDataDao().getAll()
    }

    suspend fun insertJoke(jokeModelEntity: JokeModelEntity): Long {
        return roomDB.getDataDao().insert(jokeModelEntity)
    }
}