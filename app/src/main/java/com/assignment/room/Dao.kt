package com.assignment.room

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao

@Dao
interface Dao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(jokeModelEntity: JokeModelEntity):Long

    @Delete
    fun delete(jokeModelEntity: JokeModelEntity)

    @Query("select * from JokeModelEntity ORDER BY timestamp DESC limit 10")
    fun getAll(): LiveData<List<JokeModelEntity>>
}