package com.example.shelfie.data.remote

import com.example.shelfie.data.model.Book
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------- JSON shapes (only the fields we use) ----------

data class SearchResponse(
    @SerializedName("numFound") val numFound: Int?,
    @SerializedName("docs") val docs: List<SearchDoc>?,
)

data class TrendingResponse(
    @SerializedName("works") val works: List<SearchDoc>?,
)

data class SearchDoc(
    @SerializedName("key") val key: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("first_publish_year") val firstPublishYear: Int?,
    @SerializedName("cover_i") val coverId: Int?,
    @SerializedName("subject") val subject: List<String>?,
    @SerializedName("ebook_access") val ebookAccess: String?,
    @SerializedName("ia") val ia: List<String>?,
)

data class WorkDto(
    // Open Library returns either "text" or {"type": "...", "value": "text"}
    @SerializedName("description") val description: JsonElement?,
    @SerializedName("subjects") val subjects: List<String>?,
)

fun SearchDoc.toBook(): Book? {
    val id = key?.substringAfterLast('/') ?: return null
    val name = title ?: return null
    return Book(
        key = id,
        title = name,
        authors = authorName?.take(2)?.joinToString(", ") ?: "ไม่ระบุผู้แต่ง",
        year = firstPublishYear,
        coverId = coverId,
        subjects = subject?.take(6),
        ebookAccess = ebookAccess,
        iaId = ia?.firstOrNull { it.isNotBlank() },
    )
}

fun WorkDto.descriptionText(): String? {
    val d = description ?: return null
    return when {
        d.isJsonNull -> null
        d.isJsonPrimitive -> d.asString
        d.isJsonObject -> d.asJsonObject.get("value")?.takeIf { it.isJsonPrimitive }?.asString
        else -> null
    }?.trim()?.takeIf { it.isNotEmpty() }
}

// ---------- Endpoints ----------

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("limit") limit: Int = 24,
    ): SearchResponse

    @GET("trending/daily.json")
    suspend fun trending(
        @Query("limit") limit: Int = 24,
    ): TrendingResponse

    @GET("works/{id}.json")
    suspend fun work(@Path("id") id: String): WorkDto

    companion object {
        const val SEARCH_FIELDS = "key,title,author_name,first_publish_year,cover_i,subject,ebook_access,ia"
    }
}

object OpenLibraryClient {

    /**
     * Open Library asks apps to identify themselves (name + contact email).
     * Identified apps get 3 requests/second instead of 1. Change the email to yours.
     */
    private const val USER_AGENT = "ShelfieStudentApp/1.0 (your-email@example.com)"

    internal val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }
        .build()

    val api: OpenLibraryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApi::class.java)
    }
}
