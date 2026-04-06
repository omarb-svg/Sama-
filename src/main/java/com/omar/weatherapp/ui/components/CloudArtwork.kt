package com.omar.weatherapp.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kinetic cloud artwork.
 * - sunProgress: 0f = sunrise, 0.5f = solar noon, 1f = sunset, <0 = night
 * - Crossfades smoothly between condition states
 * - Animated rain drops / snowflakes / lightning flash
 */
@Composable
fun CloudArtwork(
    conditionCode: Int,
    isDay: Boolean,
    sunProgress: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_anim")

    // ── Gentle cloud float ────────────────────────────────────────────────
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            tween(3600, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "float"
    )

    // ── Rain drop progress (12 lanes, staggered) ─────────────────────────
    val rainDrops = (0 until 12).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(520 + i * 40, easing = LinearEasing, delayMillis = (i * 60) % 400),
                RepeatMode.Restart
            ), label = "rain_$i"
        ).value
    }

    // ── Snowflake drift (6 flakes) ────────────────────────────────────────
    val snowFlakes = (0 until 6).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(1400 + i * 100, easing = LinearEasing, delayMillis = i * 230),
                RepeatMode.Restart
            ), label = "snow_$i"
        ).value
    }

    // ── Lightning flash ───────────────────────────────────────────────────
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4500
                0f at 0
                0f at 3000
                1f at 3080
                0f at 3160
                0.6f at 3240
                0f at 3320
            }, RepeatMode.Restart
        ), label = "lightning"
    )

    // ── Smooth sun/moon position ──────────────────────────────────────────
    val animSunProgress by animateFloatAsState(
        targetValue = sunProgress,
        animationSpec = tween(2000, easing = EaseInOutSine),
        label = "sun_arc"
    )

    // ── Crossfade on condition code → smooth transition ───────────────────
    Crossfade(
        targetState = conditionCode,
        animationSpec = tween(700),
        modifier = modifier,
        label = "condition_crossfade"
    ) { code ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val offY = floatOffset

            when {
                code == 0 && isDay   -> drawSunScene(w, h, offY, animSunProgress)
                code == 0 && !isDay  -> drawNightScene(w, h, offY)
                code in 1..2         -> drawPartlyCloud(w, h, offY, isDay, animSunProgress)
                code in 71..77       -> drawSnowCloud(w, h, offY, snowFlakes)
                code in 61..67
                    || code in 80..82 -> drawRainCloud(w, h, offY, rainDrops)
                code in 95..99       -> drawStormCloud(w, h, offY, rainDrops, lightningAlpha)
                else                 -> drawOvercastCloud(w, h, offY, isDay, animSunProgress)
            }
        }
    }
}

// ── Sun arc position helper ───────────────────────────────────────────────────
private fun sunArcPos(progress: Float, w: Float, h: Float): Offset {
    // progress 0→1 traces a semicircle: left-bottom → top-center → right-bottom
    val clipped = progress.coerceIn(0f, 1f)
    val angle   = clipped * Math.PI.toFloat()
    val x = w * 0.12f + clipped * (w * 0.76f)
    val y = h * 0.88f - sin(angle) * (h * 0.78f)
    return Offset(x, y)
}

// ── CLEAR DAY ─────────────────────────────────────────────────────────────────
private fun DrawScope.drawSunScene(w: Float, h: Float, offY: Float, sunProgress: Float) {
    val pos = sunArcPos(sunProgress, w, h)
    val cx  = pos.x
    val cy  = (pos.y + offY * 0.25f).coerceIn(h * 0.08f, h * 0.78f)
    val r   = w * 0.24f

    // Outer glow
    drawCircle(Color(0xFFFFD040).copy(alpha = 0.22f), radius = r * 1.9f, center = Offset(cx, cy))
    drawCircle(Color(0xFFFFD040).copy(alpha = 0.14f), radius = r * 2.4f, center = Offset(cx, cy))

    // Rays
    for (i in 0 until 12) {
        val a = Math.toRadians(i * 30.0).toFloat()
        drawLine(
            Color(0xFFFFCC44),
            Offset(cx + (r + 8f) * cos(a), cy + (r + 8f) * sin(a)),
            Offset(cx + (r + 28f) * cos(a), cy + (r + 28f) * sin(a)),
            strokeWidth = 3.5f, cap = StrokeCap.Round
        )
    }

    // Sun body
    drawCircle(Color(0xFFFFD040), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0xFFFFF5C0), radius = r * 0.42f, center = Offset(cx - r * 0.22f, cy - r * 0.22f))
}

