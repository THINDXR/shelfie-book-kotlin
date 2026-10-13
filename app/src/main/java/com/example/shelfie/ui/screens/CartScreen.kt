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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.PaymentMethod
import com.example.shelfie.data.model.Receipt
import com.example.shelfie.data.model.formatBaht
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.BookCover
import com.example.shelfie.ui.components.EmptyState
import com.example.shelfie.ui.components.LabeledRow
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.components.SecondaryButton
import com.example.shelfie.ui.theme.Cream
import com.example.shelfie.ui.theme.ErrorInk
import com.example.shelfie.ui.theme.FieldLine
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.PeachGradient
import com.example.shelfie.ui.theme.TitleSerif

@Composable
fun CartScreen(
    vm: AppViewModel,
    onOpenBook: (String) -> Unit,
    onGoDiscover: () -> Unit,
    onGoLibrary: () -> Unit,
) {
    val cart by vm.cart.collectAsStateWithLifecycle()
    var method by rememberSaveable { mutableStateOf(PaymentMethod.PROMPTPAY) }
    var receipt by remember { mutableStateOf<Receipt?>(null) }

    receipt?.let { r ->
        PaymentSuccess(
            receipt = r,
            onLibrary = { receipt = null; onGoLibrary() },
            onShopMore = { receipt = null; onGoDiscover() },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("ตะกร้าของฉัน", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (cart.isNotEmpty()) Text("${cart.size} เล่ม", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }

        if (cart.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "ตะกร้ายังว่างอยู่",
                body = "เลือกหนังสือที่สนใจจากหน้าค้นพบ แล้วกด “+ ตะกร้า”",
                actionLabel = "เลือกซื้อหนังสือ",
                onAction = onGoDiscover,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cart, key = { it.key }) { book ->
                CartItem(
                    book = book,
                    onOpen = { onOpenBook(book.key) },
                    onLater = { vm.saveForLater(book) },
                    onRemove = { vm.removeFromCart(book.key) },
                )
            }
            item {
                Text(
                    "วิธีชำระเงิน",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(PaymentMethod.entries.toList(), key = { it.name }) { option ->
                val selected = option == method
                Surface(
                    onClick = { method = option },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, if (selected) Ink else FieldLine),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                ) {
                    Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected,
                            onClick = { method = option },
                            colors = RadioButtonDefaults.colors(selectedColor = Ink),
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                Text(
                    "* ระบบชำระเงินในแอปนี้เป็นการจำลอง ไม่มีการตัดเงินจริง",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        HorizontalDivider(color = Line)
        Surface(color = Color.White) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val total = cart.sumOf { it.price }
                LabeledRow("ราคาหนังสือ (${cart.size} เล่ม)", formatBaht(total))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ยอดชำระ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(formatBaht(total), fontFamily = TitleSerif, fontSize = 30.sp)
                }
                PrimaryButton(
                    text = "ชำระเงิน ${formatBaht(total)}",
                    onClick = { receipt = vm.checkout(method) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CartItem(book: Book, onOpen: () -> Unit, onLater: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = onOpen, color = Color.Transparent) {
                BookCover(book, Modifier.size(64.dp, 92.dp), titleSize = 12.sp, corner = 6.dp, elevation = 3.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.authors, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
                Text("e-book · EPUB", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatBaht(book.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "เก็บไว้ก่อน",
                        onClick = onLater,
                        icon = Icons.Outlined.BookmarkBorder,
                        small = true,
                        contentColor = InkMuted,
                    )
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "ลบ ${book.title} ออกจากตะกร้า", tint = ErrorInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSuccess(receipt: Receipt, onLibrary: () -> Unit, onShopMore: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PeachGradient)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = Ink, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Text("ชำระเงินสำเร็จ", style = MaterialTheme.typography.headlineLarge)
        Text(
            "ซื้อ ${receipt.titles.size} เล่ม รวม ${formatBaht(receipt.total)} ผ่าน${receipt.method.label}\n${receipt.titles.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
        Text("หนังสือถูกเพิ่มไว้ใน “เก็บเข้าชั้น” แล้ว", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        PrimaryButton("ไปที่ชั้นหนังสือ", onLibrary, Modifier.fillMaxWidth())
        SecondaryButton("เลือกซื้อต่อ", onShopMore, Modifier.fillMaxWidth())
    }
}
