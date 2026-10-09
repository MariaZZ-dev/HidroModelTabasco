package com.hidromodel.tabasco.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta "Hydrological Clarity" tomada del DESIGN.md exportado por Stitch. */
object Hm {
    val Canvas = Color(0xFFF5F8FA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDim = Color(0xFFEDF2F5)
    val Line = Color(0xFFDCE4E8)
    val Grid = Color(0xFFE2E9ED)
    val Teal = Color(0xFF1B6B78)
    val TealDark = Color(0xFF145560)
    val Slate = Color(0xFF486E82)
    val TealSurface = Color(0xFFE7F2F4)
    val TextPrimary = Color(0xFF111C22)
    val TextSecondary = Color(0xFF5C707C)
    val TextDisabled = Color(0xFF9CAFB9)
    val RainBlue = Color(0xFF2A8E9E)

    val NormalFg = Color(0xFF2E7D52)
    val NormalBg = Color(0xFFE8F5E9)
    val NormalBorder = Color(0xFFC8E6C9)
    val AlertFg = Color(0xFFB57414)
    val AlertBg = Color(0xFFFFF4E5)
    val AlertBorder = Color(0xFFFFE0B2)
    val CriticalFg = Color(0xFFC0392B)
    val CriticalBg = Color(0xFFFDF2F0)
    val CriticalBorder = Color(0xFFF8D7DA)
}

private val Scheme = lightColorScheme(
    primary = Hm.Teal,
    onPrimary = Color.White,
    primaryContainer = Hm.TealSurface,
    onPrimaryContainer = Hm.Teal,
    secondary = Hm.Slate,
    background = Hm.Canvas,
    onBackground = Hm.TextPrimary,
    surface = Hm.Surface,
    onSurface = Hm.TextPrimary,
    surfaceVariant = Hm.SurfaceDim,
    onSurfaceVariant = Hm.TextSecondary,
    outline = Hm.Line,
    error = Hm.CriticalFg,
)

@Composable
fun HidroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
