package com.example.ubicasure.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Obtiene el ID Token del usuario autenticado en Firebase cuando una petición
 * protegida lo necesita. El token no se persiste localmente: Firebase se encarga
 * de mantenerlo y renovarlo cuando corresponde.
 */
object FirebaseTokenProvider {

    suspend fun getIdToken(forceRefresh: Boolean = false): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("No hay un usuario autenticado en Firebase.")

        return suspendCancellableCoroutine { continuation ->
            user.getIdToken(forceRefresh)
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) {
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {
                        val token = task.result?.token

                        if (token.isNullOrBlank()) {
                            continuation.resumeWithException(
                                IllegalStateException("Firebase no devolvió un ID Token válido.")
                            )
                        } else {
                            continuation.resume(token)
                        }
                    } else {
                        continuation.resumeWithException(
                            task.exception
                                ?: IllegalStateException("No se pudo obtener el ID Token de Firebase.")
                        )
                    }
                }
        }
    }

    suspend fun getAuthorizationHeader(forceRefresh: Boolean = false): String {
        return "Bearer ${getIdToken(forceRefresh)}"
    }
}
