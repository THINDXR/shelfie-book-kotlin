package com.example.shelfie.data.local

import android.content.Context
import com.example.shelfie.data.model.AppSettings
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.LibraryEntry
import com.example.shelfie.data.model.Profile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Saves each user's cart, shelf and profile on the phone as JSON (SharedPreferences + Gson).
 * Data is kept per user id, so two accounts on the same phone do not mix.
 *
 * Want it in the cloud later? Write the same objects to Firestore under users/{uid}.
 */
class UserDataStore(context: Context) {

    private val prefs = context.getSharedPreferences("shelfie_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val cartType = object : TypeToken<List<Book>>() {}.type
    private val libraryType = object : TypeToken<Map<String, LibraryEntry>>() {}.type

    fun loadCart(uid: String): List<Book> =
        read<List<Book>>("cart_$uid", cartType) ?: emptyList()

    fun saveCart(uid: String, cart: List<Book>) = write("cart_$uid", cart)

    fun loadLibrary(uid: String): Map<String, LibraryEntry> =
        read<Map<String, LibraryEntry>>("library_$uid", libraryType) ?: emptyMap()

    fun saveLibrary(uid: String, library: Map<String, LibraryEntry>) = write("library_$uid", library)

    fun loadProfile(uid: String): Profile? = read("profile_$uid", Profile::class.java)

    fun saveProfile(uid: String, profile: Profile) = write("profile_$uid", profile)

    @Suppress("USELESS_ELVIS")
    fun loadSettings(): AppSettings {
        val saved = read<AppSettings>("settings", AppSettings::class.java) ?: return AppSettings()
        // Gson can leave fields null if the saved JSON is from an older version of the app.
        val defaults = AppSettings()
        return AppSettings(
            fontSize = saved.fontSize ?: defaults.fontSize,
            readerTheme = saved.readerTheme ?: defaults.readerTheme,
            dailyReminder = saved.dailyReminder,
        )
    }

    fun saveSettings(settings: AppSettings) = write("settings", settings)

    private fun <T> read(key: String, type: java.lang.reflect.Type): T? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching { gson.fromJson<T>(json, type) }.getOrNull()
    }

    private fun write(key: String, value: Any) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }
}
