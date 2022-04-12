package com.menwhile.loginexploration.data

import kotlin.random.Random
import kotlinx.coroutines.delay

class LoginRepository {

    /**
     * Launches login call and returns created userId if success
     */
    suspend fun login(userEmail: String, password: String): String {
        // Simulate call
        delay(2000)
        return Random.nextInt().toString()
    }
}