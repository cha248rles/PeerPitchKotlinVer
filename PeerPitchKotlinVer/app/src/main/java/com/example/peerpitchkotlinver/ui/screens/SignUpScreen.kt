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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.peerpitchkotlinver.ui.components.LoginLinkRow
import com.example.peerpitchkotlinver.ui.components.NextButton
import com.example.peerpitchkotlinver.ui.components.PeerPitchLogo
import com.example.peerpitchkotlinver.ui.components.PitchTextField
import com.example.peerpitchkotlinver.ui.components.PitchTopBar
import com.example.peerpitchkotlinver.ui.components.PresentationIllustration
import com.example.peerpitchkotlinver.ui.theme.PitchGold

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onLoginInstead: () -> Unit,
    onNext: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchGold)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PitchTopBar(title = "SignUp", onBack = onBack)
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
                value = username,
                onValueChange = { username = it },
                placeholder = "Username"
            )
            Spacer(modifier = Modifier.height(16.dp))
            PitchTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LoginLinkRow(
                prefix = "Have an Account? ",
                link = "Login",
                onLinkClick = onLoginInstead
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButton(onClick = onNext, modifier = Modifier.align(Alignment.End))
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(onBack = {}, onLoginInstead = {}, onNext = {})
}
