package com.assignment.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.assignment.data.model.JokeModel

@Entity
class JokeModelEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var joke: String? = null
    var timeStamp: Long? = null

    fun toDataModel(): JokeModel {
        return JokeModel(joke, timeStamp)
    }
}