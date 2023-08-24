package com.assignment.presentation.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.assignment.data.model.JokeModel
import com.assignment.databinding.FragmentJokeListBinding
import com.assignment.presentation.viewmodel.JokesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class JokeListFragment : Fragment() {
    private lateinit var binding: FragmentJokeListBinding
    private lateinit var adapter: JokeListAdapter
    private var jokeList = ArrayList<JokeModel>()
    private val viewModel: JokesViewModel by viewModel()
    private var timer: Timer? = null

    companion object{
        const val TIMES: Long = 60
        const val SECOND: Long = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentJokeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        initObservers()
    }

    override fun onStart() {
        super.onStart()
        initialiseTimer()
    }

    private fun initialiseTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                getNewJoke()
            }
        }, 0, TIMES  * SECOND)
    }

    private fun getNewJoke() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            showHideJokeLoader(true)
            delay(SECOND/2)
            viewModel.getRemoteJoke()
        }
    }

    private fun showHideJokeLoader(isShow: Boolean) {
        binding.lottieAnim.visibility = let {
            if (isShow){
                binding.lottieAnim.playAnimation()
                return@let View.VISIBLE
            }else{
                binding.lottieAnim.cancelAnimation()
                return@let View.INVISIBLE
            }
        }
    }

    private fun initAdapter() {
        adapter = JokeListAdapter(jokeList)
        binding.rvJokes.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.getJokesFromDB().observe(viewLifecycleOwner) {
                showHideJokeLoader(false)
                if(it==null){
                    return@observe
                }
                jokeList.clear()
                jokeList.addAll(it.map { jokesEntity ->
                    jokesEntity.toDataModel()
                }.reversed())
                binding.rvJokes.scrollToPosition(jokeList.size - 1)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onStop() {
        super.onStop()
        timer = null
    }
}