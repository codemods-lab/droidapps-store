package com.innovcrest.utildroid.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Scalloped "cookie" outline: a circle whose radius dips [depth] (0..1) between [lobes] bumps.
 * [rotationDeg] turns the first bump away from 3 o'clock.
 */
class CookieShape(
    private val lobes: Int,
    private val depth: Float,
    private val rotationDeg: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val steps = lobes * 16
        val rotation = rotationDeg * PI / 180.0
        for (i in 0 until steps) {
            val angle = 2.0 * PI * i / steps
            val scale = 1.0 - depth / 2.0 + depth / 2.0 * cos(lobes * (angle - rotation))
            val x = (cx + cx * scale * cos(angle)).toFloat()
            val y = (cy + cy * scale * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

object StoreShapes {
    /** Store logo. */
    val Cookie6: Shape = CookieShape(lobes = 6, depth = 0.165f)
    val Cookie8: Shape = CookieShape(lobes = 8, depth = 0.095f)
    val Cookie9: Shape = CookieShape(lobes = 9, depth = 0.12f)
    val Clover: Shape = CookieShape(lobes = 4, depth = 0.275f, rotationDeg = 45f)

    /** Shapes handed out to app tiles that have no icon. */
    val Tiles: List<Shape> = listOf(Cookie9, Clover, Cookie8, Cookie6)

    val Card: Shape = RoundedCornerShape(28.dp)
}

private val GroupOuter = 24.dp
private val GroupInner = 6.dp

/** Corner shape for row [index] of a [count]-row group: big corners outside, tight ones between rows. */
fun groupedShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupOuter else GroupInner
    val bottom = if (index == count - 1) GroupOuter else GroupInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

/** Gap between rows of a group. */
val GroupGap = 2.dp
