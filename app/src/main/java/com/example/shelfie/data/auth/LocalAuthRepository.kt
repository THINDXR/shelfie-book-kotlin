package com.example.shelfie.data.auth

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Accounts kept on this phone (SharedPreferences). Passwords are never stored as-is:
 * we keep a random salt and the SHA-256 hash of salt + password.
 *
 * It really checks whether an email has signed up before:
 * - login with an unknown email  -> NOT_REGISTERED
 * - register with a known email  -> ALREADY_REGISTERED
 *
 * A demo account (demo@shelfie.app / 123456) is created on first launch so you can test both cases.
 */
class LocalAuthRepository(context: Context) : AuthRepository {

    private data class LocalAccount(val name: String, val salt: String, val hash: String)

    private val prefs = context.getSharedPreferences("shelfie_accounts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mapType = object : TypeToken<MutableMap<String, LocalAccount>>() {}.type

    override val isCloud: Boolean = false

    init {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            val accounts = readAccounts()
            accounts[DEMO_EMAIL] = newAccount("มายด์", "123456")
            writeAccounts(accounts)
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }

    override fun currentUser(): AuthUser? {
        val email = prefs.getString(KEY_SESSION, null) ?: return null
        val account = readAccounts()[email] ?: return null
        return AuthUser(uid = "local:$email", email = email, displayName = account.name)
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val e = AuthValidator.normalizeEmail(email)
        val accounts = readAccounts()
        if (accounts.containsKey(e)) {
            return AuthResult.Failure(
                AuthErrorKind.ALREADY_REGISTERED,
                "อีเมลนี้เคยสมัครสมาชิกไว้แล้ว เข้าสู่ระบบด้วยอีเมลนี้ได้เลย"
            )
        }
        accounts[e] = newAccount(name.trim(), password)
        writeAccounts(accounts)
        prefs.edit().putString(KEY_SESSION, e).apply()
        return AuthResult.Success(AuthUser("local:$e", e, name.trim()))
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val e = AuthValidator.normalizeEmail(email)
        val account = readAccounts()[e]
            ?: return AuthResult.Failure(
                AuthErrorKind.NOT_REGISTERED,
                "ไม่พบบัญชีที่ใช้อีเมลนี้ คุณยังไม่เคยสมัครสมาชิก"
            )
        if (hash(account.salt, password) != account.hash) {
            return AuthResult.Failure(AuthErrorKind.WRONG_PASSWORD, "รหัสผ่านไม่ถูกต้อง ลองใหม่อีกครั้ง")
        }
        prefs.edit().putString(KEY_SESSION, e).apply()
        return AuthResult.Success(AuthUser("local:$e", e, account.name))
    }

    override suspend fun updateDisplayName(name: String) {
        val email = prefs.getString(KEY_SESSION, null) ?: return
        val accounts = readAccounts()
        val account = accounts[email] ?: return
        accounts[email] = account.copy(name = name.trim())
        writeAccounts(accounts)
    }

    override fun logout() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    // ---------- helpers ----------

    private fun readAccounts(): MutableMap<String, LocalAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return mutableMapOf()
        return runCatching { gson.fromJson<MutableMap<String, LocalAccount>>(json, mapType) }
            .getOrNull() ?: mutableMapOf()
    }

    private fun writeAccounts(accounts: Map<String, LocalAccount>) {
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).apply()
    }

    private fun newAccount(name: String, password: String): LocalAccount {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = bytes.toHex()
        return LocalAccount(name, salt, hash(salt, password))
    }

    private fun hash(salt: String, password: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    companion object {
        const val DEMO_EMAIL = "demo@shelfie.app"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_SESSION = "session_email"
        private const val KEY_SEEDED = "seeded_demo"
    }
}
