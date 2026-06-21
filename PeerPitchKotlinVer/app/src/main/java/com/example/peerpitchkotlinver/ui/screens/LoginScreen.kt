/*
 * What: Login screen for returning users — collects email/password and signs in via AuthRepository.
 * Who:  Charles O'Connell and Anish Machiraju
 * When: 2026-06-21
 */
package com.example.peerpitchkotlinver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerpitchkotlinver.auth.AuthRepository
import com.example.peerpitchkotlinver.ui.components.NextButton
import com.example.peerpitchkotlinver.ui.components.PeerPitchLogo
import com.example.peerpitchkotlinver.ui.components.PitchTextField
import com.example.peerpitchkotlinver.ui.components.PitchTopBar
import com.example.peerpitchkotlinver.ui.components.PresentationIllustration
import com.example.peerpitchkotlinver.ui.theme.PitchGold

/**
 * Renders the returning-user login screen: a gold-backed form with the PeerPitch logo,
 * presentation illustration, email/password fields, inline error text, and a Next button
 * that triggers sign-in.
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchGold)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PitchTopBar(title = "Returning User\nLogin", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            PeerPitchLogo()
            Spacer(modifier = Modifier.height(24.dp))
            PresentationIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            PitchTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email"
            )
            Spacer(modifier = Modifier.height(16.dp))
            PitchTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error!!,
                    color = Color(0xFFB00020),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            NextButton(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please enter your email and password."
                        return@NextButton
                    }
                    error = null
                    loading = true
                    AuthRepository.signIn(email, password) { result ->
                        loading = false
                        result.fold(
                            onSuccess = { onNext() },
                            onFailure = { error = it.message ?: "Login failed." }
                        )
                    }
                },
                enabled = !loading,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Design-time preview of the login screen with no-op navigation callbacks. */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onBack = {}, onNext = {})
}
