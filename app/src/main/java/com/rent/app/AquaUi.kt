package com.rent.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------------------
// Palette + theme: Mac OS "Aqua" (glossy blue buttons, pinstripes, window title bars)
// ---------------------------------------------------------------------------

object Aqua {
    val BlueDark = Color(0xFF1F6FD0)
    val TextDark = Color(0xFF222222)
    val TextMuted = Color(0xFF666666)
    val Green = Color(0xFF1B8A2F)
    val Red = Color(0xFFC62828)
    val GridLine = Color(0xFFCBCBCB)
    val Stripe = Color(0xFFEFF5FC)
}

enum class AquaTone(
    val c1: Color,
    val c2: Color,
    val c3: Color,
    val c4: Color,
    val border: Color,
    val text: Color,
    val textShadow: Color
) {
    Blue(
        Color(0xFFB4D7FA), Color(0xFF6AAEF0), Color(0xFF2F7FE0), Color(0xFF63B3F5),
        Color(0xFF1F5FB5), Color.White, Color(0x66000000)
    ),
    Red(
        Color(0xFFF9B9B0), Color(0xFFF07468), Color(0xFFD7301F), Color(0xFFF2776A),
        Color(0xFF9E1C10), Color.White, Color(0x66000000)
    ),
    Silver(
        Color(0xFFFFFFFF), Color(0xFFE6E6E6), Color(0xFFC9C9C9), Color(0xFFE9E9E9),
        Color(0xFF8E8E8E), Color(0xFF222222), Color(0xCCFFFFFF)
    ),
    Disabled(
        Color(0xFFF4F4F4), Color(0xFFDEDEDE), Color(0xFFCACACA), Color(0xFFE0E0E0),
        Color(0xFFB0B0B0), Color(0xFF8A8A8A), Color(0xCCFFFFFF)
    )
}

@Composable
fun RentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Aqua.BlueDark,
            onPrimary = Color.White,
            background = Color(0xFFEDEDED),
            surface = Color.White,
            onSurface = Aqua.TextDark
        ),
        content = content
    )
}

fun Modifier.aquaBackground(): Modifier = this
    .background(Color(0xFFEDEDED))
    .drawBehind {
        val period = 4.dp.toPx()
        val stripe = 2.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(Color(0xFFE4E4E4), Offset(x, 0f), Size(stripe, size.height))
            x += period
        }
    }

/** Glossy Aqua surface: split gradient, white highlight on the top half, thin dark border. */
private fun Modifier.glossy(tone: AquaTone, shape: Shape, glossRadius: Dp?): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            0f to tone.c1,
            0.5f to tone.c2,
            0.5f to tone.c3,
            1f to tone.c4
        )
    )
    .drawBehind {
        val inset = 3.dp.toPx()
        val radius = glossRadius?.toPx() ?: size.height
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.10f)),
                startY = 0f,
                endY = size.height * 0.5f
            ),
            topLeft = Offset(inset * 2, inset * 0.6f),
            size = Size(size.width - inset * 4, size.height * 0.46f),
            cornerRadius = CornerRadius(radius)
        )
    }
    .border(BorderStroke(1.dp, tone.border), shape)

@Composable
fun AquaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AquaTone = AquaTone.Blue,
    enabled: Boolean = true,
    height: Dp = 36.dp,
    fontSize: TextUnit = 14.sp
) {
    val t = if (enabled) tone else AquaTone.Disabled
    Box(
        modifier = modifier
            .height(height)
            .glossy(t, RoundedCornerShape(percent = 50), null)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = t.text,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                shadow = Shadow(t.textShadow, Offset(0f, 1.5f), 2f)
            )
        )
    }
}

/** Square, rounded-corner "app icon" style button used on Home. */
@Composable
fun AquaTile(
    label: String,
    sub: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AquaTone = AquaTone.Blue,
    enabled: Boolean = true
) {
    val t = if (enabled) tone else AquaTone.Disabled
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(4.dp, shape)
            .glossy(t, shape, 16.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = t.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    shadow = Shadow(t.textShadow, Offset(0f, 1.5f), 2f)
                )
            )
            if (!sub.isNullOrBlank()) {
                Text(
                    text = sub,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = TextStyle(color = t.text, fontSize = 11.sp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState()
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Window chrome
// ---------------------------------------------------------------------------

@Composable
private fun TrafficLight(color: Color, onClick: (() -> Unit)?) {
    val base = Modifier
        .size(13.dp)
        .clip(CircleShape)
        .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.55f), color)))
        .border(0.5.dp, lerp(color, Color.Black, 0.35f), CircleShape)
    Box(if (onClick != null) base.clickable(onClick = onClick) else base)
}

/** Brushed-metal title bar with the three round window buttons (the red one closes dialogs). */
@Composable
fun AquaTitleBar(title: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFFF3F3F3), Color(0xFFC8C8C8))))
            .drawBehind {
                drawLine(
                    Color(0xFF8C8C8C),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrafficLight(Color(0xFFFF5F57), onClose)
            TrafficLight(Color(0xFFFEBC2E), null)
            TrafficLight(Color(0xFF28C840), null)
        }
        Text(
            text = title,
            style = TextStyle(
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                shadow = Shadow(Color(0xCCFFFFFF), Offset(0f, 1f), 0f)
            )
        )
    }
}

@Composable
fun AquaDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 620.dp)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, Color(0xFF7A7A7A), shape)
        ) {
            AquaTitleBar(title, onClose = onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Inputs + messages
// ---------------------------------------------------------------------------

@Composable
fun AquaField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    lines: Int = 1,
    placeholder: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder) },
        singleLine = lines == 1,
        minLines = lines,
        maxLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Aqua.BlueDark,
            unfocusedBorderColor = Color(0xFF9A9A9A),
            cursorColor = Aqua.BlueDark,
            focusedLabelColor = Aqua.BlueDark
        )
    )
}

data class UiMsg(val text: String, val error: Boolean)

@Composable
fun StatusMessage(msg: UiMsg?) {
    if (msg != null) {
        Text(
            text = msg.text,
            color = if (msg.error) Aqua.Red else Aqua.Green,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
