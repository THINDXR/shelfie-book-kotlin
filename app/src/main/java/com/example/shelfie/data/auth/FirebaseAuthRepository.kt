package com.example.shelfie.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real accounts with Firebase Authentication (Email/Password).
 *
 * Firebase itself knows whether an email has signed up:
 * - createUser on an existing email throws FirebaseAuthUserCollisionException
 * - signIn on an unknown email throws FirebaseAuthInvalidUserException
 *   (only when "Email enumeration protection" is OFF in the Firebase console;
 *   when it is ON, Firebase answers "invalid credentials" for both cases on purpose)
 */
class FirebaseAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    override val isCloud: Boolean = true

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun register(name: String, email: String, password: String): AuthResult = try {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
        val user = result.user ?: error("no user")
        user.updateProfile(nameChange(name)).awaitTask()
        AuthResult.Success(AuthUser(user.uid, user.email ?: email.trim(), name.trim()))
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthResult.Failure(AuthErrorKind.ALREADY_REGISTERED, "อีเมลนี้เคยสมัครสมาชิกไว้แล้ว")
    } catch (e: FirebaseAuthWeakPasswordException) {
        AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รหัสผ่านง่ายเกินไป ลองใช้รหัสที่ยาวขึ้น")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รูปแบบอีเมลไม่ถูกต้อง")
    } catch (e: FirebaseNetworkException) {
        AuthResult.Failure(AuthErrorKind.NETWORK, "เชื่อมต่ออินเทอร์เน็ตไม่ได้ ลองใหม่อีกครั้ง")
    } catch (e: Exception) {
        AuthResult.Failure(AuthErrorKind.UNKNOWN, "สมัครสมาชิกไม่สำเร็จ: ${e.localizedMessage}")
    }

    override suspend fun login(email: String, password: String): AuthResult = try {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
        val user = result.user ?: error("no user")
        AuthResult.Success(user.toAuthUser())
    } catch (e: FirebaseAuthInvalidUserException) {
        AuthResult.Failure(AuthErrorKind.NOT_REGISTERED, "ไม่พบบัญชีที่ใช้อีเมลนี้ คุณยังไม่เคยสมัครสมาชิก")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthResult.Failure(
            AuthErrorKind.WRONG_PASSWORD,
            "อีเมลหรือรหัสผ่านไม่ถูกต้อง หากยังไม่เคยสมัคร กรุณาสมัครสมาชิกก่อน"
        )
    } catch (e: FirebaseNetworkException) {
        AuthResult.Failure(AuthErrorKind.NETWORK, "เชื่อมต่ออินเทอร์เน็ตไม่ได้ ลองใหม่อีกครั้ง")
    } catch (e: Exception) {
        AuthResult.Failure(AuthErrorKind.UNKNOWN, "เข้าสู่ระบบไม่สำเร็จ: ${e.localizedMessage}")
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult = try {
        auth.sendPasswordResetEmail(email.trim()).awaitTask()
        AuthResult.Success(AuthUser("", email.trim(), ""))
    } catch (e: FirebaseAuthInvalidUserException) {
        AuthResult.Failure(AuthErrorKind.NOT_REGISTERED, "ไม่พบบัญชีที่ใช้อีเมลนี้ในระบบ")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthResult.Failure(AuthErrorKind.INVALID_INPUT, "รูปแบบอีเมลไม่ถูกต้อง")
    } catch (e: FirebaseNetworkException) {
        AuthResult.Failure(AuthErrorKind.NETWORK, "เชื่อมต่ออินเทอร์เน็ตไม่ได้ ลองใหม่อีกครั้ง")
    } catch (e: Exception) {
        AuthResult.Failure(AuthErrorKind.UNKNOWN, "ส่งอีเมลไม่สำเร็จ: ${e.localizedMessage}")
    }

    override suspend fun updateDisplayName(name: String) {
        runCatching {
            auth.currentUser?.updateProfile(nameChange(name))?.awaitTask()
        }
    }

    override fun logout() = auth.signOut()

    private fun nameChange(name: String): UserProfileChangeRequest =
        UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName?.takeIf { it.isNotBlank() } ?: email.orEmpty().substringBefore('@'),
    )
}

/** Turns a Firebase Task into a suspend call (same idea as addOnSuccessListener / addOnFailureListener). */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
