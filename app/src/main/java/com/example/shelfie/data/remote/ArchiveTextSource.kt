package com.example.shelfie.data.remote

import com.example.shelfie.data.model.BookText
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/**
 * Loads the full text of a FREE (public domain) book from Internet Archive.
 *
 * Open Library's search tells us which books are free (ebook_access = "public")
 * and gives the Internet Archive id (ia). The scanned book's OCR text lives at
 *   https://archive.org/download/{ia}/{ia}_djvu.txt
 * If that file name doesn't exist for a book, we ask the item's file list
 *   https://archive.org/metadata/{ia}
 * for any *_djvu.txt file.
 *
 * Only call this for books the user opens. Never download many books in a loop.
 */
object ArchiveTextSource {

    private data class Metadata(@SerializedName("files") val files: List<FileEntry>?)
    private data class FileEntry(@SerializedName("name") val name: String?)

    private val gson = Gson()

    /** About this many characters per page in the reader. */
    private const val PAGE_CHARS = 1600

    suspend fun load(iaId: String): BookText = withContext(Dispatchers.IO) {
        val raw = fetch("https://archive.org/download/$iaId/${iaId}_djvu.txt")
            ?: findTextFileName(iaId)?.let { fetch("https://archive.org/download/$iaId/$it") }
            ?: throw IOException("no text file for $iaId")
        val pages = paginate(clean(raw))
        if (pages.isEmpty()) throw IOException("empty text for $iaId")
        BookText(pages)
    }

    /** Returns the body, or null for 404 / 403 (file missing or not public). */
    private fun fetch(url: String): String? {
        val request = Request.Builder().url(url).build()
        OpenLibraryClient.http.newCall(request).execute().use { response ->
            if (response.code == 404 || response.code == 403) return null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string()
        }
    }

    private fun findTextFileName(iaId: String): String? {
        val json = fetch("https://archive.org/metadata/$iaId") ?: return null
        val files = runCatching { gson.fromJson(json, Metadata::class.java) }.getOrNull()?.files.orEmpty()
        return files.mapNotNull { it.name }.firstOrNull { it.endsWith("_djvu.txt") }
    }

    /**
     * OCR text has a line break at the end of every scanned line.
     * Join those lines back into paragraphs and undo words split with "-" at line ends.
     */
    internal fun clean(raw: String): List<String> =
        raw.replace("\r\n", "\n")
            .replace('\u000c', '\n') // page-break characters
            .split(Regex("\\n\\s*\\n"))
            .map { block ->
                block.replace(Regex("-\\n(?=\\p{Ll})"), "")
                    .replace(Regex("\\s*\\n\\s*"), " ")
                    .replace(Regex(" {2,}"), " ")
                    .trim()
            }
            .filter { it.length > 1 && !it.matches(Regex("[\\divxlcIVXLC.\\s]{1,8}")) } // drop bare page numbers

    /** Groups paragraphs into pages of roughly [PAGE_CHARS] characters. */
    internal fun paginate(paragraphs: List<String>): List<String> {
        val pages = mutableListOf<String>()
        val current = StringBuilder()
        // Some scans have no blank lines at all, so very long "paragraphs" are cut at sentence ends.
        val pieces = paragraphs.flatMap { splitLong(it) }
        for (p in pieces) {
            if (current.isNotEmpty() && current.length + p.length > PAGE_CHARS) {
                pages += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(p)
        }
        if (current.isNotEmpty()) pages += current.toString()
        return pages
    }

    private fun splitLong(text: String): List<String> {
        if (text.length <= PAGE_CHARS) return listOf(text)
        val parts = mutableListOf<String>()
        var rest = text
        while (rest.length > PAGE_CHARS) {
            val window = rest.substring(0, PAGE_CHARS)
            val cut = maxOf(window.lastIndexOf(". "), window.lastIndexOf("? "), window.lastIndexOf("! "))
                .takeIf { it > PAGE_CHARS / 2 }?.plus(1)
                ?: window.lastIndexOf(' ').takeIf { it > 0 }
                ?: PAGE_CHARS
            parts += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) parts += rest
        return parts
    }
}
