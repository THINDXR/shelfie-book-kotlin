package com.example.shelfie.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfie.data.auth.AuthRepositoryFactory
import com.example.shelfie.data.auth.AuthResult
import com.example.shelfie.data.auth.AuthUser
import com.example.shelfie.data.auth.AuthValidator
import com.example.shelfie.data.local.UserDataStore
import com.example.shelfie.data.model.AppSettings
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.BookText
import com.example.shelfie.data.model.Categories
import com.example.shelfie.data.model.Category
import com.example.shelfie.data.model.FontSizeOption
import com.example.shelfie.data.model.LibraryEntry
import com.example.shelfie.data.model.PaymentMethod
import com.example.shelfie.data.model.Profile
import com.example.shelfie.data.model.ReaderTheme
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.data.model.Receipt
import com.example.shelfie.data.remote.ArchiveTextSource
import com.example.shelfie.data.remote.OpenLibraryClient
import com.example.shelfie.data.remote.descriptionText
import com.example.shelfie.data.remote.toBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val category: Category = Categories.all.first(),
    val query: String = "",
    val loading: Boolean = false,
    val books: List<Book> = emptyList(),
    val error: String? = null,
)

/** Description of a work: null while loading, "" when Open Library has none. */
typealias DescriptionMap = Map<String, String>

/** Loading state of a free book's real text. */
sealed interface TextState {
    data object Loading : TextState
    data class Ready(val text: BookText) : TextState
    data object Failed : TextState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepositoryFactory.create(app)
    private val store = UserDataStore(app)
    private val api = OpenLibraryClient.api

    val usesCloudAuth: Boolean = auth.isCloud

    private val _user = MutableStateFlow(auth.currentUser())
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    private val _profile = MutableStateFlow(Profile(""))
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _cart = MutableStateFlow<List<Book>>(emptyList())
    val cart: StateFlow<List<Book>> = _cart.asStateFlow()

    private val _library = MutableStateFlow<Map<String, LibraryEntry>>(emptyMap())
    val library: StateFlow<Map<String, LibraryEntry>> = _library.asStateFlow()

    private val _libraryFilter = MutableStateFlow(ReadingStatus.READING)
    val libraryFilter: StateFlow<ReadingStatus> = _libraryFilter.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _discover = MutableStateFlow(DiscoverUiState())
    val discover: StateFlow<DiscoverUiState> = _discover.asStateFlow()

    private val _descriptions = MutableStateFlow<DescriptionMap>(emptyMap())
    val descriptions: StateFlow<DescriptionMap> = _descriptions.asStateFlow()

    private val _bookTexts = MutableStateFlow<Map<String, TextState>>(emptyMap())
    val bookTexts: StateFlow<Map<String, TextState>> = _bookTexts.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val bookIndex = mutableMapOf<String, Book>()
    private val resultCache = mutableMapOf<String, List<Book>>() // Open Library asks apps to cache
    private var searchJob: Job? = null

    init {
        _user.value?.let { loadUserData(it) }
        loadDiscover()
    }

    // ---------------- Auth ----------------

    suspend fun login(email: String, password: String): AuthResult {
        AuthValidator.check(email, password)?.let { return it }
        val result = auth.login(email, password)
        if (result is AuthResult.Success) onSignedIn(result.user, welcome = "ยินดีต้อนรับกลับ ${result.user.displayName}")
        return result
    }

    suspend fun register(name: String, email: String, password: String, confirm: String): AuthResult {
        AuthValidator.checkRegister(name, email, password, confirm)?.let { return it }
        val result = auth.register(name, email, password)
        if (result is AuthResult.Success) {
            store.saveProfile(result.user.uid, Profile(name.trim()))
            onSignedIn(result.user, welcome = "สมัครสมาชิกสำเร็จ ยินดีต้อนรับ!")
        }
        return result
    }

    private fun onSignedIn(user: AuthUser, welcome: String) {
        loadUserData(user)
        _user.value = user
        toast(welcome)
    }

    fun logout() {
        auth.logout()
        _user.value = null
        _cart.value = emptyList()
        _library.value = emptyMap()
        _profile.value = Profile("")
    }

    private fun loadUserData(user: AuthUser) {
        _cart.value = store.loadCart(user.uid)
        _library.value = store.loadLibrary(user.uid)
        _profile.value = store.loadProfile(user.uid) ?: Profile(user.displayName)
        _cart.value.forEach { bookIndex[it.key] = it }
        _library.value.values.forEach { bookIndex[it.book.key] = it.book }
    }

    // ---------------- Discover ----------------

    fun selectCategory(category: Category) {
        _discover.update { it.copy(category = category) }
        loadDiscover()
    }

