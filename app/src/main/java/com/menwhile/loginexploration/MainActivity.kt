package com.menwhile.loginexploration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.menwhile.loginexploration.data.LoginRepository
import com.menwhile.loginexploration.domain.LoginViewModel
import com.menwhile.loginexploration.domain.Outcome
import com.menwhile.loginexploration.domain.Step
import com.menwhile.loginexploration.ui.screen.EnterEmailScreen
import com.menwhile.loginexploration.ui.screen.EnterPasswordScreen
import com.menwhile.loginexploration.ui.screen.OptionsOverviewScreen
import com.menwhile.loginexploration.ui.theme.LoginExplorationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels { MainViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginExplorationTheme {
                val navController = rememberNavController()
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    LoginScreen(viewModel, navController) //TODO get viewModel
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(viewModel: LoginViewModel, navController: NavHostController) {

    // We need to get our step flow back on sync with what is visible on screen, this will help
    navController.addOnDestinationChangedListener { controller, destination, _ ->
        destination.route?.let {  navigatedRoute ->
            val navId = Step.NavId.valueOf(navigatedRoute)
            viewModel.onDestinationChanged(navId)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Step.NavId.OPTIONS.name,
        modifier = Modifier.padding(16.dp)
    ) {
        composable(Step.NavId.START.name) {
            Text("Empty")
        }
        composable(Step.NavId.OPTIONS.name) {
            val optionsFlow = viewModel.uiState.filterStep<Step.LoginOptionsStep>().map { it.data.options }
            OptionsOverviewScreen(dataFlow = optionsFlow, onOptionSelected = viewModel::onOptionSelected)
        }
        composable(Step.NavId.EMAIL.name) {
            val enterEmailFlow = viewModel.uiState.filterStep<Step.EnterEmailStep>()
            EnterEmailScreen(enterEmailFlow, onEmailEntered = viewModel::onEmailEntered)
        }
        composable(Step.NavId.PASSWORD.name) {
            val enterPasswordFlow = viewModel.uiState.filterStep<Step.EnterPassword>()
            EnterPasswordScreen(dataFlow = enterPasswordFlow, onPasswordEntered = viewModel::onPasswordEntered)
        }
        composable(Step.NavId.END.name) {
           Text(
               modifier = Modifier.padding(32.dp),
               text = "User created"
           )
        }
    }

    // We want a flow which only cares about the step we should navigate to
    val stepFlow = viewModel.uiState.map {
        it.data
    }

    LoginStep(stepFlow = stepFlow, navController = navController)
}

/**
 * Sugar for this filter that is repeated for every screen.
 * We decided to crash if the cast fails, since it's more likely a problem that will be detected in development and should not happen in prod.
 * Still, having this kind of "trust" behavior is one of the drawbacks of the approach
 */
private inline fun <reified S>Flow<Outcome<Step>>.filterStep(): Flow<Outcome<S>> {
    return filter { it.data is S } as Flow<Outcome<S>>
}

@Composable
fun LoginStep(stepFlow: Flow<Step>, navController: NavHostController) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stepFlowLifecycleAware = remember(stepFlow, lifecycleOwner) {
        stepFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    val stepState by stepFlowLifecycleAware.collectAsState(Step.Start)

    when (val step = stepState) {
        Step.Start -> { /* only here because we need an start destination */}
        is Step.LoginOptionsStep -> navigateToOptions(navController)
        is Step.EnterEmailStep -> navigateToEmailStep(navController)
        is Step.EnterPassword -> navigateToPasswordSep(navController)
        Step.End -> { navigateToEnd(navController) }
    }
}

private fun navigateToOptions(navController: NavHostController) {
    navController.navigate(Step.NavId.OPTIONS.name) {
        launchSingleTop = true
        popUpTo(Step.NavId.START.name) {
            inclusive = true
        }
    }
}

private fun navigateToEmailStep(navController: NavHostController) {
    navController.navigate(Step.NavId.EMAIL.name) {
        launchSingleTop = true
    }
}

private fun navigateToPasswordSep(navController: NavHostController) {
    navController.navigate(Step.NavId.PASSWORD.name) {
        launchSingleTop =  true
    }
}

private fun navigateToEnd(navController: NavHostController) {
    navController.navigate(Step.NavId.END.name){
        // Need to popup to Options, because Start is not in the back stack anymore (was poped up when navigation to Options)
        popUpTo(Step.NavId.OPTIONS.name) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

class MainViewModelFactory(activity: MainActivity) : AbstractSavedStateViewModelFactory(activity, null) {

    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val repo = LoginRepository()
        return LoginViewModel(repo, handle) as T
    }
}
