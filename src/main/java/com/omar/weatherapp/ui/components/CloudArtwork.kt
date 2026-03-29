package com.omar.weatherapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp

/**
 * 3D cloud artwork rendered entirely in Compose Canvas —
 * visually matches the screenshot: dark puffy cloud with
 * a red sun peeking below.
 */
@Composable
fun CloudArtwork(
    conditionCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    // Gentle floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Canvas(modifier = modifier.size(300.dp, 240.dp)) {
        val w = size.width
        val h = size.height
        val offsetY = floatOffset

        when {
            conditionCode == 0 && isDay  -> drawSunOnly(w, h, offsetY)
            conditionCode == 0 && !isDay -> drawMoonOnly(w, h, offsetY)
            conditionCode in 1..2        -> drawPartlyCloud(w, h, offsetY, isDay)
            conditionCode in 71..77      -> drawSnowCloud(w, h, offsetY)
            conditionCode in 61..67
                || conditionCode in 80..82 -> drawRainCloud(w, h, offsetY)
            conditionCode in 95..99      -> drawStormCloud(w, h, offsetY)
            else                         -> drawOvercastCloud(w, h, offsetY, isDay)
        }
    }
}

// ── CLEAR DAY: large radiant sun ──────────────────────────────────────────
private fun DrawScope.drawSunOnly(w: Float, h: Float, offY: Float) {
    val cx = w * 0.5f
    val cy = h * 0.5f + offY
    val r  = w * 0.28f

    // Glow
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(60f, 0f, 0f, android.graphics.Color.argb(80, 255, 200, 50))
            }
        }
        canvas.drawCircle(Offset(cx, cy), r + 20f, glowPaint)
    }

    // Sun rays
    val rayColor = Color(0xFFFFCC44)
    for (i in 0 until 12) {
        val angle = Math.toRadians(i * 30.0).toFloat()
        val start = Offset(cx + (r + 10f) * kotlin.math.cos(angle), cy + (r + 10f) * kotlin.math.sin(angle))
        val end   = Offset(cx + (r + 32f) * kotlin.math.cos(angle), cy + (r + 32f) * kotlin.math.sin(angle))
        drawLine(rayColor, start, end, strokeWidth = 4f, cap = StrokeCap.Round)
    }

    // Main sun
    drawCircle(Color(0xFFFFD040), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0xFFFFF8DC), radius = r * 0.5f, center = Offset(cx - r * 0.15f, cy - r * 0.15f))
}

// ── CLEAR NIGHT: crescent moon ────────────────────────────────────────────
private fun DrawScope.drawMoonOnly(w: Float, h: Float, offY: Float) {
    val cx = w * 0.5f
    val cy = h * 0.5f + offY
    val r  = w * 0.22f

    drawCircle(Color(0xFFE8DFC0), radius = r, center = Offset(cx, cy))
    // Cut crescent by covering with background color
    drawCircle(Color(0xFFF0EDE8), radius = r * 0.82f, center = Offset(cx + r * 0.38f, cy - r * 0.18f))

    // Stars
    val stars = listOf(
        Offset(w * 0.15f, h * 0.15f) to 5f,
        Offset(w * 0.80f, h * 0.20f) to 4f,
        Offset(w * 0.70f, h * 0.65f) to 3f,
        Offset(w * 0.25f, h * 0.70f) to 3.5f
    )
    stars.forEach { (pos, size) ->
        drawCircle(Color(0xFFD4CFA8), radius = size, center = pos)
    }
}

// ── PARTLY CLOUDY: sun behind a light cloud ───────────────────────────────
private fun DrawScope.drawPartlyCloud(w: Float, h: Float, offY: Float, isDay: Boolean) {
    // Background sun/moon
    val sunCx = w * 0.68f
    val sunCy = h * 0.35f + offY * 0.4f
    if (isDay) {
        drawCircle(Color(0xFFFFD040), radius = w * 0.18f, center = Offset(sunCx, sunCy))
    } else {
        drawCircle(Color(0xFFE8DFC0), radius = w * 0.16f, center = Offset(sunCx, sunCy))
    }
    // Partial cloud covering sun
    drawCloudShape(w, h, offY, Color(0xFF2A2A2A), scaleX = 0.72f, scaleY = 0.72f)
}

// ── OVERCAST: signature dark cloud + red sun ──────────────────────────────
private fun DrawScope.drawOvercastCloud(w: Float, h: Float, offY: Float, isDay: Boolean) {
    // Red sun / moon glowing below cloud
    val sunColor = if (isDay) Color(0xFFE8372A) else Color(0xFFB03020)
    drawCircle(
        color = sunColor,
        radius = w * 0.27f,
        center = Offset(w * 0.5f, h * 0.72f + offY)
    )

    drawCloudShape(w, h, offY, Color(0xFF1C1C1C))
}

