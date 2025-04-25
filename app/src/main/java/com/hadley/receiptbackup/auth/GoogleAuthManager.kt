package com.hadley.receiptbackup.auth

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.hadley.receiptbackup.BuildConfig
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel

object GoogleAuthManager {
    fun getSignInClient(activity: Activity): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.SERVER_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, options)
    }

    fun firebaseAuthWithGoogle(idToken: String, onComplete: (FirebaseUser?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(task.result?.user)
                } else {
                    Log.e("GoogleAuth", "Sign-in failed", task.exception)
                    onComplete(null)
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun signOut(activity: Activity, viewModel: ReceiptItemViewModel, onComplete: () -> Unit) {
        getSignInClient(activity).signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
            viewModel.clearItems()
            onComplete()
        }
    }
}