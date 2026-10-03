package com.alokrathava.sdk.security

interface CredentialStore {
    fun saveToken(robotId: String, token: String)
    fun getToken(robotId: String): String?
    fun clearToken(robotId: String)
}

class InMemoryCredentialStore : CredentialStore {
    private val tokens = mutableMapOf<String, String>()

    override fun saveToken(robotId: String, token: String) {
        tokens[robotId] = token
    }

    override fun getToken(robotId: String): String? {
        return tokens[robotId]
    }

    override fun clearToken(robotId: String) {
        tokens.remove(robotId)
    }
}