// ── RAIN CLOUD ─────────────────────────────────────────────────────────────
private fun DrawScope.drawRainCloud(w: Float, h: Float, offY: Float) {
    drawCloudShape(w, h, offY, Color(0xFF222222))

    // Rain drops
    val rainColor = Color(0xFF4488CC)
    val drops = listOf(
        Offset(w * 0.28f, h * 0.72f + offY) to Offset(w * 0.22f, h * 0.85f + offY),
        Offset(w * 0.42f, h * 0.75f + offY) to Offset(w * 0.36f, h * 0.88f + offY),
        Offset(w * 0.56f, h * 0.72f + offY) to Offset(w * 0.50f, h * 0.85f + offY),
        Offset(w * 0.70f, h * 0.75f + offY) to Offset(w * 0.64f, h * 0.88f + offY),
        Offset(w * 0.35f, h * 0.82f + offY) to Offset(w * 0.29f, h * 0.95f + offY),
        Offset(w * 0.63f, h * 0.82f + offY) to Offset(w * 0.57f, h * 0.95f + offY),
    )
    drops.forEach { (start, end) ->
        drawLine(rainColor, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
    }
}

// ── SNOW CLOUD ─────────────────────────────────────────────────────────────
private fun DrawScope.drawSnowCloud(w: Float, h: Float, offY: Float) {
    drawCloudShape(w, h, offY, Color(0xFF2E2E2E))

    // Snowflakes
    val snowColor = Color(0xFFAAD8F0)
    val flakes = listOf(
        Offset(w * 0.30f, h * 0.78f + offY),
        Offset(w * 0.50f, h * 0.82f + offY),
        Offset(w * 0.70f, h * 0.78f + offY),
        Offset(w * 0.40f, h * 0.92f + offY),
        Offset(w * 0.60f, h * 0.92f + offY),
    )
    flakes.forEach { center ->
        for (angle in listOf(0f, 60f, 120f)) {
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val dx = 14f * kotlin.math.cos(rad)
            val dy = 14f * kotlin.math.sin(rad)
            drawLine(snowColor, center - Offset(dx, dy), center + Offset(dx, dy), strokeWidth = 4f, cap = StrokeCap.Round)
        }
        drawCircle(snowColor, radius = 5f, center = center)
    }
}

// ── STORM CLOUD ────────────────────────────────────────────────────────────
private fun DrawScope.drawStormCloud(w: Float, h: Float, offY: Float) {
    drawCloudShape(w, h, offY, Color(0xFF111111))

    // Lightning bolt
    val boltColor = Color(0xFFFFEE44)
    val boltPath = Path().apply {
        moveTo(w * 0.54f, h * 0.63f + offY)
        lineTo(w * 0.44f, h * 0.77f + offY)
        lineTo(w * 0.52f, h * 0.77f + offY)
        lineTo(w * 0.42f, h * 0.93f + offY)
        lineTo(w * 0.60f, h * 0.75f + offY)
        lineTo(w * 0.52f, h * 0.75f + offY)
        close()
    }
    drawPath(boltPath, Color(0xFFFFEE44))
    drawPath(boltPath, Color(0xFFFFFFAA), style = Stroke(width = 2f))
}

// ── Core cloud shape (shared) ─────────────────────────────────────────────
private fun DrawScope.drawCloudShape(
    w: Float, h: Float, offY: Float,
    cloudColor: Color,
    scaleX: Float = 1f, scaleY: Float = 1f
) {
    val sX = scaleX
    val sY = scaleY
    val baseY = h * 0.35f + offY

    // Bottom puffs
    drawCircle(cloudColor, radius = w * 0.165f * sX, center = Offset(w * 0.22f, h * 0.54f * sY + baseY * (1 - sY)))
    drawCircle(cloudColor, radius = w * 0.190f * sX, center = Offset(w * 0.46f, h * 0.52f * sY + baseY * (1 - sY)))
    drawCircle(cloudColor, radius = w * 0.165f * sX, center = Offset(w * 0.70f, h * 0.54f * sY + baseY * (1 - sY)))

    // Top puffs
    drawCircle(cloudColor, radius = w * 0.130f * sX, center = Offset(w * 0.34f, h * 0.40f * sY + baseY * (1 - sY)))
    drawCircle(cloudColor, radius = w * 0.155f * sX, center = Offset(w * 0.56f, h * 0.35f * sY + baseY * (1 - sY)))
    drawCircle(cloudColor, radius = w * 0.115f * sX, center = Offset(w * 0.72f, h * 0.42f * sY + baseY * (1 - sY)))

    // Fill body — refine to avoid "black bar" edge artifacts
    val bodyTop    = h * 0.44f * sY + baseY * (1 - sY)
    val bodyBottom = h * 0.58f * sY + baseY * (1 - sY)
    drawRect(
        cloudColor,
        topLeft = Offset(w * 0.12f, bodyTop),
        size = Size(w * 0.76f, bodyBottom - bodyTop)
    )
}
