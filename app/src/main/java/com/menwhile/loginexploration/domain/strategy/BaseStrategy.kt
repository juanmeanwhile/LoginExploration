package com.menwhile.loginexploration.domain.strategy

import android.util.Log
import com.menwhile.loginexploration.domain.FilledData
import com.menwhile.loginexploration.domain.Step
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Responsible from deciding the next step based on the current data passed.
 * It doesn't contain state
 * IMPORTANT: never store data in this class
 */
class BaseStrategy {

    private val reducers = listOf(
        ::processLoginOptions,
        ::processUserEmail,
        ::processPassword,
        ::processEnd
    )

    // TODO this would be abstract probably, so each strategy can provide it's empty FilledData
    /**
     * Provides the initial data when starting the login process
     */
    fun getInitialFilledData() = FilledData(null, null, null, false, null)

    // StateFlow doesn't work due to distinctUntilChanged (since its possible to navigate back, might need to emit the same), so we use SharedFlow
    private val _currentState = MutableSharedFlow<Step>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val currentStep: Flow<Step> = _currentState

    suspend fun onDataProvided(filledData: FilledData) {
        Log.d("Strategy", "onDataProvided() $filledData")
        for (stepReducer in reducers) {
            val nextStep = stepReducer.invoke(filledData)
            if (nextStep != null) {
                _currentState.emit(nextStep)
                break
            }
        }
    }

    private fun processLoginOptions(data: FilledData): Step? {
        return Step.LoginOptionsStep(
            options = listOf("facebook", "Google", "Email-password")
        ).takeIf { data.flowType == null }
    }

    private fun processUserEmail(data: FilledData): Step? {
        return Step.EnterEmailStep(data.userEmail).takeIf {
            data.userEmail == null
        }
    }

    private fun processPassword(data: FilledData): Step? {
        return Step.EnterPassword().takeIf {
            data.userPassword == null
        }
    }

    private fun processEnd(data: FilledData): Step? {
        return Step.End.takeIf {
            data.userId != null
        }
    }
}