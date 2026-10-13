@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.ReaderTheme
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.TextState
import com.example.shelfie.ui.components.EmptyState
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.components.ProgressLine
import com.example.shelfie.ui.components.SecondaryButton
import com.example.shelfie.ui.theme.FieldLine
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Maroon
import com.example.shelfie.ui.theme.ThaiSerif
import com.example.shelfie.ui.theme.TitleSerif
import com.example.shelfie.ui.theme.backgroundColor
import com.example.shelfie.ui.theme.contentColor

/**
 * E-book reader.
 *
 * - Free books (Open Library ebook_access = "public"): the real text is downloaded from
 *   Internet Archive and shown page by page. Progress follows the page you are on.
 * - Other books: Open Library has no text we may show, so the page shows the book's
 *   description as a sample, plus a link to borrow it on openlibrary.org when borrowable.
 */
@Composable
fun ReaderScreen(bookKey: String, vm: AppViewModel, onBack: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val descriptions by vm.descriptions.collectAsStateWithLifecycle()
    val texts by vm.bookTexts.collectAsStateWithLifecycle()
    var showPanel by rememberSaveable { mutableStateOf(false) }

    val entry = library[bookKey]
    if (entry == null) {
        EmptyState(Icons.Outlined.Public, "ไม่พบหนังสือในชั้น", "ซื้อหรือกดอ่านฟรีก่อนจึงจะเปิดอ่านได้", "ย้อนกลับ", onBack)
        return
    }
    val book = vm.findBook(bookKey)?.takeIf { it.iaId != null } ?: entry.book

    LaunchedEffect(bookKey, book.isFreeToRead) {
        if (book.isFreeToRead) vm.loadBookText(book) else vm.loadDescription(bookKey)
    }

    val theme = settings.readerTheme
    val colors = ReaderColors(
        bg = theme.backgroundColor,
        fg = theme.contentColor,
        chip = if (theme == ReaderTheme.NIGHT) Color(0xFF2C2927) else theme.contentColor.copy(alpha = 0.07f),
        line = if (theme == ReaderTheme.NIGHT) Color(0xFF3A3634) else theme.contentColor.copy(alpha = 0.12f),
        link = if (theme == ReaderTheme.NIGHT) Color(0xFFF2B8B0) else Maroon,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderIconButton(Icons.AutoMirrored.Filled.ArrowBack, "ปิดหน้าอ่าน", colors.chip, colors.fg, onBack)
            Text(
                book.title,
                color = colors.fg.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            ReaderIconButton(Icons.Outlined.Tune, "ธีมและการตั้งค่า", colors.chip, colors.fg) { showPanel = !showPanel }
        }

        Box(Modifier.weight(1f)) {
            if (book.isFreeToRead) {
                when (val state = texts[bookKey]) {
                    is TextState.Ready -> {
                        val pages = state.text.pages
                        val page = entry.page.coerceIn(0, pages.lastIndex)
                        // key(page): a new scroll position (top of page) every time the page changes
                        key(page) {
                            RealPage(book, pages[page], page, pages.size, colors)
                        }
                    }
                    TextState.Failed -> LoadFailed(book, colors) { vm.loadBookText(book) }
                    else -> Loading(colors)
                }
            } else {
                SamplePage(book, descriptions[bookKey], colors)
            }
        }

        if (showPanel) {
            SettingsPanel(vm = vm, selectedTheme = theme, fontLabel = settings.fontSize.label, onClose = { showPanel = false })
        }

        val ready = texts[bookKey] as? TextState.Ready
        if (book.isFreeToRead && ready != null) {
            val total = ready.text.pages.size
            val page = entry.page.coerceIn(0, total - 1)
            PagerBar(
                page = page,
                total = total,
                progress = entry.progress,
                colors = colors,
                onPrev = { vm.goToPage(bookKey, page - 1, total) },
                onNext = { vm.goToPage(bookKey, page + 1, total) },
            )
        } else if (!book.isFreeToRead) {
            SampleBar(entry.progress, colors) { vm.turnPage(bookKey) }
        }
    }
}

private data class ReaderColors(val bg: Color, val fg: Color, val chip: Color, val line: Color, val link: Color)

