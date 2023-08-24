package com.assignment.presentation.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.assignment.databinding.ActivityContainerBinding

class FragmentContainerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContainerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}