package com.menwhile.loginexploration.data

import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Dummy repo simulating network calls.
 * A proper implementation would use DataSources
 */
class LoginRepository {

    /**
     * Launches login call and returns created userId if success
     * @return userId if the login operation worked, exception if not
     */
    suspend fun login(userEmail: String, password: String): String {
        // Simulate call
        delay(2000)
        return Random.nextInt().toString()
    }
}