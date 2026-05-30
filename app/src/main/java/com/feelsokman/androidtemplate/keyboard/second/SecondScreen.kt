package com.feelsokman.androidtemplate.keyboard.second

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.feelsokman.androidtemplate.keyboard.customViewModel
import timber.log.Timber

@Composable
fun SecondScreen(
    goBack: () -> Unit
) {
    val secondViewModel = customViewModel<SecondViewModel>()

    Box(
        modifier = Modifier
            .height(370.dp)
            .background(Color.LightGray)
            .navigationBarsPadding() // Avoids bottom insets if that's what you mean
            .padding(16.dp), // Optional padding for content
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // more space around elements
        ) {
            Text(
                text = "Welcome to the Second Screen!",
                color = Color.DarkGray // nicer text color
            )
            Button(
                onClick = goBack
            ) {
                Text(text = "go back")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.tag("KeyboardComposeView").d("SecondScreen left composition")
        }
    }
}
