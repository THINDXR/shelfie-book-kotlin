@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfie.data.model.FontSizeOption
import com.example.shelfie.data.model.ReaderTheme
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.components.SecondaryButton
import com.example.shelfie.ui.theme.BlushSoft
import com.example.shelfie.ui.theme.ErrorInk
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
import com.example.shelfie.ui.theme.backgroundColor
import com.example.shelfie.ui.theme.contentColor

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenLibrary: (ReadingStatus) -> Unit,
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()

    var editing by rememberSaveable { mutableStateOf(false) }
    var editName by rememberSaveable { mutableStateOf("") }
    var editBio by rememberSaveable { mutableStateOf("") }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PeachGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("โปรไฟล์", style = MaterialTheme.typography.headlineLarge)

        // ---------- Profile card / edit form ----------
        if (!editing) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(92.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Maroon, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(profile.name.take(1).uppercase().ifEmpty { "?" }, color = Color.White, fontFamily = ThaiSerif, fontSize = 38.sp)
                }
                Text(profile.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
                Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                Text(
                    profile.bio.ifBlank { "ยังไม่ได้เขียนแนะนำตัว" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                SecondaryButton(
                    text = "แก้ไขโปรไฟล์",
                    onClick = {
                        editName = profile.name
                        editBio = profile.bio
                        editing = true
                    },
                    icon = Icons.Outlined.Edit,
                    small = true,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        } else {
            Card {
                Text("แก้ไขโปรไฟล์", style = MaterialTheme.typography.headlineSmall)
                ProfileField("ชื่อที่แสดง", editName, { editName = it }, singleLine = true)
                ProfileField("แนะนำตัวสั้น ๆ", editBio, { editBio = it }, singleLine = false)
                Text("อีเมล: ${user?.email.orEmpty()} (ใช้เข้าสู่ระบบ เปลี่ยนไม่ได้)", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("ยกเลิก", { editing = false }, Modifier.weight(1f))
                    PrimaryButton(
                        "บันทึก",
                        {
                            vm.updateProfile(editName, editBio)
                            if (editName.isNotBlank()) editing = false
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }

        // ---------- Reading stats (shortcuts into the shelf) ----------
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(library.values.count { it.status == ReadingStatus.READING }, "กำลังอ่าน", Modifier.weight(1f)) {
                onOpenLibrary(ReadingStatus.READING)
            }
            StatTile(library.values.count { it.status == ReadingStatus.DONE }, "อ่านจบแล้ว", Modifier.weight(1f)) {
                onOpenLibrary(ReadingStatus.DONE)
            }
            StatTile(library.values.count { it.owned }, "ซื้อแล้ว", Modifier.weight(1f)) {
                onOpenLibrary(ReadingStatus.SHELF)
            }
        }

        // ---------- Font size ----------
        Card {
            SectionHeader(Icons.Outlined.TextFields, "ขนาดตัวอักษร")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SoftSurface, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FontSizeOption.entries.forEachIndexed { index, option ->
                    val selected = option == settings.fontSize
                    Surface(
                        onClick = { vm.setFontSize(option) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (selected) Color.White else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("Aa", fontFamily = TitleSerif, fontSize = (15 + index * 3).sp, color = if (selected) Maroon else InkMuted)
                            Text(option.label, style = MaterialTheme.typography.labelSmall, color = if (selected) Maroon else InkMuted)
                        }
                    }
                }
            }
            Text(
                "ขนาดนี้ใช้กับทุกหน้าในแอป รวมถึงหน้าอ่าน e-book",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }

        // ---------- Reader theme ----------
        Card {
            SectionHeader(Icons.Outlined.Palette, "ธีมหน้าอ่าน")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReaderTheme.entries.forEach { theme ->
                    ThemeSwatch(theme, selected = theme == settings.readerTheme) { vm.setReaderTheme(theme) }
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = settings.readerTheme.backgroundColor,
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "ตัวอย่าง: หน้าอ่านจะใช้ธีม “${settings.readerTheme.label}” และตัวอักษรขนาด “${settings.fontSize.label}”",
                    color = settings.readerTheme.contentColor,
                    fontFamily = ThaiSerif,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // ---------- Other settings ----------
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Line)) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Ink)
                    Text(
                        "แจ้งเตือนให้อ่านต่อทุกวัน",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Switch(
                        checked = settings.dailyReminder,
                        onCheckedChange = vm::setDailyReminder,
                        colors = SwitchDefaults.colors(checkedTrackColor = Maroon),
                    )
                }
                HorizontalDivider(color = Line, modifier = Modifier.padding(horizontal = 18.dp))
                TextButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = ErrorInk)
                    Text("  ออกจากระบบ", color = ErrorInk, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                }
            }
        }

        Text(
            if (vm.usesCloudAuth) "บัญชีนี้ใช้ Firebase Authentication" else "บัญชีนี้เก็บไว้ในเครื่อง (โหมดทดลอง)",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("ออกจากระบบ?") },
            text = { Text("ตะกร้าและชั้นหนังสือจะยังอยู่ เมื่อเข้าสู่ระบบด้วยบัญชีเดิมอีกครั้ง") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    vm.logout()
                }) { Text("ออกจากระบบ", color = ErrorInk) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("ยกเลิก", color = Ink) }
            },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Maroon, modifier = Modifier.size(20.dp))
        Text("  $title", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatTile(count: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontFamily = TitleSerif, fontSize = 28.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ReaderTheme, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = theme.backgroundColor,
        border = BorderStroke(if (selected) 3.dp else 1.5.dp, if (selected) Ink else FieldLine),
        modifier = Modifier
            .size(50.dp)
            .background(if (selected) BlushSoft else Color.Transparent, CircleShape)
            .padding(if (selected) 3.dp else 0.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "ก",
                color = theme.contentColor,
                fontFamily = ThaiSerif,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = FieldLine,
            focusedLabelColor = Ink,
            cursorColor = Ink,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
