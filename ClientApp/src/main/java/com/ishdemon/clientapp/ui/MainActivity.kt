package com.ishdemon.clientapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.ishdemon.clientapp.viewmodel.SecureViewModel
import com.ishdemon.clientapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SecureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launchWhenStarted {
            viewModel.tokenFlow.collectLatest { token ->
                token?.let { binding.tvRecieved.text = it }
            }
        }

        lifecycleScope.launch {
            viewModel.isBound.collectLatest { bound ->
                if(bound)
                    Toast.makeText(this@MainActivity,"ServerApp connected", Toast.LENGTH_SHORT).show()
                binding.btnSendSecureData.isEnabled = bound
            }
        }
        lifecycleScope.launch {
            viewModel.isInstalled.collectLatest { isInstalled ->
                if(isInstalled == false){
                    Toast.makeText(this@MainActivity,"ServerApp is not installed. Please Install the server app first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.pushFlow.collectLatest { push ->
                push?.let { binding.tvRecieved.append("\nPush: $it") }
            }
        }

        binding.btnSendSecureData.setOnClickListener {
            binding.tvSent.text = binding.editText.text.toString()
            viewModel.sendSecureData(binding.editText.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindServices(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindServices(this)
    }
}