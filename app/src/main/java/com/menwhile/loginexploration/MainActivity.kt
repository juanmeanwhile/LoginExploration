package com.menwhile.loginexploration

import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.menwhile.loginexploration.data.LoginRepository
import com.menwhile.loginexploration.domain.LoginViewModel
import com.menwhile.loginexploration.domain.Outcome
import com.menwhile.loginexploration.domain.Step
import com.menwhile.loginexploration.domain.strategy.BaseStrategy
import com.menwhile.loginexploration.ui.screen.EnterEmailScreen
import com.menwhile.loginexploration.ui.screen.EnterPasswordScreen
import com.menwhile.loginexploration.ui.screen.OptionsOverviewScreen
import com.menwhile.loginexploration.ui.theme.LoginExplorationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels { MainViewModelFactory() }

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
    Log.d("LoginScreen", "recomposed")

    // TODO make end pop up the rest of the steps ( o it's the last one
    // TODO remove Empty state
    // TODO move to separated Fragment

    NavHost(
        navController = navController,
        startDestination = Step.NavId.EMPTY.name,
        modifier = Modifier.padding(16.dp)
    ) {
        composable(Step.NavId.EMPTY.name) {
            Text("Empty")
        }
        composable(Step.NavId.EMAIL.name) {
            val enterEmailFlow = viewModel.uiState.filter { it.data is Step.EnterEmailStep } as Flow<Outcome<Step.EnterEmailStep>> //TODO move to VM method
            EnterEmailScreen(enterEmailFlow, onEmailEntered = viewModel::onEmailEntered)
        }
        composable(Step.NavId.PASSWORD.name) {
            val enterPasswordFlow = viewModel.uiState.filter { it.data is Step.EnterPassword } as Flow<Outcome<Step.EnterPassword>> //TODO move to VM method
            EnterPasswordScreen(dataFlow = enterPasswordFlow, onPasswordEntered = viewModel::onPasswordEntered)
        }
        composable(
            route = "${Step.NavId.OPTIONS.name}/{options}",
            arguments = listOf(
                navArgument("options") {
                    // Make argument type safe
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val options = entry.arguments?.getString("options")!!.split(",").toList()
            OptionsOverviewScreen(options = options, onOptionSelected = viewModel::onOptionSelected)
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

@Composable
fun LoginStep(stepFlow: Flow<Step>, navController: NavHostController) {
    Log.d("LoginStep", "Recomposed")
    val lifecycleOwner = LocalLifecycleOwner.current
    val stepFlowLifecycleAware = remember(stepFlow, lifecycleOwner) {
        stepFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    val stepState by stepFlowLifecycleAware.collectAsState(Step.Empty)

    when (val step = stepState) {
        is Step.ConfirmTermsStep -> TODO()
        is Step.EnterEmailStep -> navigateToEmailStep(navController)
        is Step.EnterPassword -> navigateToPasswordSep(navController)
        is Step.LoginOptionsStep -> navigateToOptions(navController, step.options)
        is Step.VerifyMinAge -> TODO()
        Step.Empty -> { }
        Step.End -> { navigateToEnd(navController) }
    }
}

private fun navigateToPasswordSep(navController: NavHostController) {
    navController.navigate(Step.NavId.PASSWORD.name)
}

private fun navigateToEmailStep(navController: NavHostController) {
    navController.navigate("${Step.NavId.EMAIL}", )
}

private fun navigateToOptions(navController: NavHostController, loginOptions: List<String>) {
    val param = loginOptions.toTypedArray()
    val stParam = param.joinToString(separator = ",")
    navController.navigate("${Step.NavId.OPTIONS}/$stParam", )
}

private fun navigateToEnd(navController: NavHostController) {
    navController.navigate("${Step.NavId.END.name}", )
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val strategy = BaseStrategy()
        val repo = LoginRepository()
        return LoginViewModel(strategy, repo) as T
    }
}
