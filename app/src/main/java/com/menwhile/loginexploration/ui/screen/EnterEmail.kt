package com.menwhile.loginexploration.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.menwhile.loginexploration.ui.theme.LoginExplorationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.menwhile.loginexploration.domain.Outcome
import com.menwhile.loginexploration.domain.Step
import kotlinx.coroutines.flow.Flow

@Composable
fun EnterEmailScreen(dataFlow: Flow<Outcome<Step.EnterEmailStep>>, onEmailEntered: (email: String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val dataFlowLifecycleAware = remember(dataFlow, lifecycleOwner) {
        dataFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    val uiState by dataFlowLifecycleAware.collectAsState(Outcome.Success(Step.EnterEmailStep("")))
    EnterEmailScreenContent(uiState = uiState, onEmailEntered = onEmailEntered)
}

@Composable
fun EnterEmailScreenContent(uiState: Outcome<Step.EnterEmailStep>, onEmailEntered: (email: String) -> Unit) {

    var text by rememberSaveable {
      mutableStateOf(uiState.data.enteredEmail.orEmpty())
    }

    LoginScreen(title = "Enter Email") {
        Column {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = { Text("Email:") }
            )
            when (val o = uiState){
                is Outcome.Error -> {
                    ErrorLabel(o.ex.toString())
                }
                is Outcome.Loading -> {
                    Text(text = "loading...")
                }
                is Outcome.Success -> { /* display nothing */ }
            }
            Button(modifier = Modifier.padding(vertical = 32.dp),
                onClick = { onEmailEntered.invoke(text) }) {
                Text(text = "Continue")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EnterEmailPreview() {
    LoginExplorationTheme {
        EnterEmailScreenContent(Outcome.Success(Step.EnterEmailStep(null))) {}
    }
}