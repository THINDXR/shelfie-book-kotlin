@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.data.model.formatBaht
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.CircleIconButton
import com.example.shelfie.ui.components.CoverFan
import com.example.shelfie.ui.components.EmptyState
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.components.SecondaryButton
import com.example.shelfie.ui.components.SoftPill
import com.example.shelfie.ui.theme.BlushSoft
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Maroon
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.PeachGradient
import com.example.shelfie.ui.theme.SoftSurface
import com.example.shelfie.ui.theme.SuccessBg
import com.example.shelfie.ui.theme.SuccessInk
import com.example.shelfie.ui.theme.TitleSerif
import com.example.shelfie.ui.theme.colors

@Composable
fun BookDetailScreen(
    bookKey: String,
    vm: AppViewModel,
    onBack: () -> Unit,
    onRead: (String) -> Unit,
    onGoCart: () -> Unit,
) {
    val book = vm.findBook(bookKey)
    if (book == null) {
        EmptyState(Icons.Outlined.AutoStories, "ไม่พบข้อมูลหนังสือ", "ลองกลับไปเลือกใหม่อีกครั้ง", "ย้อนกลับ", onBack)
        return
    }
    val library by vm.library.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val descriptions by vm.descriptions.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(bookKey) { vm.loadDescription(bookKey) }

    val entry = library[book.key]
    val owned = entry?.owned == true
    val inCart = cart.any { it.key == book.key }
    val description = descriptions[book.key]

    Column(
        Modifier
            .fillMaxSize()
            .background(PeachGradient)
    ) {
        // Top bar: back, bookmark (= เก็บเข้าชั้น shortcut), cart
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "ย้อนกลับ", onBack)
            Box(Modifier.weight(1f))
            CircleIconButton(
                icon = if (entry != null) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (entry != null) "นำออกจากชั้นหนังสือ" else "เก็บเข้าชั้น",
                onClick = {
                    if (entry == null) vm.setStatus(book, ReadingStatus.SHELF) else vm.removeFromLibrary(book.key)
                },
                tint = Maroon,
            )
            Box(Modifier.padding(4.dp))
            CircleIconButton(Icons.Outlined.ShoppingBag, "ตะกร้าสินค้า", onGoCart, badge = cart.size)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoverFan(front = book, width = 160.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    book.title,
                    fontFamily = TitleSerif,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    listOfNotNull(book.authors, book.year?.let { "พิมพ์ครั้งแรก $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    book.isFreeToRead -> SoftPill("อ่านฟรี · เนื้อหาเต็มเล่ม", SuccessBg, SuccessInk)
                    book.isBorrowable -> SoftPill("ยืมอ่านได้บน Open Library", SoftSurface, InkMuted)
                    else -> SoftPill("e-book · EPUB", SoftSurface, InkMuted)
                }
                if (owned && !book.isFreeToRead) SoftPill("ซื้อแล้ว", SuccessBg, SuccessInk)
                book.subjects.orEmpty().take(3).forEach { SoftPill(it, BlushSoft, Maroon) }
            }

            Text(
                when (description) {
                    null -> "กำลังโหลดเรื่องย่อจาก Open Library…"
                    "" -> "Open Library ยังไม่มีเรื่องย่อของเล่มนี้"
                    else -> description
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (description.isNullOrEmpty()) Muted else Ink,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("สถานะของฉัน", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReadingStatus.entries.forEach { status ->
                        val selected = entry?.status == status
                        val c = status.colors()
                        Surface(
                            onClick = { vm.setStatus(book, status) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) c.background else Color.White,
                            border = BorderStroke(1.5.dp, if (selected) c.content else Line),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    status.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) c.content else Ink,
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { uriHandler.openUri(book.webUrl) }) {
                androidx.compose.material3.Icon(
                    if (book.isBorrowable) Icons.Outlined.LocalLibrary else Icons.Outlined.Public,
                    contentDescription = null,
                    tint = Maroon,
                )
                Text(
                    if (book.isBorrowable) "  ยืมอ่านฟรีบน Open Library (ต้องล็อกอินบนเว็บ)" else "  ดูหน้านี้บน Open Library",
                    color = Maroon,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        HorizontalDivider(color = Line)
        Surface(color = Color.White) {
            if (owned) {
                PrimaryButton(
                    text = when (entry?.status) {
                        ReadingStatus.READING -> "อ่านต่อ (${entry?.progress ?: 0}%)"
                        ReadingStatus.DONE -> "อ่านอีกครั้ง"
                        else -> "เริ่มอ่าน"
                    },
                    onClick = { if (vm.startReading(book)) onRead(book.key) },
                    icon = Icons.Outlined.AutoStories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            } else if (book.isFreeToRead) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text("ลิขสิทธิ์หมดแล้ว", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("ฟรี", fontFamily = TitleSerif, fontSize = 28.sp)
                    }
                    PrimaryButton(
                        text = "อ่านฟรีเลย",
                        onClick = { if (vm.startReading(book)) onRead(book.key) },
                        icon = Icons.Outlined.AutoStories,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column {
                        Text("ราคา e-book", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(formatBaht(book.price), fontFamily = TitleSerif, fontSize = 28.sp)
                    }
                    SecondaryButton(
                        text = if (inCart) "ดูตะกร้า" else "ใส่ตะกร้า",
                        onClick = { if (inCart) onGoCart() else vm.addToCart(book) },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = "ซื้อเลย",
                        onClick = {
                            vm.ensureInCart(book)
                            onGoCart()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
