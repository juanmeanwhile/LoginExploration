package com.menwhile.loginexploration.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.menwhile.loginexploration.ui.theme.LoginExplorationTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun OptionsOverviewScreen(dataFlow: Flow<List<String>>, onOptionSelected: (option: String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val dataFlowLifecycleAware = remember(dataFlow, lifecycleOwner) {
        dataFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    val uiState by dataFlowLifecycleAware.collectAsState(listOf())
    OptionsOverviewScreenContent(options = uiState, onOptionSelected = onOptionSelected)
}

@Composable
fun OptionsOverviewScreenContent(options: List<String>, onOptionSelected: (option: String) -> Unit) {

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
        OptionsOverviewScreenContent(listOf("Facebook", "google", "email"), {})
    }
}