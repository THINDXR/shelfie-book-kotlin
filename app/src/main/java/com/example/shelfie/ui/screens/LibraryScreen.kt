@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.LibraryEntry
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.data.model.formatBaht
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.BookCover
import com.example.shelfie.ui.components.EmptyState
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.components.ProgressLine
import com.example.shelfie.ui.components.SecondaryButton
import com.example.shelfie.ui.components.SoftPill
import com.example.shelfie.ui.components.StatusPill
import com.example.shelfie.ui.theme.Cream
import com.example.shelfie.ui.theme.ErrorInk
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.SoftSurface
import com.example.shelfie.ui.theme.TitleSerif
import com.example.shelfie.ui.theme.colors

@Composable
fun LibraryScreen(
    vm: AppViewModel,
    onOpenBook: (String) -> Unit,
    onRead: (String) -> Unit,
    onGoCart: () -> Unit,
    onGoDiscover: () -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val filter by vm.libraryFilter.collectAsStateWithLifecycle()

    val counts = ReadingStatus.entries.associateWith { s -> library.values.count { it.status == s } }
    val shelfItems = library.values.filter { it.status == filter }.sortedByDescending { it.updatedAt }
    val cartKeys = cart.map { it.key }.toSet()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Cream),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("ชั้นหนังสือของฉัน", style = MaterialTheme.typography.headlineLarge)
        }

        // Status counters double as filter tabs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ReadingStatus.READING, ReadingStatus.SHELF, ReadingStatus.DONE).forEach { status ->
                    val selected = status == filter
                    val c = status.colors()
                    Surface(
                        onClick = { vm.setLibraryFilter(status) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) c.background else Color.White,
                        border = BorderStroke(1.5.dp, if (selected) c.content else Line),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${counts[status] ?: 0}",
                                fontFamily = TitleSerif,
                                fontSize = 30.sp,
                                color = if (selected) c.content else Ink,
                            )
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

        if (shelfItems.isEmpty()) {
            item {
                val (title, body) = when (filter) {
                    ReadingStatus.READING -> "ยังไม่มีเล่มที่กำลังอ่าน" to "ซื้อ e-book แล้วกด “เริ่มอ่าน” หนังสือจะมาอยู่ที่นี่"
                    ReadingStatus.SHELF -> "ชั้นยังว่างอยู่" to "กดไอคอนบุ๊กมาร์กในหน้าหนังสือ เพื่อเก็บเล่มที่สนใจไว้ก่อน"
                    ReadingStatus.DONE -> "ยังไม่มีเล่มที่อ่านจบ" to "อ่านจนครบ 100% หนังสือจะย้ายมาที่นี่ให้อัตโนมัติ"
                }
                EmptyState(Icons.Outlined.AutoStories, title, body, "ไปค้นพบหนังสือ", onGoDiscover)
            }
        } else {
            items(shelfItems, key = { it.book.key }) { entry ->
                LibraryItem(
                    entry = entry,
                    inCart = entry.book.key in cartKeys,
                    onOpen = { onOpenBook(entry.book.key) },
                    onPrimary = {
                        when {
                            !entry.owned && entry.book.key in cartKeys -> onGoCart()
                            !entry.owned -> vm.addToCart(entry.book)
                            else -> if (vm.startReading(entry.book)) onRead(entry.book.key)
                        }
                    },
                    onSetStatus = { vm.setStatus(entry.book, it) },
                    onRemove = { vm.removeFromLibrary(entry.book.key) },
                )
            }
        }
    }
}

@Composable
private fun LibraryItem(
    entry: LibraryEntry,
    inCart: Boolean,
    onOpen: () -> Unit,
    onPrimary: () -> Unit,
    onSetStatus: (ReadingStatus) -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val book = entry.book

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = onOpen, color = Color.Transparent) {
                BookCover(book, Modifier.size(70.dp, 100.dp), titleSize = 13.sp, corner = 6.dp, elevation = 4.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = "เปลี่ยนสถานะ ${book.title}", tint = InkMuted)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Text(
                                "ย้ายไปที่สถานะ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            ReadingStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.label) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .background(status.colors().dot, CircleShape)
                                        )
                                    },
                                    trailingIcon = if (status == entry.status) {
                                        { Icon(Icons.Outlined.Check, contentDescription = "สถานะปัจจุบัน") }
                                    } else null,
                                    onClick = {
                                        menuOpen = false
                                        onSetStatus(status)
                                    },
                                )
                            }
                            if (!entry.owned) {
                                HorizontalDivider(color = Line)
                                DropdownMenuItem(
                                    text = { Text("นำออกจากชั้นหนังสือ", color = ErrorInk) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = ErrorInk) },
                                    onClick = {
                                        menuOpen = false
                                        onRemove()
                                    },
                                )
                            }
                        }
                    }
                }
                Text(book.authors, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(entry.status)
                    SoftPill(
                        when {
                            book.isFreeToRead -> "อ่านฟรี"
                            entry.owned -> "ซื้อแล้ว"
                            else -> "ยังไม่ได้ซื้อ · ${formatBaht(book.price)}"
                        },
                        SoftSurface,
                        InkMuted,
                    )
                }
                if (entry.status == ReadingStatus.READING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressLine(entry.progress, Modifier.weight(1f))
                        Text("  ${entry.progress}%", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    }
                }
                val label = when {
                    !entry.owned -> if (inCart) "ไปที่ตะกร้า" else "ใส่ตะกร้า"
                    entry.status == ReadingStatus.READING -> "อ่านต่อ"
                    entry.status == ReadingStatus.SHELF -> "เริ่มอ่าน"
                    else -> "อ่านอีกครั้ง"
                }
                if (entry.owned && entry.status == ReadingStatus.READING) {
                    PrimaryButton(label, onPrimary, Modifier.padding(top = 4.dp))
                } else {
                    SecondaryButton(label, onPrimary, Modifier.padding(top = 4.dp), small = true)
                }
            }
        }
    }
}
