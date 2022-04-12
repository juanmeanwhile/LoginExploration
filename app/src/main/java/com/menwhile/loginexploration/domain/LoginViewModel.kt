package com.menwhile.loginexploration.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menwhile.loginexploration.data.LoginRepository
import com.menwhile.loginexploration.domain.exception.InvalidEmailException
import com.menwhile.loginexploration.domain.strategy.BaseStrategy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val strategy: BaseStrategy,
    private val loginRepo: LoginRepository
) : ViewModel() {

    private val currentFilledData: FilledData
        get() = savedInstanceData ?: strategy.getInitialFilledData()

    // TODO add saved state here
    // this represent the saved instance
    private var savedInstanceData: FilledData? = null

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
    // TODO consider storing the data into a new abstraction owned by the strategy or something which avoids anyone calling _filledData and strategy.oNDataProvided manually

    val uiState: Flow<Outcome<Step>> = combine(_actionState, strategy.currentStep) { actionStatus: Outcome<Unit>, step: Step ->
        Log.d("LoginViewModel", "combine status: ${actionStatus::class.simpleName} - ${step.id.name}")
        actionStatus.map { step }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    init {
        _filledData.tryEmit(strategy.getInitialFilledData())

        // strategy should never called manually, instead just post on filledData is
        viewModelScope.launch {

            _filledData.collectLatest { data ->
                Log.d("LoginViewModel", "_filledData collecting $data")
                //TODO save into Saved State
                savedInstanceData = data

                strategy.onDataProvided(data)
            }
        }
    }

    fun onOptionSelected(option: String) {
        //TODO we assume option was Email just for simplicity
        viewModelScope.launch {
            val updatedData = currentFilledData.copy(flowType = FlowType.EMAIL_PASS)
            _filledData.emit(updatedData)
        }
    }

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
}