// ── CLEAR NIGHT ───────────────────────────────────────────────────────────────
private fun DrawScope.drawNightScene(w: Float, h: Float, offY: Float) {
    val cx = w * 0.52f
    val cy = h * 0.42f + offY
    val r  = w * 0.20f

    drawCircle(Color(0xFFE8DFC0), radius = r, center = Offset(cx, cy))
    // Crescent cut
    drawCircle(Color(0xFFF0EDE8), radius = r * 0.80f, center = Offset(cx + r * 0.38f, cy - r * 0.18f))

    // Stars scattered
    listOf(
        Offset(w * 0.12f, h * 0.12f) to 5f,
        Offset(w * 0.82f, h * 0.18f) to 4f,
        Offset(w * 0.22f, h * 0.68f) to 3.5f,
        Offset(w * 0.75f, h * 0.62f) to 3f,
        Offset(w * 0.48f, h * 0.15f) to 2.5f,
        Offset(w * 0.90f, h * 0.42f) to 2.5f,
    ).forEach { (p, s) -> drawCircle(Color(0xFFCDC8A2), radius = s, center = p) }
}

// ── PARTLY CLOUDY ────────────────────────────────────────────────────────────
private fun DrawScope.drawPartlyCloud(
    w: Float, h: Float, offY: Float, isDay: Boolean, sunProgress: Float
) {
    val pos = sunArcPos(sunProgress, w, h)
    val sx  = pos.x.coerceIn(w * 0.35f, w * 0.78f)
    val sy  = (pos.y + offY * 0.25f).coerceIn(h * 0.15f, h * 0.55f)
    val orb = if (isDay) Color(0xFFE8372A) else Color(0xFFD0C0A0)

    drawCircle(orb, radius = w * 0.20f, center = Offset(sx, sy))
    drawCircle(orb.copy(alpha = 0.18f), radius = w * 0.30f, center = Offset(sx, sy))
    drawCloudShape(w, h, offY, Color(0xFF3A3A3A), scale = 0.80f)
}

// ── OVERCAST — pink/red sun behind dark cloud ─────────────────────────────────
private fun DrawScope.drawOvercastCloud(
    w: Float, h: Float, offY: Float, isDay: Boolean, sunProgress: Float
) {
    val pos = sunArcPos(sunProgress, w, h)
    val sx  = pos.x.coerceIn(w * 0.28f, w * 0.78f)
    val sy  = (pos.y + offY * 0.25f).coerceIn(h * 0.12f, h * 0.58f)
    val orb = if (isDay) Color(0xFFDC3020) else Color(0xFFAA2820)

    // Glow halo
    drawCircle(orb.copy(alpha = 0.20f), radius = w * 0.44f, center = Offset(sx, sy))
    // Solid orb
    drawCircle(orb, radius = w * 0.29f, center = Offset(sx, sy))

    // Main cloud mass
    drawCloudShape(w, h, offY, Color(0xFF212121))

    // Floating secondary puffs for depth
    drawCircle(Color(0xFF9A9A9A), radius = w * 0.11f, center = Offset(w * 0.08f, h * 0.60f + offY))
    drawCircle(Color(0xFF888888), radius = w * 0.07f, center = Offset(w * 0.88f, h * 0.64f + offY))
}

// ── RAIN CLOUD — animated streaks ─────────────────────────────────────────────
private fun DrawScope.drawRainCloud(w: Float, h: Float, offY: Float, drops: List<Float>) {
    drawCloudShape(w, h, offY, Color(0xFF1E1E1E))

    val xLanes = listOf(0.18f, 0.26f, 0.34f, 0.42f, 0.50f, 0.58f, 0.66f, 0.74f, 0.22f, 0.46f, 0.62f, 0.78f)
    val cloudBottom = h * 0.58f + offY

    drops.forEachIndexed { i, progress ->
        val x = w * xLanes[i]
        val len = h * 0.30f
        val startY = cloudBottom + progress * len
        val endY   = startY + 13f
        val alpha  = (1f - progress * 1.4f).coerceIn(0f, 1f)
        if (alpha > 0.02f) {
            drawLine(
                Color(0xFF5599CC).copy(alpha = alpha),
                Offset(x, startY), Offset(x - 4f, endY),
                strokeWidth = 4.5f, cap = StrokeCap.Round
            )
        }
    }
}

