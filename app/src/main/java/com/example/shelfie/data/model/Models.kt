package com.example.shelfie.data.model

/**
 * One book (an Open Library "work"). [key] is the short id such as "OL45804W".
 */
data class Book(
    val key: String,
    val title: String,
    val authors: String,
    val year: Int?,
    val coverId: Int?,
    val subjects: List<String>? = null,
    /**
     * From Open Library search: "public" = free to read and download,
     * "borrowable" = must be borrowed on openlibrary.org, "printdisabled" / "no_ebook" = no e-book.
     */
    val ebookAccess: String? = null,
    /** Internet Archive identifier of a scanned edition (where the actual book file lives). */
    val iaId: String? = null,
) {
    /** Public-domain scan we are allowed to load and show inside the app. */
    val isFreeToRead: Boolean
        get() = ebookAccess == "public" && !iaId.isNullOrBlank()

    /** In copyright: the user can borrow it on openlibrary.org, but the app cannot load its text. */
    val isBorrowable: Boolean
        get() = ebookAccess == "borrowable"

    /**
     * Open Library does not sell books, so the e-book price is a mock value
     * derived from the key. Free-to-read books cost nothing.
     */
    val price: Int
        get() = if (isFreeToRead) 0 else 99 + Math.floorMod(key.hashCode(), 25) * 10

    /** Cover image from the Covers API. Sizes: 'S', 'M', 'L'. */
    fun coverUrl(size: Char = 'M'): String? =
        coverId?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }

    val webUrl: String
        get() = "https://openlibrary.org/works/$key"

    /** Internet Archive's own book reader (works in a browser). */
    val archiveReaderUrl: String?
        get() = iaId?.let { "https://archive.org/details/$it" }
}

/** A free book's text, split into pages for the reader. */
data class BookText(val pages: List<String>)

enum class ReadingStatus(val label: String) {
    READING("กำลังอ่าน"),
    SHELF("เก็บเข้าชั้น"),
    DONE("อ่านจบแล้ว"),
}

/** A book the user has put on their shelf, with its status and reading progress (0–100). */
data class LibraryEntry(
    val book: Book,
    val status: ReadingStatus,
    val owned: Boolean,
    val progress: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Last page read, for free books whose real text is loaded (0-based). */
    val page: Int = 0,
)

/**
 * Categories based on https://openlibrary.org/collections.
 * [query] is sent to search.json; null means "trending books".
 */
data class Category(
    val id: String,
    val label: String,
    val query: String?,
    val description: String,
)

object Categories {
    val all = listOf(
        Category("all", "ทั้งหมด", null, "หนังสือที่กำลังมาแรงบน Open Library วันนี้"),
        Category("free", "อ่านฟรี", "ebook_access:public", "หนังสือลิขสิทธิ์หมดแล้ว เปิดอ่านเนื้อหาจริงในแอปได้ฟรี"),
        Category("hugo", "รางวัล Hugo", "subject:\"hugo award\"", "นวนิยายไซไฟที่ได้รางวัล Hugo"),
        Category("banned", "หนังสือต้องห้าม", "subject:\"banned books\" OR subject:censorship", "หนังสือที่เคยถูกแบนหรือถูกร้องเรียนให้ถอดออกจากห้องสมุด"),
        Category("newbery", "เหรียญ Newbery", "subject:\"newbery medal\"", "วรรณกรรมเยาวชนที่ได้เหรียญ Newbery"),
        Category("horror", "สยองขวัญ", "subject:horror", "เรื่องหลอนชวนขนลุก สำหรับคืนที่นอนไม่หลับ"),
        Category("classic", "คลาสสิก", "subject:\"classic literature\"", "วรรณกรรมคลาสสิกที่ควรอ่านสักครั้งในชีวิต"),
        Category("math", "คณิต & วิทย์", "subject:mathematics", "จากคอลเลกชัน Learn Math และ Girls in STEM"),
    )
}

enum class FontSizeOption(val label: String, val scale: Float) {
    SMALL("เล็ก", 0.9f),
    MEDIUM("กลาง", 1.0f),
    LARGE("ใหญ่", 1.15f),
    XLARGE("ใหญ่มาก", 1.3f),
}

/** Colors are ARGB longs so this file stays free of Compose imports. */
enum class ReaderTheme(val label: String, val background: Long, val content: Long) {
    PAPER("กระดาษขาว", 0xFFFFFFFF, 0xFF1F1A17),
    BLUSH("ชมพูอ่อน", 0xFFFBECE6, 0xFF2A1F1B),
    SEPIA("ซีเปีย", 0xFFF3E7D3, 0xFF3B2F22),
    NIGHT("กลางคืน", 0xFF1C1A19, 0xFFE9E2DC),
}

data class AppSettings(
    val fontSize: FontSizeOption = FontSizeOption.MEDIUM,
    val readerTheme: ReaderTheme = ReaderTheme.BLUSH,
    val dailyReminder: Boolean = true,
)

data class Profile(
    val name: String,
    val bio: String = "",
)

enum class PaymentMethod(val label: String) {
    PROMPTPAY("พร้อมเพย์ (สแกน QR)"),
    CARD("บัตรเครดิต / เดบิต"),
    WALLET("TrueMoney Wallet"),
}

data class Receipt(
    val titles: List<String>,
    val total: Int,
    val method: PaymentMethod,
)

fun formatBaht(amount: Int): String = "฿" + "%,d".format(amount)
