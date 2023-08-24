package com.assignment.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [JokeModelEntity::class], version = 2, exportSchema = false)
abstract class RoomDB : RoomDatabase() {
    abstract fun getDataDao(): Dao
}