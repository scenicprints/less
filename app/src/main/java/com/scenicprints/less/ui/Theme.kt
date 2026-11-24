package com.scenicprints.less.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scenicprints.less.data.Colourway

/**
 * The whole palette, and it is deliberately short: two greys for surfaces, one hairline, two
 * inks, and one accent. The accent is Braun's orange. It marks the pointer on a scale and the one
 * Nudge that needs you, and is never decoration. See DESIGN.md.
 */
data class Palette(
    val ground: Color,
    val raised: Color,
    val sunk: Color,
    val line: Color,
    val ink: Color,
    val dim: Color,
    val accent: Color,
    val onAccent: Color,
    val dark: Boolean
)

val Orange = Color(0xFFEC5B1A)

/** The black Braun: charcoal, never pure black, so the knob still reads against it. */
val Black = Palette(
    ground = Color(0xFF131313),
    raised = Color(0xFF1E1E1D),
    sunk = Color(0xFF0B0B0B),
    line = Color(0xFF2F2F2D),
    ink = Color(0xFFE6E4DE),
    dim = Color(0xFF8B8984),
    accent = Orange,
    onAccent = Color(0xFFF6F2EA),
    dark = true
)

/** The white Braun: warm grey, never pure white. */
val White = Palette(
    ground = Color(0xFFE9E7E1),
    raised = Color(0xFFDFDDD6),
    sunk = Color(0xFFD3D0C8),
    line = Color(0xFFC4C1B9),
    ink = Color(0xFF1B1B19),
    dim = Color(0xFF6E6C66),
    accent = Orange,
    onAccent = Color(0xFFF6F2EA),
    dark = false
)

val LocalPalette = staticCompositionLocalOf { Black }
val LocalType = staticCompositionLocalOf { Type(FontFamily.SansSerif) }

/** One typeface, few sizes, nothing bold. The clock is the only large thing on the home. */
class Type(family: FontFamily) {
    val clock = TextStyle(
        fontFamily = family, fontSize = 92.sp, lineHeight = 92.sp, fontWeight = FontWeight.Light,
        letterSpacing = (-3).sp, fontFeatureSettings = "tnum"
    )
    val search = TextStyle(fontFamily = family, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.3).sp)
    val name = TextStyle(fontFamily = family, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    val body = TextStyle(fontFamily = family, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    /** Rams lettering: small, spaced, upper case. */
    val label = TextStyle(fontFamily = family, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
    val tiny = TextStyle(fontFamily = family, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Normal)
}

/**
 * Nothing is bundled. A TTF or OTF placed in assets/fonts is used, otherwise the phone's Roboto
 * stands in. Inter is the suggested file.
 */
fun fontFamily(c: Context): FontFamily {
    val files = runCatching { c.assets.list("fonts")?.toList() }.getOrNull().orEmpty()
        .filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) }
        .sorted()
    if (files.isEmpty()) return FontFamily.SansSerif
    val fonts = files.map { name ->
        val lower = name.lowercase()
        val weight = when {
            "thin" in lower -> FontWeight.Thin
            "light" in lower -> FontWeight.Light
            "medium" in lower -> FontWeight.Medium
            "bold" in lower -> FontWeight.Bold
            else -> FontWeight.Normal
        }
        Font("fonts/$name", c.assets, weight)
    }
    return FontFamily(fonts)
}

@Composable
fun LessTheme(colourway: Colourway, content: @Composable () -> Unit) {
    val p = if (colourway == Colourway.BLACK) Black else White
    val c = LocalContext.current
    val type = remember { Type(fontFamily(c)) }
    val scheme = if (p.dark) {
        darkColorScheme(
            primary = p.ink, onPrimary = p.ground, background = p.ground, onBackground = p.ink,
            surface = p.ground, onSurface = p.ink, onSurfaceVariant = p.dim, outline = p.line,
            surfaceVariant = p.raised
        )
    } else {
        lightColorScheme(
            primary = p.ink, onPrimary = p.ground, background = p.ground, onBackground = p.ink,
            surface = p.ground, onSurface = p.ink, onSurfaceVariant = p.dim, outline = p.line,
            surfaceVariant = p.raised
        )
    }
    CompositionLocalProvider(LocalPalette provides p, LocalType provides type) {
        MaterialTheme(colorScheme = scheme, typography = Typography(bodyLarge = type.body), content = content)
    }
}
