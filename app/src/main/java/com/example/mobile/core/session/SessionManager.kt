package com.example.mobile.core.session

import android.content.Context
import com.example.mobile.core.datastore.SessionDataStore
import com.example.mobile.core.model.AuthSession
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SessionManager {

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session = _session.asStateFlow()

    private val mutex = Mutex()
    private var storage: SessionDataStore? = null

    suspend fun restore(context: Context) {
        mutex.withLock {
            val currentStorage = storage ?: SessionDataStore(
                context.applicationContext
            ).also { storage = it }

            _session.value = try {
                currentStorage.read()
            } catch (_: IOException) {
                null
            }
        }
    }

    suspend fun update(session: AuthSession) {
        mutex.withLock {
            val currentStorage = checkNotNull(storage) {
                "Call SessionManager.restore before saving a session"
            }
            currentStorage.save(session)
            _session.value = session
        }
    }

    suspend fun clear() {
        mutex.withLock {
            storage?.clear()
            _session.value = null
        }
    }
}
