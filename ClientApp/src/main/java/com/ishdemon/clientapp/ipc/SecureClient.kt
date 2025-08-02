package com.ishdemon.clientapp.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ishdemon.common.CryptoUtils
import com.ishdemon.ipc.IEncryptService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureClient @Inject constructor(
    private val cryptoUtils: CryptoUtils
) {
    private var service: IEncryptService? = null
    private var bound = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IEncryptService.Stub.asInterface(binder)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
        }
    }

    fun bind(context: Context) {
        val intent = Intent().apply {
            component =
                ComponentName("com.ishdemon.serverapp", "com.ishdemon.serverapp.SecureService")
            action = "AIDL_BIND"
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        if (bound) {
            context.unbindService(conn)
            bound = false
        }
    }

    private lateinit var clientKeyPair: KeyPair

    fun initKeys() {
        clientKeyPair = cryptoUtils.generateKeyPair()
    }

    suspend fun sendData(data: String): String? = withContext(Dispatchers.IO) {
        val serverPubKeyBytes = service?.publicKey ?: return@withContext null
        val serverPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(serverPubKeyBytes))
        val encryptedData = cryptoUtils.encrypt(data.toByteArray(), serverPublicKey)
        val encryptedResponse = service?.processData(encryptedData, clientKeyPair.public.encoded)
            ?: return@withContext null
        val decryptedResponse = cryptoUtils.decrypt(encryptedResponse, clientKeyPair.private)
        return@withContext decryptedResponse.decodeToString()
    }

}