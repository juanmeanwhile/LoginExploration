package com.menwhile.loginexploration.domain

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menwhile.loginexploration.data.LoginRepository
import com.menwhile.loginexploration.domain.exception.InvalidEmailException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

private const val SS_FILLED_DATA = "filledData"

/**
 * Holds business logic.
 * Exposes a flow with the ui state containing the step that should be displayed in the UI and which data need to be used.
 * When new data has been filled by the user, internally a call is made to [onDataProvided] which will end up emitting the next step we should navigate to
 *
 * To keep it simple, business logic lives directly in the ViewModel, but would be better moved into UseCases. This will come in future iterations.
 */
class LoginViewModel(
    private val loginRepo: LoginRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * Holds the current status of a login action
     */
    private val _actionState: MutableStateFlow<Outcome<Unit>> = MutableStateFlow(Outcome.Success(Unit))

    /**
     * Holds the data filled by the user, which can be used by the strategy to decide what is the next step.
     */
    private val _filledData = MutableSharedFlow<FilledData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Holds the step the user is currently in. Emitting new step means that we need to navigate to that Step screen
     */
    private val _currentStep = MutableSharedFlow<Step>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * One single flow expose ui state, represented by [Outcome] (success, loading, error) and the [Step] that should be displayed in the UI
     */
    val uiState: Flow<Outcome<Step>> = combine(_actionState, _currentStep) { actionStatus: Outcome<Unit>, step: Step ->
        // Use Outcome from action flow but with the data from step flow. I prefer to expose one single flow to the UI
        actionStatus.map { step }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    /**
     * Help field to get the current Data internally. Since we are using SharedFlow, we cannot access its value directly, so we have this helper method.
     * Another option could be to just to use first() from [_filledData].
     */
    private val currentFilledData: FilledData
        get() = savedInstanceData ?: FilledData(null, null, null, false, null)

    /**
     * Backing property to save data into savedInstance State
     */
    private var savedInstanceData: FilledData?
        set(value) { savedStateHandle[SS_FILLED_DATA] = value}
        get() = savedStateHandle[SS_FILLED_DATA]


    init {
        // Start the emission, this will get data from saved State if there are any, so recreation is handled
        _filledData.tryEmit(currentFilledData)

        viewModelScope.launch {
            _filledData.collectLatest { data ->
                // Store in saved instance to survive recreation
                savedInstanceData = data

                // Decide which step needs to be triggered
                onDataProvided(data)
            }
        }
    }

    /**
     * Notify ViewModel that destination has changed so we can detect back navigation and adjust uiState accordingly
     */
    fun onDestinationChanged(navId: Step.NavId) {
        viewModelScope.launch {
            val currentStep = _currentStep.first()
            // When the user navigates back, our step in uiState flow won't match with the step visible to the user. Thi match is needed in order for the
            // screen being able to get the screen data (due to the filter being done).
            if (currentStep.id != navId) {
                _currentStep.tryEmit(generateStep(navId = navId, currentFilledData))
            }
        }
    }

    /**
     * A Login option was selected by the user
     */
    fun onOptionSelected(option: String) {
        // We assume option was Email just for simplicity
        viewModelScope.launch {
            val updatedData = currentFilledData.copy(flowType = FlowType.EMAIL_PASS)
            _filledData.emit(updatedData)
        }
    }

    /**
     * User has entered it's email.
     * Sample of a method with validation.
     */
    fun onEmailEntered(email: String) {
        viewModelScope.launch {
            // Do validation (business logic)
            if (email.contains('@')){
                val updatedData = currentFilledData.copy(userEmail = email)
                _filledData.emit(updatedData)
                _actionState.emit(Outcome.Success(Unit))
            } else {
                _actionState.emit(Outcome.Error(ex = InvalidEmailException(), data = Unit))
            }
        }
    }

    /**
     * User has entered a password.
     * Sample of a method which interacts with a repository for network call
     */
    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            with (currentFilledData) {
                _actionState.emit(Outcome.Loading(Unit))
                kotlin.runCatching {
                    // JUAN is not great to reach this place and having to !! in some fields
                    // We crash because we shouldn't reach this place and we prefer to crash the app and realise in development
                    val userId = loginRepo.login(userEmail!!, password)

                    _actionState.emit(Outcome.Success(Unit))
                    _filledData.emit(currentFilledData.copy(userPassword = password, userId = userId))
                }.onFailure {
                    _actionState.emit(Outcome.Error(ex = it, data = Unit))
                }
            }
        }
    }

    /**
     * For now we'll use this method to wrap our logic on which Step need to be shown depending on which data the user has already provided.
     * In future iterations we'll explore a better place to put this logic, but for now I want to keep it simple
     */
    private fun onDataProvided(data: FilledData) {
        val nextNavId =  when {
            data.flowType == null -> Step.NavId.OPTIONS
            data.userEmail == null -> Step.NavId.EMAIL
            data.userPassword == null -> Step.NavId.PASSWORD
            else -> {
                if (data.userId != null) {
                    Step.NavId.END
                } else {
                    Step.NavId.PASSWORD
                }
            }
        }

        val nextStep = generateStep(nextNavId, data)
        _currentStep.tryEmit(nextStep)
    }

    private fun generateStep(navId: Step.NavId, filledData: FilledData): Step {
        return when (navId){
            Step.NavId.START -> Step.Start
            Step.NavId.OPTIONS -> Step.LoginOptionsStep(listOf("Google", "Twitter", "Email"))
            Step.NavId.EMAIL -> Step.EnterEmailStep(filledData.userEmail)
            Step.NavId.PASSWORD -> Step.EnterPassword()
            Step.NavId.END -> Step.End
        }
    }
}

