package com.hadley.receiptbackup.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.*
import com.hadley.receiptbackup.BuildConfig
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object GoogleAuthManager {
    suspend fun signInWithGoogle(activity: Activity): FirebaseUser? {
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.SERVER_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleCredential.idToken)
            } else {
                Log.e("GoogleAuth", "Unexpected credential type")
                null
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "Credential sign-in failed", e)
            null
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result?.user)
                    } else {
                        Log.e("GoogleAuth", "Sign-in failed", task.exception)
                        continuation.resume(null)
                    }
                }
        }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    suspend fun signOut(activity: Activity, viewModel: ReceiptItemViewModel) {
        val credentialManager = CredentialManager.create(activity)
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuth", "Failed to clear credential state", e)
        }
        FirebaseAuth.getInstance().signOut()
        viewModel.clearItems()
    }
}