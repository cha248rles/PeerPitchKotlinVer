/*
 * What: Thin wrapper around Firebase email/password Authentication, exposing sign-in,
 *       sign-up, sign-out and the current user's signed-in state.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Thin wrapper around Firebase Authentication (email/password).
 *
 * Requires a Firebase project: the Google Services plugin reads `app/google-services.json`
 * at build time and Firebase initializes the default app automatically on launch.
 * Enable "Email/Password" under Authentication → Sign-in method in the Firebase console.
 */
object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /** Email of the currently signed-in user, or null if signed out. */
    val currentUserEmail: String? get() = auth.currentUser?.email

    /** True if a user is already signed in (useful to skip the login screen). */
    val isSignedIn: Boolean get() = auth.currentUser != null

    /** Sign in an existing user with email/password; reports success or failure via [onResult]. */
    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Sign-in failed")))
                }
            }
    }

    /** Create a new account with email/password; reports success or failure via [onResult]. */
    fun signUp(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Sign-up failed")))
                }
            }
    }

    /** Sign the current user out. */
    fun signOut() = auth.signOut()
}
