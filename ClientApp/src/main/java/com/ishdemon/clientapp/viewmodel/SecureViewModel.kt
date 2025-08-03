package com.ishdemon.clientapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishdemon.clientapp.ipc.PushClient
import com.ishdemon.clientapp.ipc.SecureClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecureViewModel @Inject constructor(
    private val secureClient: SecureClient,
    private val pushClient: PushClient
) : ViewModel() {

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow

    private val _pushFlow = MutableStateFlow<String?>(null)
    val pushFlow: StateFlow<String?> = _pushFlow

    val isBound: StateFlow<Boolean> get() = secureClient.isBound

    val isInstalled: StateFlow<Boolean?> get() = secureClient.isInstalled

    fun bindServices(context: Context) {
        secureClient.initKeys()
        secureClient.bind(context)
        pushClient.bind(context)

        viewModelScope.launch {
            pushClient.pushMessages.collect { msg ->
                _pushFlow.emit(msg)
            }
        }
    }

    fun unbindServices(context: Context) {
        secureClient.unbind(context)
        pushClient.unbind(context)
    }

    fun sendSecureData(data: String) {
        viewModelScope.launch {
            val token = secureClient.sendData(data)
            if (token != null) {
                _tokenFlow.emit(token)
            } else {
                _tokenFlow.emit("Error: No response or ServerApp not connected")
            }
        }
    }
}
