package com.assignment.data.model

import com.assignment.room.JokeModelEntity

data class JokeModel(var joke: String?, var timeStamp: Long?) {
    fun toDataEntity() = JokeModelEntity().apply {
        joke = this@JokeModel.joke
        timeStamp = this@JokeModel.timeStamp
    }
}