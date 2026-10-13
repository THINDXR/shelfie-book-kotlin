@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfie.data.auth.AuthErrorKind
import com.example.shelfie.data.auth.AuthResult
import com.example.shelfie.data.auth.LocalAuthRepository
import com.example.shelfie.data.model.Book
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.components.CoverFan
import com.example.shelfie.ui.components.PrimaryButton
import com.example.shelfie.ui.theme.ErrorBg
import com.example.shelfie.ui.theme.ErrorInk
import com.example.shelfie.ui.theme.FieldLine
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.PeachGradient
import com.example.shelfie.ui.theme.SoftSurface
import com.example.shelfie.ui.theme.ThaiSerif
import com.example.shelfie.ui.theme.TitleSerif
import kotlinx.coroutines.launch

// Shown on the cover stack (Pride and Prejudice, Open Library work OL66554W).
private val heroBook = Book("OL66554W", "Pride and Prejudice", "Jane Austen", 1813, null)

@Composable
fun AuthScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<AuthResult.Failure?>(null) }

    fun submit() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            val result = if (isRegister) vm.register(name, email, password, confirm) else vm.login(email, password)
            loading = false
            if (result is AuthResult.Failure) error = result
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PeachGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Shelfie", fontFamily = TitleSerif, fontSize = 48.sp, color = Ink)
            Text(
                "ค้นพบเรื่องราวที่อยู่กับคุณไปอีกนาน",
                fontFamily = ThaiSerif,
                fontSize = 17.sp,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
            CoverFan(front = heroBook, width = 116.dp)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModeSwitch(isRegister = isRegister) {
                        isRegister = it
                        error = null
                    }

                    if (isRegister) {
                        AuthField("ชื่อที่แสดง", name, { name = it }, "เช่น มายด์")
                    }
                    AuthField(
                        "อีเมล", email, { email = it; error = null }, "you@example.com",
                        keyboardType = KeyboardType.Email,
                    )
                    AuthField(
                        "รหัสผ่าน", password, { password = it }, "อย่างน้อย 6 ตัวอักษร",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        imeAction = if (isRegister) ImeAction.Next else ImeAction.Done,
                        onDone = { submit() },
                    )
                    if (isRegister) {
                        AuthField(
                            "ยืนยันรหัสผ่าน", confirm, { confirm = it }, "พิมพ์รหัสผ่านอีกครั้ง",
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            showPassword = showPassword,
                            imeAction = ImeAction.Done,
                            onDone = { submit() },
                        )
                    }

                    error?.let { failure ->
                        ErrorBox(
                            failure = failure,
                            onSwitch = {
                                isRegister = failure.kind == AuthErrorKind.NOT_REGISTERED
                                error = null
                            },
                        )
                    }

                    PrimaryButton(
                        text = if (isRegister) "สมัครสมาชิก" else "เข้าสู่ระบบ",
                        onClick = { submit() },
                        loading = loading,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!vm.usesCloudAuth && !isRegister) {
                        OutlinedButton(
                            onClick = {
                                email = LocalAuthRepository.DEMO_EMAIL
                                password = "123456"
                                error = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, FieldLine),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "ใช้บัญชีทดลอง ${LocalAuthRepository.DEMO_EMAIL} / 123456",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                            )
                        }
                    }
                }
            }

            Text(
                if (vm.usesCloudAuth) "บัญชีเก็บอย่างปลอดภัยด้วย Firebase Authentication"
                else "โหมดทดลอง: บัญชีถูกเก็บไว้ในเครื่องนี้ (เพิ่ม google-services.json เพื่อใช้ Firebase)",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModeSwitch(isRegister: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SoftSurface, CircleShape)
            .padding(4.dp)
    ) {
        listOf(false to "เข้าสู่ระบบ", true to "สมัครสมาชิก").forEach { (register, label) ->
            val selected = register == isRegister
            Surface(
                onClick = { onChange(register) },
                shape = CircleShape,
                color = if (selected) Ink else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Muted) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPassword) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน",
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = FieldLine,
            focusedLabelColor = Ink,
            cursorColor = Ink,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorBox(failure: AuthResult.Failure, onSwitch: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = ErrorBg, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorInk, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(failure.message, color = ErrorInk, style = MaterialTheme.typography.bodyMedium)
                val action = when (failure.kind) {
                    AuthErrorKind.NOT_REGISTERED -> "สมัครสมาชิกด้วยอีเมลนี้"
                    AuthErrorKind.ALREADY_REGISTERED -> "ไปหน้าเข้าสู่ระบบ"
                    else -> null
                }
                if (action != null) {
                    TextButton(onClick = onSwitch) {
                        Text(action, color = ErrorInk, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
