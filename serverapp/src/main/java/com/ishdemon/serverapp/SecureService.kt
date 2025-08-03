package com.ishdemon.serverapp

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.ishdemon.common.CryptoUtils
import com.ishdemon.ipc.IEncryptService
import dagger.hilt.android.AndroidEntryPoint
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

@AndroidEntryPoint
class SecureService : Service() {

    companion object {
        private const val TAG = "SecureService"
        private const val MAX_BINDER_SIZE = 512 * 1024 // ~0.5MB safe margin
    }

    @Inject
    lateinit var cryptoUtils: CryptoUtils

    private lateinit var keyPair: KeyPair
    private var clientMessenger: Messenger? = null

    override fun onCreate() {
        super.onCreate()
        keyPair = cryptoUtils.generateKeyPair()
        Log.d(TAG, "Server key pair generated")
    }

    private val aidlBinder = object : IEncryptService.Stub() {
        override fun getPublicKey(): ByteArray = cryptoUtils.getPublicKey()

        override fun processData(encryptedData: ByteArray, clientPublicKeyBytes: ByteArray): ByteArray {
            if (encryptedData.size > MAX_BINDER_SIZE) {
                pushMessageToClient("Payload exceeds Binder size limit")
                throw IllegalArgumentException("Payload exceeds Binder size limit")
            }

            val decrypted = cryptoUtils.decrypt(encryptedData, keyPair.private)
            val processed = "*#FROM_SERVER_" + decrypted.decodeToString()

            val clientPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(clientPublicKeyBytes))
            pushMessageToClient("Encrypting..")

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
        try {
            val bundle = Bundle().apply { putString("message", text) }
            val msg = Message.obtain(null, 2).apply { data = bundle }
            clientMessenger?.send(msg)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to push message to client", e)
        }
    }
}
