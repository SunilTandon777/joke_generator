package com.assignment.room

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao

@Dao
interface Dao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(jokeModelEntity: JokeModelEntity):Long

    @Query("select * from JokeModelEntity ORDER BY timestamp DESC limit 10")
    fun getStoredJokes(): LiveData<List<JokeModelEntity>>
}