// ── SNOW CLOUD — drifting flakes ──────────────────────────────────────────────
private fun DrawScope.drawSnowCloud(w: Float, h: Float, offY: Float, flakes: List<Float>) {
    drawCloudShape(w, h, offY, Color(0xFF2A2A2A))

    val xLanes  = listOf(0.22f, 0.38f, 0.54f, 0.70f, 0.30f, 0.62f)
    val cloudBottom = h * 0.58f + offY

    flakes.forEachIndexed { i, progress ->
        val x     = w * xLanes[i] + sin(progress * Math.PI.toFloat() * 2f) * 8f
        val y     = cloudBottom + progress * h * 0.32f
        val alpha = (1f - progress * 1.3f).coerceIn(0f, 1f)
        if (alpha > 0.02f) {
            val snowC = Color(0xFFAAD8F0).copy(alpha = alpha)
            for (angle in listOf(0f, 60f, 120f)) {
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val arm = 10f
                drawLine(snowC, Offset(x - cos(rad) * arm, y - sin(rad) * arm),
                    Offset(x + cos(rad) * arm, y + sin(rad) * arm), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            drawCircle(snowC, radius = 3.5f, center = Offset(x, y))
        }
    }
}

// ── STORM CLOUD — rain + lightning ───────────────────────────────────────────
private fun DrawScope.drawStormCloud(
    w: Float, h: Float, offY: Float, drops: List<Float>, lightAlpha: Float
) {
    drawCloudShape(w, h, offY, Color(0xFF111111))

    // Rain (same as rain cloud but darker)
    val xLanes = listOf(0.18f, 0.26f, 0.34f, 0.42f, 0.50f, 0.58f, 0.66f, 0.74f, 0.22f, 0.46f, 0.62f, 0.78f)
    val cloudBottom = h * 0.58f + offY
    drops.forEachIndexed { i, progress ->
        val x     = w * xLanes[i]
        val startY = cloudBottom + progress * h * 0.28f
        val endY  = startY + 12f
        val alpha = (1f - progress * 1.4f).coerceIn(0f, 1f)
        if (alpha > 0.02f) {
            drawLine(Color(0xFF336699).copy(alpha = alpha * 0.75f),
                Offset(x, startY), Offset(x - 3f, endY), strokeWidth = 4f, cap = StrokeCap.Round)
        }
    }

    // Lightning bolt (flashes via lightAlpha)
    if (lightAlpha > 0.01f) {
        val boltPath = Path().apply {
            moveTo(w * 0.54f, cloudBottom - h * 0.10f)
            lineTo(w * 0.44f, cloudBottom + h * 0.12f)
            lineTo(w * 0.52f, cloudBottom + h * 0.12f)
            lineTo(w * 0.42f, cloudBottom + h * 0.32f)
            lineTo(w * 0.60f, cloudBottom + h * 0.08f)
            lineTo(w * 0.52f, cloudBottom + h * 0.08f)
            close()
        }
        drawPath(boltPath, Color(0xFFFFEE44).copy(alpha = lightAlpha))
        drawPath(boltPath, Color(0xFFFFFFCC).copy(alpha = lightAlpha * 0.6f), style = Stroke(2f))
    }
}

// ── Shared cloud puff shape ───────────────────────────────────────────────────
private fun DrawScope.drawCloudShape(
    w: Float, h: Float, offY: Float,
    cloudColor: Color,
    scale: Float = 1f
) {
    val cx = w * 0.46f
    val cy = h * 0.44f + offY
    val s  = scale

    // Fill rect to close gaps between puffs
    drawRect(
        cloudColor,
        topLeft = Offset(cx - w * 0.38f * s, cy - w * 0.06f * s),
        size    = Size(w * 0.76f * s, w * 0.22f * s)
    )

    // Bottom row
    drawCircle(cloudColor, w * 0.178f * s, Offset(cx - w * 0.26f * s, cy + w * 0.01f * s))
    drawCircle(cloudColor, w * 0.198f * s, Offset(cx,                   cy))
    drawCircle(cloudColor, w * 0.172f * s, Offset(cx + w * 0.27f * s, cy + w * 0.01f * s))

    // Top row
    drawCircle(cloudColor, w * 0.142f * s, Offset(cx - w * 0.14f * s, cy - w * 0.14f * s))
    drawCircle(cloudColor, w * 0.160f * s, Offset(cx + w * 0.10f * s, cy - w * 0.16f * s))
    drawCircle(cloudColor, w * 0.122f * s, Offset(cx + w * 0.30f * s, cy - w * 0.10f * s))

    // Left tail puff
    drawCircle(cloudColor, w * 0.108f * s, Offset(cx - w * 0.36f * s, cy + w * 0.09f * s))
}
