package com.scenicprints.less.drawer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.scenicprints.less.apps.Index
import com.scenicprints.less.ui.LocalPalette
import com.scenicprints.less.ui.LocalType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The knob: a rotary dial sitting mostly off the right edge, so a third of its rim shows.
 *
 * A machined part, not a drawing of one: a dark recess it sits in, a knurled grip round the edge,
 * a bevel line, a printed scale band with a major tick per letter and a minor tick between, the
 * letters following the rim, and an inner face. A fixed orange index mark outside the rim points
 * at the current letter. It turns as the list scrolls; drag the rim and the list follows.
 *
 * [position] is where the index points, in letters: 2.0 is C, 2.5 is halfway through C's apps.
 */
@Composable
fun Knob(
    position: Float,
    present: Set<Char>,
    modifier: Modifier = Modifier,
    onDrag: (Float) -> Unit,
    onRelease: () -> Unit
) {
    val p = LocalPalette.current
    val type = LocalType.current
    val measurer = rememberTextMeasurer()
    val radius = 200.dp
    val step = 6f
    Canvas(
        modifier.pointerInput(Unit) {
            val r = radius.toPx()
            detectVerticalDragGestures(onDragEnd = onRelease, onDragCancel = onRelease) { change, dy ->
                // Arc length along the rim to degrees, degrees to letters. Down at the left of the
                // rim turns the knob so earlier letters come to the index.
                val degrees = dy / r * (180f / PI.toFloat())
                onDrag(-degrees / step)
                change.consume()
            }
        }
    ) {
        val r = radius.toPx()
        val cx = size.width + 80.dp.toPx()
        val cy = size.height / 2
        val c = Offset(cx, cy)

        drawCircle(p.sunk, r + 8.dp.toPx(), c)
        drawCircle(p.raised, r, c)
        // The knurl: fine ridges every two degrees round the grip.
        var d = 100f
        while (d <= 260f) {
            line(c, r, r - 6.dp.toPx(), d, p.line, 1.dp.toPx())
            d += 2f
        }
        drawCircle(p.ground, r - 7.dp.toPx(), c)
        drawCircle(p.line, r - 7.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawCircle(p.sunk, r - 9.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))

        // The scale band: letters round the rim, each at its own angle, the current one in ink.
        val letters = Index.rail
        val current = position.toInt().coerceIn(0, letters.size - 1)
        for (i in letters.indices) {
            val deg = 180f - (i - position) * step
            if (deg < 96f || deg > 264f) continue
            val major = p.dim
            line(c, r - 14.dp.toPx(), r - 23.dp.toPx(), deg, major, 1.dp.toPx())
            if (i < letters.size - 1) line(c, r - 14.dp.toPx(), r - 18.dp.toPx(), deg - step / 2, p.line, 1.dp.toPx())
            val colour = when {
                i == current -> p.ink
                letters[i] in present -> p.dim
                else -> p.line
            }
            val at = point(c, r - 40.dp.toPx(), deg)
            val layout = measurer.measure(letters[i].toString(), type.tiny.copy(color = colour))
            rotate(deg - 180f, at) {
                drawText(layout, topLeft = Offset(at.x - layout.size.width / 2, at.y - layout.size.height / 2))
            }
        }

        drawCircle(p.raised, r - 54.dp.toPx(), c)
        drawCircle(p.line, r - 54.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawCircle(p.sunk, r - 56.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))

        // The index: fixed outside the rim, pointing in.
        val left = cx - r
        drawLine(p.accent, Offset(left - 22.dp.toPx(), cy), Offset(left - 12.dp.toPx(), cy), 2.dp.toPx(), StrokeCap.Round)
        val tip = Offset(left - 10.dp.toPx(), cy)
        drawPath(
            androidx.compose.ui.graphics.Path().apply {
                moveTo(tip.x - 6.dp.toPx(), tip.y - 4.dp.toPx())
                lineTo(tip.x, tip.y)
                lineTo(tip.x - 6.dp.toPx(), tip.y + 4.dp.toPx())
                close()
            },
            p.accent
        )
    }
}

private fun point(c: Offset, r: Float, deg: Float): Offset {
    val t = deg * PI.toFloat() / 180f
    return Offset(c.x + r * cos(t), c.y + r * sin(t))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.line(
    c: Offset, r1: Float, r2: Float, deg: Float, colour: androidx.compose.ui.graphics.Color, width: Float
) {
    drawLine(colour, point(c, r1, deg), point(c, r2, deg), width)
}
