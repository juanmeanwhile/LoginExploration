package com.menwhile.loginexploration.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.menwhile.loginexploration.ui.theme.LoginExplorationTheme

@Composable
fun OptionsOverviewScreen(options: List<String>, onOptionSelected: (option: String) -> Unit) {
    LoginScreen(title = "Options") {
        options.forEach { option ->
            OutlinedButton(onClick = { onOptionSelected.invoke(option) }) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = option
                )
            }
            Divider()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginOptionsPreview() {
    LoginExplorationTheme {
        OptionsOverviewScreen(listOf("Facebook", "google", "email"), {})
    }
}