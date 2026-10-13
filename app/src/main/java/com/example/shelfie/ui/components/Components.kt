@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shelfie.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shelfie.data.model.Book
import com.example.shelfie.data.model.ReadingStatus
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.Line
import com.example.shelfie.ui.theme.Maroon
import com.example.shelfie.ui.theme.Muted
import com.example.shelfie.ui.theme.TitleSerif
import com.example.shelfie.ui.theme.colors

private val coverPalette = listOf(
    Color(0xFFB5652B) to Color(0xFFFFF6EC),
    Color(0xFF2F3E4E) to Color(0xFFEEF3F7),
    Color(0xFF9E2B25) to Color(0xFFFFF1EE),
    Color(0xFF5D6B3A) to Color(0xFFF8F6EC),
    Color(0xFF3B3F7A) to Color(0xFFF1F0FF),
    Color(0xFF6B2D3C) to Color(0xFFFBEFF1),
    Color(0xFFE9C7C3) to Color(0xFF4A1F24),
    Color(0xFF33473A) to Color(0xFFEEF4EE),
)

/**
 * Book cover: the real cover from the Covers API, drawn on top of a typographic
 * cover. If a book has no cover (or the image fails) the typographic one shows.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 18.sp,
    corner: Dp = 8.dp,
    elevation: Dp = 6.dp,
) {
    val (bg, ink) = coverPalette[Math.floorMod(book.key.hashCode(), coverPalette.size)]
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = book.title,
                color = ink,
                fontFamily = TitleSerif,
                fontSize = titleSize,
                lineHeight = titleSize * 1.05f,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.authors.uppercase(),
                color = ink,
                fontSize = (titleSize.value * 0.38f).coerceAtLeast(7f).sp,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        book.coverUrl('M')?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Three tilted covers, like the reference design's "discover" card stack. */
@Composable
fun CoverFan(front: Book?, modifier: Modifier = Modifier, width: Dp = 150.dp) {
    val height = width * 1.42f
    Box(modifier = modifier.height(height + 28.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width * 0.92f, height * 0.92f)
                .offset(x = -(width * 0.3f))
                .rotate(-9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF4C9C4))
        )
        Box(
            Modifier
                .size(width * 0.92f, height * 0.92f)
                .offset(x = width * 0.3f)
                .rotate(9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFC4404E))
        )
        if (front != null) {
            BookCover(front, Modifier.size(width, height), titleSize = 26.sp, corner = 10.dp, elevation = 14.dp)
        }
    }
}

@Composable
fun StatusPill(status: ReadingStatus, modifier: Modifier = Modifier) {
    val c = status.colors()
    Surface(shape = CircleShape, color = c.background, modifier = modifier) {
        Text(
            text = status.label,
            color = c.content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun SoftPill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = background, modifier = modifier) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Black pill button (main action), like "Next" in the reference design. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = Ink,
    small: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (contentColor == Ink) Ink else contentColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor, containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = if (small) 14.dp else 20.dp),
        modifier = modifier.heightIn(min = if (small) 40.dp else 52.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
        }
        Text(text, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
    }
}

/** Round icon button with an optional red count badge (cart). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    background: Color = Color.White,
    tint: Color = Ink,
) {
    Box(modifier) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            border = if (background == Color.White) BorderStroke(1.dp, Line) else null,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        if (badge > 0) {
            Surface(
                shape = CircleShape,
                color = Maroon,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .heightIn(min = 20.dp),
            ) {
                Text(
                    text = badge.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(92.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Maroon, modifier = Modifier.size(40.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun ProgressLine(progress: Int, modifier: Modifier = Modifier, track: Color = Color(0xFFF1E4DC), bar: Color = Maroon) {
    Box(
        modifier
            .height(5.dp)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0, 100) / 100f)
                .height(5.dp)
                .clip(CircleShape)
                .background(bar)
        )
    }
}

@Composable
fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
