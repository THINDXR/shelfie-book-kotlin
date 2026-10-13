@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.Categories
import com.example.shelfie.data.model.LibraryEntry
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.data.model.formatBaht
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.BookCover
import com.example.shelfie.ui.components.CircleIconButton
import com.example.shelfie.ui.components.EmptyState
import com.example.shelfie.ui.components.ProgressLine
import com.example.shelfie.ui.components.SoftPill
import com.example.shelfie.ui.components.StatusPill
import com.example.shelfie.ui.theme.SuccessBg
import com.example.shelfie.ui.theme.SuccessInk
import com.example.shelfie.ui.theme.BlushSoft
import com.example.shelfie.ui.theme.FieldLine
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Maroon
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.PeachGradient
import com.example.shelfie.ui.theme.SoftSurface
import com.example.shelfie.ui.theme.ThaiSerif
import com.example.shelfie.ui.theme.TitleSerif

@Composable
fun DiscoverScreen(
    vm: AppViewModel,
    onOpenBook: (String) -> Unit,
    onRead: (String) -> Unit,
    onGoCart: () -> Unit,
    onGoProfile: () -> Unit,
) {
    val state by vm.discover.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    val continueEntry = library.values
        .filter { it.owned && it.status == ReadingStatus.READING }
        .maxByOrNull { it.updatedAt }
    val cartKeys = cart.map { it.key }.toSet()
    val full = GridItemSpan(2)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PeachGradient),
    ) {
        // Header: greeting + shortcuts to cart and profile
        item(span = { full }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("สวัสดี ${profile.name}", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    Text("ค้นพบหนังสือเล่มต่อไป", style = MaterialTheme.typography.headlineLarge)
                }
                CircleIconButton(Icons.Outlined.ShoppingBag, "ตะกร้าสินค้า", onGoCart, badge = cart.size)
                Spacer(Modifier.width(8.dp))
                Surface(onClick = onGoProfile, shape = CircleShape, color = Maroon, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.name.take(1).uppercase().ifEmpty { "?" },
                            color = Color.White,
                            fontFamily = ThaiSerif,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }

        // Search box (searches Open Library as you type, after a short pause)
        item(span = { full }) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("ค้นหาชื่อหนังสือ หรือผู้แต่ง", color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "ล้างคำค้นหา")
                        }
                    }
                } else null,
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focus.clearFocus()
                    vm.loadDiscover()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    cursorColor = Ink,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Category chips (based on Open Library Collections)
        item(span = { full }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Categories.all, key = { it.id }) { category ->
                    val selected = category.id == state.category.id
                    Surface(
                        onClick = { vm.selectCategory(category) },
                        shape = CircleShape,
                        color = if (selected) Ink else Color.White,
                        border = BorderStroke(1.dp, if (selected) Ink else Line),
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                category.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else Ink,
                            )
                        }
                    }
                }
            }
        }

        // Shortcut: continue reading
        if (continueEntry != null && state.query.isBlank()) {
            item(span = { full }) {
                ContinueCard(continueEntry) { onRead(continueEntry.book.key) }
            }
        }

        item(span = { full }) {
            Column {
                Text(
                    if (state.query.isBlank()) state.category.label.let { if (state.category.id == "all") "มาแรงวันนี้" else it }
                    else "ผลการค้นหา “${state.query.trim()}”",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    if (state.query.isBlank()) state.category.description
                    else if (state.loading) "กำลังค้นหา…" else "พบ ${state.books.size} เล่ม",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        when {
            state.loading -> item(span = { full }) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Maroon)
                }
            }

            state.error != null -> item(span = { full }) {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "โหลดไม่สำเร็จ",
                    body = state.error.orEmpty(),
                    actionLabel = "ลองใหม่",
                    onAction = vm::loadDiscover,
                )
            }

            state.books.isEmpty() -> item(span = { full }) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "ไม่พบหนังสือ",
                    body = "ลองค้นด้วยชื่อภาษาอังกฤษหรือชื่อผู้แต่ง หรือเลือกหมวด “ทั้งหมด”",
                    actionLabel = "ล้างตัวกรอง",
                    onAction = vm::resetDiscover,
                )
            }

            else -> items(state.books, key = { it.key }) { book ->
                BookGridCard(
                    book = book,
                    entry = library[book.key],
                    inCart = book.key in cartKeys,
                    onOpen = { onOpenBook(book.key) },
                    onQuick = {
                        val entry = library[book.key]
                        when {
                            entry?.owned == true || book.isFreeToRead -> if (vm.startReading(book)) onRead(book.key)
                            book.key in cartKeys -> onGoCart()
                            else -> vm.addToCart(book)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(entry: LibraryEntry, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Ink, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(entry.book, Modifier.size(52.dp, 74.dp), titleSize = 10.sp, corner = 6.dp, elevation = 0.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("อ่านต่อจากครั้งที่แล้ว", color = Color(0xFFF2C4BC), style = MaterialTheme.typography.labelMedium)
                Text(
                    entry.book.title,
                    color = Color.White,
                    fontFamily = TitleSerif,
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressLine(
                        entry.progress,
                        Modifier.weight(1f),
                        track = Color(0xFF4A403C),
                        bar = Color(0xFFF2B8B0),
                    )
                    Text("  ${entry.progress}%", color = Color(0xFFE8DFDA), style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "อ่านต่อ", tint = Ink)
                }
            }
        }
    }
}

@Composable
private fun BookGridCard(
    book: Book,
    entry: LibraryEntry?,
    inCart: Boolean,
    onOpen: () -> Unit,
    onQuick: () -> Unit,
) {
    val owned = entry?.owned == true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = onOpen, color = Color.Transparent) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    BookCover(
                        book,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4.3f),
                        titleSize = 22.sp,
                        corner = 10.dp,
                        elevation = 8.dp,
                    )
                    if (entry != null) {
                        StatusPill(entry.status, Modifier.padding(8.dp))
                    } else if (book.isFreeToRead) {
                        SoftPill("อ่านฟรี", SuccessBg, SuccessInk, Modifier.padding(8.dp))
                    }
                }
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(book.authors, book.year?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    book.isFreeToRead -> "ฟรี"
                    owned -> "ซื้อแล้ว"
                    else -> formatBaht(book.price)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (book.isFreeToRead) SuccessInk else Ink,
                modifier = Modifier.weight(1f),
            )
            val (label, bg, fg) = when {
                owned -> Triple("อ่าน", Ink, Color.White)
                book.isFreeToRead -> Triple("อ่านฟรี", Ink, Color.White)
                inCart -> Triple("ในตะกร้า", SoftSurface, Ink)
                else -> Triple("+ ตะกร้า", Color.White, Ink)
            }
            Surface(
                onClick = onQuick,
                shape = CircleShape,
                color = bg,
                border = if (bg == Color.White) BorderStroke(1.dp, Ink) else null,
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
