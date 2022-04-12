package com.menwhile.loginexploration.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(title: String, content: @Composable () -> Unit) {

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.h2
        )
        content.invoke()
    }
}

@Composable
fun ErrorLabel(text: String, modifier: Modifier = Modifier.wrapContentSize()) {
    Text(
        text = text,
        color = Color.Red
    )
}