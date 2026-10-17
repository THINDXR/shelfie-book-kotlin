package com.example.shelfie.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
)

enum class AuthErrorKind {
    NOT_REGISTERED,      // login with an email that has never signed up
    ALREADY_REGISTERED,  // sign up with an email that already exists
    WRONG_PASSWORD,
    INVALID_INPUT,
    NETWORK,
    UNKNOWN,
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val kind: AuthErrorKind, val message: String) : AuthResult
}

interface AuthRepository {
    /** true = Firebase (accounts in the cloud), false = accounts stored on this phone. */
    val isCloud: Boolean
    fun currentUser(): AuthUser?
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    suspend fun updateDisplayName(name: String)
    fun logout()
}

object AuthValidator {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizeEmail(email: String) = email.trim().lowercase()

    fun check(email: String, password: String): AuthResult.Failure? = when {
        !emailRegex.matches(normalizeEmail(email)) ->
            AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รูปแบบอีเมลไม่ถูกต้อง")
        password.length < 6->
            AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
        else -> null
    }

    fun checkRegister(name: String, email: String, password: String, confirm: String): AuthResult.Failure? =
        when {
            name.isBlank() -> AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "กรุณากรอกชื่อที่แสดง")
            else -> check(email, password) ?: if (password != confirm) {
                AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รหัสผ่านทั้งสองช่องไม่ตรงกัน")
            } else null
        }
}

object AuthRepositoryFactory {
    /** Uses Firebase when google-services.json was added to the project, otherwise local accounts. */
    fun create(context: Context): AuthRepository =
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuthRepository()
        else LocalAuthRepository(context)
}
