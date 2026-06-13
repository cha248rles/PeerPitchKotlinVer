package com.example.peerpitchkotlinver.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerpitchkotlinver.ui.theme.PitchBlack
import com.example.peerpitchkotlinver.ui.theme.PitchBlue
import com.example.peerpitchkotlinver.ui.theme.PitchGold

@Composable
fun PeerPitchLogo(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Peer",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Cursive
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProjectorBoardIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pitch",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive
            )
        }
    }
}

@Composable
fun ProjectorBoardIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, h * 0.05f),
            size = Size(w, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(width = strokeWidth)
        )
        drawLine(tint, Offset(w * 0.18f, h * 0.22f), Offset(w * 0.82f, h * 0.22f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.4f), Offset(w * 0.6f, h * 0.4f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.6f), Offset(w * 0.5f, h * 0.78f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.22f, h), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.78f, h), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun PresentationIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // whiteboard with bar chart
            drawRoundRect(Color(0xFFF2F2F2), Offset(w * 0.08f, h * 0.05f), Size(w * 0.5f, h * 0.55f), CornerRadius(12f))
            drawRoundRect(Color(0xFFB0B0B0), Offset(w * 0.08f, h * 0.05f), Size(w * 0.5f, h * 0.55f), CornerRadius(12f), style = Stroke(width = 3f))
            drawRect(Color(0xFFE57373), Offset(w * 0.15f, h * 0.36f), Size(w * 0.07f, h * 0.2f))
            drawRect(Color(0xFF4DB6AC), Offset(w * 0.26f, h * 0.26f), Size(w * 0.07f, h * 0.3f))
            drawRect(Color(0xFFFFB74D), Offset(w * 0.37f, h * 0.16f), Size(w * 0.07f, h * 0.4f))
            // pie chart
            drawCircle(Color(0xFFE57373), radius = w * 0.07f, center = Offset(w * 0.74f, h * 0.2f))
            drawArc(Color(0xFF37474F), -90f, 120f, true, topLeft = Offset(w * 0.67f, h * 0.2f - w * 0.07f), size = Size(w * 0.14f, w * 0.14f))
            // speech bubble
            drawRoundRect(Color(0xFFECEFF1), Offset(w * 0.64f, h * 0.4f), Size(w * 0.24f, h * 0.16f), CornerRadius(12f))
            // audience
            val people = listOf(
                Triple(w * 0.2f, Color(0xFF37474F), Color(0xFFE57373)),
                Triple(w * 0.5f, Color(0xFF6D4C41), Color(0xFF4DB6AC)),
                Triple(w * 0.8f, Color(0xFF263238), Color(0xFFFFB74D))
            )
            people.forEach { (x, head, body) ->
                drawCircle(head, radius = w * 0.05f, center = Offset(x, h * 0.72f))
                drawRoundRect(body, Offset(x - w * 0.08f, h * 0.8f), Size(w * 0.16f, h * 0.2f), CornerRadius(w * 0.06f))
            }
        }
    }
}

@Composable
fun PitchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9E9E9E), fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(50),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = PitchBlack,
            focusedTextColor = PitchBlack,
            unfocusedTextColor = PitchBlack
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    contentColor: Color = PitchBlack
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        border = BorderStroke(2.dp, PitchBlue),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun NextButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = PitchBlack,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp)
    ) {
        Text("Next", fontSize = 16.sp, fontFamily = FontFamily.Serif)
    }
}

@Composable
fun BackArrowIcon(onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(
        modifier = modifier
            .size(28.dp)
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.1f
        drawLine(tint, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.45f, h * 0.22f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.45f, h * 0.78f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun MenuIcon(modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit = {}) {
    Canvas(
        modifier = modifier
            .size(28.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.1f
        drawLine(tint, Offset(w * 0.1f, h * 0.28f), Offset(w * 0.9f, h * 0.28f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.1f, h * 0.5f), Offset(w * 0.9f, h * 0.5f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.1f, h * 0.72f), Offset(w * 0.9f, h * 0.72f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun PitchTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        BackArrowIcon(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        MenuIcon(modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun LoginLinkRow(prefix: String, link: String, onLinkClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(prefix, color = PitchBlack, fontSize = 12.sp, fontFamily = FontFamily.Serif)
        Text(
            text = link,
            color = PitchBlack,
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onLinkClick)
        )
    }
}

/**
 * Circular overall-score badge with a progress ring drawn around the number.
 * [score] is expected on a 0..100 scale.
 */
@Composable
fun ScoreBadge(score: Int, modifier: Modifier = Modifier, label: String = "Overall Score") {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            val fraction = (score.coerceIn(0, 100)) / 100f
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.09f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text("/ 100", color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Serif)
    }
}

/**
 * Compact white tile showing a single metric: a bold [value] under a [title],
 * with an optional [detail] caption. Designed to sit in a Row of equal-weight tiles.
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    valueColor: Color = PitchBlue
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color(0xFF6B6B6B), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.size(6.dp))
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (detail != null) {
            Spacer(modifier = Modifier.size(4.dp))
            Text(detail, color = Color(0xFF9E9E9E), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Generic white card with a [title] header followed by arbitrary [content].
 * Used for the transcription and suggestions sections.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, color = PitchBlack, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

/** A single "•"-prefixed line, used for lists of suggestions inside a [SectionCard]. */
@Composable
fun BulletLine(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("•  ", color = PitchGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text, color = Color(0xFF333333), fontSize = 13.sp)
    }
}
