package com.menwhile.loginexploration.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data which has been already gathered from the user.
 * This login flow is considered a process of gathering user information, so this represent how far we are in the login process.
 */
@Parcelize
data class FilledData(
    val flowType: FlowType?,
    val userEmail: String?,
    val userPassword: String?,
    val termsConfirmed: Boolean = false,
    val userId: String?
) : Parcelable

/**
 * Kind of flow selected by the user
 */
enum class FlowType {
    SOCIAL, EMAIL_PASS
}

sealed class Outcome<T>(open val data: T) {
    data class Loading<T>(override val data: T) : Outcome<T>(data)
    data class Success<T>(override val data: T) : Outcome<T>(data)
    data class Error<T>(override val data: T, val ex: Throwable) : Outcome<T>(data)
}

fun <T1, T2>Outcome<T1>.map(block: (data: T1) -> T2): Outcome<T2> {
    return when (this){
        is Outcome.Error -> Outcome.Error(block.invoke(data), ex)
        is Outcome.Loading -> Outcome.Loading(block.invoke(data))
        is Outcome.Success -> Outcome.Success(block.invoke(data))
    }
}

// Should be class, not object or data class. This is because we need recomposition to happen everytime an event is emitted.
// Recompositions is emitted if input is the same (uses equals()), and having a new instance (class) the equals() will return false.
// This is one of the tricks of this approach
// JUAN This is one trick and downside
sealed class Step(val id: NavId) {
    object Start : Step(NavId.START)
    class LoginOptionsStep(val options: List<String>) : Step(NavId.OPTIONS)
    class EnterEmailStep(val enteredEmail: String?) : Step(NavId.EMAIL)
    class EnterPassword : Step(NavId.PASSWORD)
    object End : Step(NavId.END)

    enum class NavId {
        START, OPTIONS, EMAIL, PASSWORD, END
    }
}
