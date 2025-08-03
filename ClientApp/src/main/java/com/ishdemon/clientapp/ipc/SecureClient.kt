package com.ishdemon.clientapp.ipc

import android.content.*
import android.content.pm.PackageManager
import android.os.*
import com.ishdemon.common.CryptoUtils
import com.ishdemon.ipc.IEncryptService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureClient @Inject constructor(
    private val cryptoUtils: CryptoUtils
) {
    companion object {
        private const val MAX_BINDER_SIZE = 512 * 1024 // 0.5MB safe margin
        private const val SERVER_PACKAGE = "com.ishdemon.serverapp"
        private const val SERVICE_CLASS = "com.ishdemon.serverapp.SecureService"
    }

    private var service: IEncryptService? = null
    private var bound = false
    private var clientKeyPair: KeyPair? = null
    private var contextRef: WeakReference<Context> = WeakReference(null)

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound

    private val _isInstalled = MutableStateFlow<Boolean?>(null)
    val isInstalled: StateFlow<Boolean?> = _isInstalled

    fun initKeys() {
        clientKeyPair = cryptoUtils.generateKeyPair()
    }

    private fun isServerAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SERVER_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IEncryptService.Stub.asInterface(binder)
            bound = true
            _isBound.value = true

            binder?.linkToDeath({
                _isBound.value = false
                bound = false
                service = null
                // Try auto-rebind
                Handler(Looper.getMainLooper()).postDelayed({
                    bind(contextRef.get() ?: return@postDelayed)
                }, 2000)
            }, 0)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
            _isBound.value = false
        }
    }

    fun bind(context: Context) {
        if (!isServerAppInstalled(context)) {
            _isInstalled.value = false
            return
        } else _isInstalled.value = true
        contextRef = WeakReference(context)
        val intent = Intent().apply {
            component = ComponentName(SERVER_PACKAGE, SERVICE_CLASS)
            action = "AIDL_BIND"
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        if (bound) {
            context.unbindService(conn)
            bound = false
            _isBound.value = false
        }
    }

    suspend fun sendData(data: String): String? = withContext(Dispatchers.IO) {
        val serverPubKeyBytes = service?.publicKey ?: return@withContext null
        val serverPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(serverPubKeyBytes))

        val encryptedData = cryptoUtils.encrypt(data.toByteArray(), serverPublicKey)

        // Binder size check
        if (encryptedData.size > MAX_BINDER_SIZE) {
            throw IllegalArgumentException("Payload exceeds Binder size limit")
        }

        val encryptedResponse = service?.processData(
            encryptedData,
            clientKeyPair?.public?.encoded
        ) ?: return@withContext null

        val decryptedResponse = cryptoUtils.decrypt(encryptedResponse, clientKeyPair!!.private)
        return@withContext decryptedResponse.decodeToString()
    }
}
