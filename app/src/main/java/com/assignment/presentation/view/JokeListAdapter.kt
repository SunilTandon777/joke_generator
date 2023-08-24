package com.assignment.presentation.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.assignment.data.model.JokeModel
import com.assignment.databinding.ItemViewJokeBinding

class JokeListAdapter(private val jokesList: List<JokeModel>) :
    RecyclerView.Adapter<JokeListAdapter.MainViewHolder>() {

    inner class MainViewHolder(private val binding: ItemViewJokeBinding) : ViewHolder(binding.root) {
        fun bindItem(jokeModel: JokeModel) {
            binding.tvJoke.text = jokeModel.joke
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MainViewHolder(
        ItemViewJokeBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount() = jokesList.size

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bindItem(jokesList[position])
    }
}