@Composable
private fun RealPage(book: Book, text: String, page: Int, total: Int, colors: ReaderColors) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (page == 0) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(book.title, fontFamily = TitleSerif, fontSize = 34.sp, lineHeight = 38.sp, color = colors.fg, textAlign = TextAlign.Center)
                Text(book.authors, fontFamily = TitleSerif, fontSize = 20.sp, color = colors.fg.copy(alpha = 0.8f))
            }
            Text(
                "ข้อความนี้มาจากการสแกนหนังสือจริงของ Internet Archive (OCR) อาจมีคำผิดหรือเลขหน้าปนอยู่บ้าง",
                style = MaterialTheme.typography.bodySmall,
                color = colors.fg.copy(alpha = 0.7f),
            )
        }
        text.split("\n\n").forEach { paragraph ->
            ReaderParagraph(paragraph, colors.fg, serif = false)
        }
        Text(
            "— หน้า ${page + 1} จาก $total —",
            style = MaterialTheme.typography.labelSmall,
            color = colors.fg.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SamplePage(book: Book, description: String?, colors: ReaderColors) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Chapter I", fontFamily = TitleSerif, fontSize = 38.sp, color = colors.fg)
            Text(book.title, fontFamily = TitleSerif, fontSize = 22.sp, color = colors.fg.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
        ReaderParagraph(
            if (book.isBorrowable) {
                "[ ตัวอย่างหน้าอ่าน ] เล่มนี้ยังมีลิขสิทธิ์ Open Library จึงให้ยืมอ่านบนเว็บเท่านั้น แอปแสดงได้แค่เรื่องย่อ กดปุ่มด้านล่างเพื่อยืมอ่านฟรีบน Open Library"
            } else {
                "[ ตัวอย่างหน้าอ่าน ] Open Library ไม่มี e-book ของเล่มนี้ หน้านี้จึงแสดงเรื่องย่อแทน ในแอปจริงจะเป็นเนื้อหาจากไฟล์ EPUB ที่ผู้ใช้ซื้อไว้"
            },
            colors.fg,
        )
        when (description) {
            null -> ReaderParagraph("กำลังโหลดเรื่องย่อ…", colors.fg)
            "" -> ReaderParagraph("เล่มนี้ยังไม่มีเรื่องย่อใน Open Library", colors.fg)
            else -> description.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
                .forEach { ReaderParagraph(it, colors.fg) }
        }
        if (book.isBorrowable) {
            SecondaryButton(
                text = "ยืมอ่านฟรีบน Open Library",
                onClick = { uriHandler.openUri(book.webUrl) },
                icon = Icons.Outlined.LocalLibrary,
                contentColor = Maroon,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextButton(onClick = { uriHandler.openUri(book.webUrl) }) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.link)
                Text("  ดูหน้านี้บน Open Library", color = colors.link, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ReaderParagraph(text: String, color: Color, serif: Boolean = true) {
    Text(
        text,
        color = color,
        fontFamily = if (serif) ThaiSerif else null,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 17.sp,
            lineHeight = 30.sp,
            textIndent = TextIndent(firstLine = 24.sp),
        ),
    )
}

@Composable
private fun Loading(colors: ReaderColors) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Maroon)
        Text(
            "กำลังโหลดหนังสือจาก Internet Archive…\nเล่มหนาอาจใช้เวลาสักครู่",
            color = colors.fg.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadFailed(book: Book, colors: ReaderColors, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = colors.link, modifier = Modifier.size(44.dp))
        Text("โหลดเนื้อหาไม่สำเร็จ", style = MaterialTheme.typography.headlineSmall, color = colors.fg)
        Text(
            "อินเทอร์เน็ตอาจหลุด หรือฉบับสแกนนี้ไม่มีไฟล์ข้อความ ลองใหม่อีกครั้ง หรือเปิดอ่านด้วยตัวอ่านของ Internet Archive",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.fg.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        PrimaryButton("ลองใหม่", onRetry)
        book.archiveReaderUrl?.let { url ->
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text("เปิดอ่านบน Internet Archive", color = colors.link, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PagerBar(
    page: Int,
    total: Int,
    progress: Int,
    colors: ReaderColors,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressLine(progress, Modifier.fillMaxWidth(), track = colors.line, bar = Maroon)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReaderIconButton(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                "หน้าก่อนหน้า",
                colors.chip,
                if (page > 0) colors.fg else colors.fg.copy(alpha = 0.3f),
            ) { if (page > 0) onPrev() }
            Text(
                "หน้า ${page + 1} / $total · อ่านไปแล้ว $progress%",
                color = colors.fg.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = if (page >= total - 1) "จบเล่ม" else "หน้าถัดไป",
                onClick = onNext,
                enabled = page < total - 1,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}

@Composable
private fun SampleBar(progress: Int, colors: ReaderColors, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressLine(progress, Modifier.fillMaxWidth(), track = colors.line, bar = Maroon)
            Text("อ่านไปแล้ว $progress%", color = colors.fg.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
        PrimaryButton(
            text = if (progress >= 100) "อ่านจบแล้ว" else "หน้าถัดไป",
            onClick = onNext,
            enabled = progress < 100,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ReaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, shape = CircleShape, color = background, modifier = Modifier.size(46.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

/** The "Themes and settings" card from the reference design: A / A buttons and theme swatches. */
@Composable
private fun SettingsPanel(vm: AppViewModel, selectedTheme: ReaderTheme, fontLabel: String, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ธีมและการตั้งค่า", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "ปิด", tint = Ink) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    Surface(
                        onClick = { vm.stepFontSize(-1) },
                        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
                        border = BorderStroke(1.dp, Line),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", fontFamily = TitleSerif, fontSize = 18.sp, color = Ink)
                        }
                    }
                    Surface(
                        onClick = { vm.stepFontSize(1) },
                        shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
                        border = BorderStroke(1.dp, Line),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", fontFamily = TitleSerif, fontSize = 26.sp, color = Ink)
                        }
                    }
                }
                Text(
                    fontLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReaderTheme.entries.forEach { t ->
                    val selected = t == selectedTheme
                    Surface(
                        onClick = { vm.setReaderTheme(t) },
                        shape = CircleShape,
                        color = t.backgroundColor,
                        border = BorderStroke(if (selected) 3.dp else 1.5.dp, if (selected) Ink else FieldLine),
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ก", color = t.contentColor, fontFamily = ThaiSerif)
                        }
                    }
                }
            }
        }
    }
}
