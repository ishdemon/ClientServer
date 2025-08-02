package com.ishdemon.serverapp

import android.app.Service
import android.content.Intent
import android.os.*
import com.ishdemon.common.CryptoUtils
import com.ishdemon.ipc.IEncryptService
import dagger.hilt.android.AndroidEntryPoint
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

@AndroidEntryPoint
class SecureService : Service() {

    @Inject
    lateinit var cryptoUtils: CryptoUtils

    private lateinit var keyPair: KeyPair
    private var clientMessenger: Messenger? = null

    override fun onCreate() {
        super.onCreate()
        keyPair = cryptoUtils.generateKeyPair()
    }

    private val aidlBinder = object : IEncryptService.Stub() {
        override fun getPublicKey(): ByteArray = cryptoUtils.getPublicKey()

        override fun processData(encryptedData: ByteArray, clientPublicKeyBytes: ByteArray): ByteArray {
            val decrypted = cryptoUtils.decrypt(encryptedData, keyPair.private)
            val processed = "*#FROM_SERVER_" + decrypted.decodeToString()
            val clientPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(clientPublicKeyBytes))
            return cryptoUtils.encrypt(processed.toByteArray(), clientPublicKey)
        }

    }

    private val messengerHandler = Handler(Looper.getMainLooper()) { msg ->
        if (msg.what == 1) clientMessenger = msg.replyTo
        true
    }
    private val messenger = Messenger(messengerHandler)

    override fun onBind(intent: Intent?): IBinder? {
        return when (intent?.action) {
            "AIDL_BIND" -> aidlBinder
            "MESSENGER_BIND" -> messenger.binder
            else -> null
        }
    }

    private fun pushMessageToClient(text: String) {
        val bundle = Bundle().apply { putString("message", text) }
        val msg = Message.obtain(null, 2).apply { data = bundle }
        clientMessenger?.send(msg)
    }
}
