package com.ishdemon.clientapp.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushClient @Inject constructor() {
    private var bound = false
    private val _pushMessages = MutableSharedFlow<String>()
    val pushMessages: SharedFlow<String> = _pushMessages

    private val incomingMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 2) {
                val text = msg.data.getString("message") ?: ""
                _pushMessages.tryEmit(text)
            }
        }
    })

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val serviceMessenger = Messenger(binder)
            val msg = Message.obtain(null, 1).apply { replyTo = incomingMessenger }
            serviceMessenger.send(msg)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    fun bind(context: Context) {
        val intent = Intent().apply {
            component =
                ComponentName("com.ishdemon.serverapp", "com.ishdemon.serverapp.SecureService")
            action = "MESSENGER_BIND"
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        if (bound) {
            context.unbindService(conn)
            bound = false
        }
    }
}