package com.menwhile.loginexploration.domain


// Should not be data class, because we want to be able to emit the same values and still produce recomposition
// TODO possible this one can be a data class, because we are using the step only for the navigation
sealed class Outcome<T>(val data: T) {
    class Loading<T>(data: T) : Outcome<T>(data)
    class Success<T>(data: T) : Outcome<T>(data)
    class Error<T>(data: T, val ex: Throwable) : Outcome<T>(data)
}
// TODO maybe this needs to be a data class again

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
    object Empty : Step(NavId.EMPTY)
    class LoginOptionsStep(val options: List<String>) : Step(NavId.OPTIONS)
    class EnterEmailStep(val enteredEmail: String?) : Step(NavId.EMAIL)
    class EnterPassword : Step(NavId.PASSWORD)
    data class VerifyMinAge(val minAge: Int) : Step(NavId.VERIFY_AGE)
    data class ConfirmTermsStep(val termsUrl: String) : Step(NavId.TERMS)
    object End : Step(NavId.END)

    enum class NavId {
        EMPTY, OPTIONS, EMAIL, PASSWORD, VERIFY_AGE, TERMS, END
    }
}
