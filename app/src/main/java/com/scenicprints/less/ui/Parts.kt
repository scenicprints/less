package com.scenicprints.less.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The hairline. Every division on the screen is this and nothing heavier. */
@Composable
fun Rule(modifier: Modifier = Modifier, colour: Color = LocalPalette.current.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(colour))
}

/** Rams lettering: upper case, small, spaced, the colour of a printed legend. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, colour: Color = LocalPalette.current.dim) {
    Text(
        text.uppercase(), modifier, color = colour, style = LocalType.current.label,
        maxLines = 1, overflow = TextOverflow.Ellipsis
    )
}

/**
 * The tuning scale: a baseline, ticks, and one pointer in the accent.
 *
 * [value] is 0..1 along the scale. With [detents] set, the long ticks are the detents and the
 * short ticks halve them, which is the dial under the pages; otherwise ten long ticks and a short
 * one between each, which is the battery.
 */
@Composable
fun Scale(value: Float, modifier: Modifier = Modifier, detents: Int = 0, pointerWidth: Float = 2f, pointerHeight: Float = 16f) {
    val p = LocalPalette.current
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
        val w = size.width
        val base = size.height - 1.dp.toPx()
        drawLine(p.line, Offset(0f, base), Offset(w, base), 1.dp.toPx())
        if (detents > 1) {
            for (i in 0 until detents) {
                val x = w * i / (detents - 1)
                drawLine(p.line, Offset(x, base), Offset(x, base - 9.dp.toPx()), 1.dp.toPx())
                if (i < detents - 1) {
                    val m = w * (i + 0.5f) / (detents - 1)
                    drawLine(p.line, Offset(m, base), Offset(m, base - 4.dp.toPx()), 1.dp.toPx())
                }
            }
        } else {
            for (i in 0..20) {
                val x = w * i / 20
                val h = if (i % 2 == 0) 9.dp.toPx() else 4.dp.toPx()
                drawLine(p.line, Offset(x, base), Offset(x, base - h), 1.dp.toPx())
            }
        }
        val pw = pointerWidth.dp.toPx()
        val x = (w * value).coerceIn(pw / 2, w - pw / 2)
        if (detents > 1) {
            drawRoundRect(
                p.accent,
                topLeft = Offset(x - pw / 2, base - pointerHeight.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(pw, pointerHeight.dp.toPx() + 1.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
            )
        } else {
            drawLine(p.accent, Offset(x, base + 1.dp.toPx()), Offset(x, base - pointerHeight.dp.toPx()), pw)
        }
    }
}

/**
 * A setting as a row: its name on the left, its options on the right, the chosen one in ink with a
 * short accent rule beneath. No switches and no drop-downs; every option is in view.
 */
@Composable
fun <T> Choice(name: String, options: List<T>, chosen: T, text: (T) -> String, onChoose: (T) -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(name, Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                options.forEach { o ->
                    val on = o == chosen
                    Column(Modifier.clickable { onChoose(o) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text(o), color = if (on) p.ink else p.dim, style = type.body)
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(14.dp).height(2.dp).background(if (on) p.accent else Color.Transparent))
                    }
                }
            }
        }
        Rule()
    }
}

/** A row that does one thing when tapped. The text says what, and there is nothing else on it. */
@Composable
fun Action(text: String, onClick: () -> Unit, colour: Color = LocalPalette.current.ink, trailing: String? = null) {
    val type = LocalType.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), color = colour, style = type.body)
            if (trailing != null) Label(trailing)
        }
        Rule()
    }
}

/** The pull: a short grey line, the one mark on the home that is not an icon. */
@Composable
fun Pull(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(36.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(p.dim))
    }
}
