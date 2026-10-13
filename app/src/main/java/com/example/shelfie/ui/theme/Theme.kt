package com.example.shelfie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.example.shelfie.R
import com.example.shelfie.data.model.ReaderTheme
import com.example.shelfie.data.model.ReadingStatus

// ---------- Colors (warm peach tone from the reference design) ----------
val Cream = Color(0xFFFFF8F3)
val Peach = Color(0xFFFBE1D4)
val Ink = Color(0xFF171412)
val InkMuted = Color(0xFF4E4440)
val Muted = Color(0xFF6B5F5A)
val Line = Color(0xFFEADDD5)
val FieldLine = Color(0xFFD9CBC2)
val SoftSurface = Color(0xFFF7EDE7)
val Maroon = Color(0xFF8C2F39)
val BlushSoft = Color(0xFFF8E1E3)
val ErrorInk = Color(0xFF7E1F1A)
val ErrorBg = Color(0xFFFBE6E4)
val SuccessBg = Color(0xFFDDEDE3)
val SuccessInk = Color(0xFF2F5D46)

val PeachGradient = Brush.verticalGradient(listOf(Peach, Cream), endY = 1100f)

data class StatusColors(val background: Color, val content: Color, val dot: Color)

fun ReadingStatus.colors(): StatusColors = when (this) {
    ReadingStatus.READING -> StatusColors(BlushSoft, Maroon, Maroon)
    ReadingStatus.SHELF -> StatusColors(Color(0xFFF6E9CF), Color(0xFF6A4B12), Color(0xFFB88A2E))
    ReadingStatus.DONE -> StatusColors(SuccessBg, SuccessInk, SuccessInk)
}

val ReaderTheme.backgroundColor: Color get() = Color(background)
val ReaderTheme.contentColor: Color get() = Color(content)

// ---------- Type ----------
/** Latin display serif for book titles (Instrument Serif, OFL). */
val TitleSerif = FontFamily(Font(R.font.instrument_serif))

/** Thai display serif for headings (Noto Serif Thai, OFL). Body text uses the system Thai font. */
val ThaiSerif = FontFamily(Font(R.font.noto_serif_thai))

private val ShelfieTypography = Typography(
    headlineLarge = TextStyle(fontFamily = ThaiSerif, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = ThaiSerif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = ThaiSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp),
)

private val ShelfieColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = Maroon,
    onSecondary = Color.White,
    secondaryContainer = BlushSoft,
    onSecondaryContainer = Maroon,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = InkMuted,
    surfaceContainer = Color.White,
    outline = FieldLine,
    outlineVariant = Line,
    error = ErrorInk,
    errorContainer = ErrorBg,
)

/**
 * [fontScale] comes from Profile > ขนาดตัวอักษร. It multiplies the phone's own
 * font scale, so every text in the app grows or shrinks together.
 */
@Composable
fun ShelfieTheme(fontScale: Float = 1f, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * fontScale)) {
        MaterialTheme(
            colorScheme = ShelfieColors,
            typography = ShelfieTypography,
            content = content,
        )
    }
}