    /** Waits until typing stops for half a second before calling the API. */
    fun onQueryChange(query: String) {
        _discover.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchDiscover()
        }
    }

    fun loadDiscover() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { fetchDiscover() }
    }

    fun resetDiscover() {
        _discover.update { it.copy(query = "", category = Categories.all.first()) }
        loadDiscover()
    }

    private suspend fun fetchDiscover() {
        val state = _discover.value
        val text = state.query.trim()
        val cacheKey = "${state.category.id}|${text.lowercase()}"
        resultCache[cacheKey]?.let { cached ->
            _discover.update { it.copy(loading = false, books = cached, error = null) }
            return
        }
        _discover.update { it.copy(loading = true, error = null) }
        try {
            val books = if (text.isEmpty() && state.category.query == null) {
                api.trending().works.orEmpty().mapNotNull { it.toBook() }
            } else {
                api.search(buildQuery(text, state.category)).docs.orEmpty().mapNotNull { it.toBook() }
            }.distinctBy { it.key }
            books.forEach { bookIndex[it.key] = it }
            resultCache[cacheKey] = books
            _discover.update { it.copy(loading = false, books = books) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _discover.update {
                it.copy(loading = false, error = "โหลดข้อมูลไม่สำเร็จ ตรวจสอบอินเทอร์เน็ตแล้วลองใหม่")
            }
        }
    }

    private fun buildQuery(text: String, category: Category): String {
        val categoryQuery = category.query ?: return text
        return if (text.isEmpty()) categoryQuery else "($text) AND ($categoryQuery)"
    }

    fun findBook(key: String): Book? =
        bookIndex[key] ?: _library.value[key]?.book ?: _cart.value.firstOrNull { it.key == key }

    fun loadDescription(key: String) {
        if (_descriptions.value.containsKey(key)) return
        viewModelScope.launch {
            val text = try {
                api.work(key).descriptionText().orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ""
            }
            _descriptions.update { it + (key to text) }
        }
    }

    // ---------------- Cart ----------------

    fun isInCart(key: String) = _cart.value.any { it.key == key }

    fun addToCart(book: Book) {
        when {
            _library.value[book.key]?.owned == true -> toast("คุณมีเล่มนี้อยู่แล้ว")
            book.isFreeToRead -> {
                claimFree(book)
                toast("เล่มนี้อ่านฟรี เพิ่มเข้าชั้นหนังสือให้แล้ว")
            }
            isInCart(book.key) -> toast("เล่มนี้อยู่ในตะกร้าแล้ว")
            else -> {
                _cart.update { it + book }
                saveCart()
                toast("เพิ่ม “${book.title}” ลงตะกร้าแล้ว")
            }
        }
    }

    /** Puts the book in the cart without a toast (used by "ซื้อเลย"). */
    fun ensureInCart(book: Book) {
        if (!book.isFreeToRead && !isInCart(book.key) && _library.value[book.key]?.owned != true) {
            _cart.update { it + book }
            saveCart()
        }
    }

    fun removeFromCart(key: String) {
        _cart.update { list -> list.filterNot { it.key == key } }
        saveCart()
        toast("ลบออกจากตะกร้าแล้ว")
    }

    fun saveForLater(book: Book) {
        _cart.update { list -> list.filterNot { it.key == book.key } }
        saveCart()
        setStatus(book, ReadingStatus.SHELF, announce = false)
        toast("ย้ายไป “เก็บเข้าชั้น” แล้ว")
    }

    /** Mock payment: every book in the cart becomes owned and lands on the shelf. */
    fun checkout(method: PaymentMethod): Receipt? {
        val items = _cart.value
        if (items.isEmpty()) return null
        _library.update { lib ->
            val next = lib.toMutableMap()
            items.forEach { book ->
                val current = lib[book.key]
                next[book.key] = current?.copy(book = book, owned = true)
                    ?: LibraryEntry(book = book, status = ReadingStatus.SHELF, owned = true, progress = 0)
            }
            next
        }
        _cart.value = emptyList()
        saveCart()
        saveLibrary()
        _libraryFilter.value = ReadingStatus.SHELF
        return Receipt(items.map { it.title }, items.sumOf { it.price }, method)
    }

    // ---------------- Library ----------------

    fun setLibraryFilter(status: ReadingStatus) {
        _libraryFilter.value = status
    }

    fun setStatus(book: Book, status: ReadingStatus, announce: Boolean = true) {
        _library.update { lib ->
            val current = lib[book.key]
            val progress = when (status) {
                ReadingStatus.DONE -> 100
                ReadingStatus.READING -> current?.progress?.takeIf { it < 100 } ?: 0
                ReadingStatus.SHELF -> current?.progress ?: 0
            }
            val page = if (status == ReadingStatus.READING && current?.progress == 100) 0 else current?.page ?: 0
            val entry = current?.copy(
                book = book,
                status = status,
                progress = progress,
                page = page,
                updatedAt = System.currentTimeMillis(),
            ) ?: LibraryEntry(book, status, owned = book.isFreeToRead, progress = progress)
            lib + (book.key to entry)
        }
        saveLibrary()
        if (announce) toast("ย้ายไป “${status.label}” แล้ว")
    }

    /** Free (public domain) books need no payment: add to the shelf as owned. */
    fun claimFree(book: Book) {
        if (!book.isFreeToRead) return
        _library.update { lib ->
            val current = lib[book.key]
            lib + (book.key to (current?.copy(book = book, owned = true)
                ?: LibraryEntry(book, ReadingStatus.SHELF, owned = true, progress = 0)))
        }
        _cart.update { list -> list.filterNot { it.key == book.key } }
        saveCart()
        saveLibrary()
    }

    fun removeFromLibrary(key: String) {
        val entry = _library.value[key] ?: return
        if (entry.owned) {
            toast("เล่มที่ซื้อหรือรับฟรีแล้วจะอยู่ในชั้นเสมอ")
            return
        }
        _library.update { it - key }
        saveLibrary()
        toast("นำออกจากชั้นแล้ว")
    }

    /** Returns true if the reader can open (free books open right away; others must be bought). */
    fun startReading(book: Book): Boolean {
        // Prefer the newest copy from search: it carries ebook_access and the Internet Archive id.
        val fresh = bookIndex[book.key] ?: book
        if (fresh.isFreeToRead) claimFree(fresh)
        val entry = _library.value[fresh.key]
        if (entry?.owned != true) {
            toast("ซื้อ e-book ก่อนจึงจะอ่านได้")
            return false
        }
        if (entry.status != ReadingStatus.READING) setStatus(fresh, ReadingStatus.READING, announce = false)
        return true
    }

    /** Downloads the real text of a free book once, then keeps it in memory. */
    fun loadBookText(book: Book) {
        val ia = book.iaId ?: return
        if (!book.isFreeToRead) return
        val state = _bookTexts.value[book.key]
        if (state is TextState.Ready || state is TextState.Loading) return
        _bookTexts.update { it + (book.key to TextState.Loading) }
        viewModelScope.launch {
            val result = try {
                TextState.Ready(ArchiveTextSource.load(ia))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TextState.Failed
            }
            _bookTexts.update { it + (book.key to result) }
        }
    }

    /** Real pages (free books): progress follows the page number; the last page marks the book as read. */
    fun goToPage(key: String, page: Int, totalPages: Int) {
        val entry = _library.value[key] ?: return
        if (totalPages <= 0) return
        val p = page.coerceIn(0, totalPages - 1)
        val progress = if (p == totalPages - 1) 100 else ((p + 1) * 100 / totalPages).coerceAtMost(99)
        val finished = progress >= 100 && entry.status != ReadingStatus.DONE
        val status = if (progress >= 100) ReadingStatus.DONE else ReadingStatus.READING
        _library.update {
            it + (key to entry.copy(page = p, progress = progress, status = status, updatedAt = System.currentTimeMillis()))
        }
        saveLibrary()
        if (finished) toast("อ่านจบแล้ว! ย้ายไป “อ่านจบแล้ว” ให้อัตโนมัติ")
    }

    /** Sample pages (books without real text): each page adds 5%. */
    fun turnPage(key: String) {
        val entry = _library.value[key] ?: return
        val progress = (entry.progress + 5).coerceAtMost(100)
        val status = if (progress >= 100) ReadingStatus.DONE else ReadingStatus.READING
        _library.update { it + (key to entry.copy(progress = progress, status = status, updatedAt = System.currentTimeMillis())) }
        saveLibrary()
        if (status == ReadingStatus.DONE) toast("อ่านจบแล้ว! ย้ายไป “อ่านจบแล้ว” ให้อัตโนมัติ")
    }

    // ---------------- Settings & profile ----------------

    fun setFontSize(option: FontSizeOption) = updateSettings { it.copy(fontSize = option) }

    fun stepFontSize(delta: Int) {
        val values = FontSizeOption.entries
        val index = (values.indexOf(_settings.value.fontSize) + delta).coerceIn(0, values.lastIndex)
        setFontSize(values[index])
    }

    fun setReaderTheme(theme: ReaderTheme) = updateSettings { it.copy(readerTheme = theme) }

    fun setDailyReminder(on: Boolean) = updateSettings { it.copy(dailyReminder = on) }

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        _settings.update(change)
        store.saveSettings(_settings.value)
    }

    fun updateProfile(name: String, bio: String) {
        val uid = _user.value?.uid ?: return
        if (name.isBlank()) {
            toast("ชื่อที่แสดงต้องไม่ว่าง")
            return
        }
        val profile = Profile(name.trim(), bio.trim())
        _profile.value = profile
        store.saveProfile(uid, profile)
        _user.update { it?.copy(displayName = profile.name) }
        viewModelScope.launch { auth.updateDisplayName(profile.name) }
        toast("บันทึกโปรไฟล์แล้ว")
    }

    // ---------------- helpers ----------------

    private fun saveCart() {
        _user.value?.let { store.saveCart(it.uid, _cart.value) }
    }

    private fun saveLibrary() {
        _user.value?.let { store.saveLibrary(it.uid, _library.value) }
    }

    private fun toast(message: String) {
        _messages.tryEmit(message)
    }
}
