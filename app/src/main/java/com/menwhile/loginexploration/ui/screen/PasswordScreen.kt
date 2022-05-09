package com.menwhile.loginexploration.ui.screen

import android.service.autofill.OnClickAction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
fun EnterPasswordScreen(dataFlow: Flow<Outcome<Step.EnterPassword>>, onPasswordEntered: (email: String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val dataFlowLifecycleAware = remember(dataFlow, lifecycleOwner) {
        dataFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    val uiState by dataFlowLifecycleAware.collectAsState(Outcome.Success(Step.EnterPassword()))
    EnterPasswordScreenContent(uiState = uiState, onPasswordEntered = onPasswordEntered)

}

@Composable
fun EnterPasswordScreenContent(uiState: Outcome<Step.EnterPassword>, onPasswordEntered: (email: String) -> Unit) {
    LoginScreen(title = "Enter Password") {
        Column {
            var text by rememberSaveable { mutableStateOf("") }
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Password") }
            )

            if (uiState is Outcome.Error){
                ErrorLabel(uiState.ex.toString())
            }
            ActionButton(modifier = Modifier.padding(vertical = 32.dp),
                isLoading = uiState is Outcome.Loading,
                onClickAction = {onPasswordEntered.invoke(text)})
        }
    }
}

@Composable
fun ActionButton(modifier: Modifier, isLoading: Boolean, onClickAction: () -> Unit){
    Button(modifier = Modifier.padding(vertical = 32.dp),
        onClick = onClickAction) {
        Text(text = if (isLoading) "Loading..." else "Continue")
    }

}