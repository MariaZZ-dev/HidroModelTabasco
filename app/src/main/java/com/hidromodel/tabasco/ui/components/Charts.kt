package com.hidromodel.tabasco.ui.components

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.model.RiskLevel
import com.hidromodel.tabasco.ui.theme.Hm
import kotlin.math.max

data class ChartSeries(
    val values: DoubleArray,
    val color: Color,
    val dashed: Boolean = false,
    val filled: Boolean = false,
    val width: Float = 2.5f,
)

data class ChartLine(val value: Double, val color: Color, val label: String)

private fun axisLabel(v: Double): String =
    if (v >= 1000) String.format(java.util.Locale.US, "%.1fk", v / 1000.0).replace(".0k", "k")
    else String.format(java.util.Locale.US, "%.0f", v)

/**
 * Gráfica de líneas dibujada con Canvas.
 *  - series: curvas (caudal simulado, observado, etc.)
 *  - rain: barras de lluvia colgando desde arriba
 *  - lines: umbrales horizontales punteados (Alerta / Crítico)
 *  - cursor: día actual (animación de la simulación)
 *  - peakIndex/peakLabel: marca del caudal pico
 */
@Composable
fun LineChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(220.dp),
    series: List<ChartSeries>,
    lines: List<ChartLine> = emptyList(),
    xLabels: List<Pair<Int, String>> = emptyList(),
    rain: DoubleArray? = null,
    cursor: Int? = null,
    yMax: Double? = null,
    peakIndex: Int? = null,
    peakLabel: String? = null,
) {
    val density = LocalDensity.current
    val paint = remember(density) {
        NativePaint().apply { isAntiAlias = true; textSize = with(density) { 10.sp.toPx() } }
    }

    Canvas(modifier) {
        val n = series.maxOfOrNull { it.values.size } ?: 0
        if (n < 2) return@Canvas
        val padL = 38.dp.toPx(); val padR = 10.dp.toPx(); val padT = 8.dp.toPx(); val padB = 20.dp.toPx()
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        val rainBand = if (rain != null) h * 0.22f else 0f
        val top = padT + rainBand + (if (rain != null) 8.dp.toPx() else 0f)
        val plotH = padT + h - top

        val dataMax = max(
            1.0,
            max(
                yMax ?: 0.0,
                max(series.maxOf { s -> s.values.maxOrNull() ?: 0.0 }, lines.maxOfOrNull { it.value } ?: 0.0),
            ),
        ) * 1.12

        fun px(i: Int) = padL + w * i / (n - 1)
        fun py(v: Double) = top + plotH * (1f - (v / dataMax).toFloat())

        // Cuadrícula + etiquetas del eje Y
        paint.color = Hm.TextSecondary.toArgb32(); paint.textAlign = NativePaint.Align.RIGHT
        for (j in 0..4) {
            val v = dataMax * j / 4.0
            val y = py(v)
            drawLine(Hm.Grid, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(axisLabel(v), padL - 6.dp.toPx(), y + 3.dp.toPx(), paint)
        }

        // Barras de lluvia
        if (rain != null && rain.isNotEmpty()) {
            val maxRain = max(1.0, rain.max())
            val barW = max(2.dp.toPx(), w / rain.size * 0.55f)
            rain.forEachIndexed { i, r ->
                if (r > 0.0) {
                    val bh = (rainBand * (r / maxRain)).toFloat()
                    drawRect(
                        Hm.RainBlue.copy(alpha = 0.55f),
                        topLeft = Offset(px(i) - barW / 2, padT),
                        size = Size(barW, bh),
                    )
                }
            }
        }

        // Curvas
        series.forEach { s ->
            val m = s.values.size
            if (m < 2) return@forEach
            val path = Path()
            val smooth = m <= 120
            path.moveTo(px(0), py(s.values[0]))
            for (i in 1 until m) {
                val x0 = px(i - 1); val y0 = py(s.values[i - 1])
                val x1 = px(i); val y1 = py(s.values[i])
                if (smooth) path.cubicTo((x0 + x1) / 2, y0, (x0 + x1) / 2, y1, x1, y1) else path.lineTo(x1, y1)
            }
            if (s.filled) {
                val area = Path().apply {
                    addPath(path)
                    lineTo(px(m - 1), py(0.0))
                    lineTo(px(0), py(0.0))
                    close()
                }
                drawPath(area, s.color.copy(alpha = 0.13f))
            }
            drawPath(
                path, s.color,
                style = Stroke(
                    width = s.width.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) else null,
                ),
            )
        }

        // Umbrales
        lines.forEach { l ->
            val y = py(l.value)
            drawLine(l.color, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f))
            paint.color = l.color.toArgb32(); paint.textAlign = NativePaint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(l.label, size.width - padR - 2.dp.toPx(), y - 4.dp.toPx(), paint)
        }

        // Cursor (día actual)
        if (cursor != null && cursor in 0 until n) {
            val x = px(cursor)
            drawLine(Hm.Teal, Offset(x, top), Offset(x, padT + h), strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f))
            val first = series.firstOrNull()
            if (first != null && cursor < first.values.size) {
                drawCircle(Color.White, 6.dp.toPx(), Offset(x, py(first.values[cursor])))
                drawCircle(first.color, 4.dp.toPx(), Offset(x, py(first.values[cursor])))
            }
        }

        // Pico
        val first = series.firstOrNull()
        if (peakIndex != null && first != null && peakIndex in first.values.indices) {
            val x = px(peakIndex); val y = py(first.values[peakIndex])
            drawCircle(Color.White, 6.dp.toPx(), Offset(x, y))
            drawCircle(Hm.CriticalFg, 4.dp.toPx(), Offset(x, y))
            if (peakLabel != null) {
                paint.color = Hm.TextPrimary.toArgb32(); paint.textAlign = NativePaint.Align.CENTER
                val tx = x.coerceIn(padL + 30.dp.toPx(), size.width - padR - 30.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(peakLabel, tx, y - 10.dp.toPx(), paint)
            }
        }

        // Etiquetas del eje X
        paint.color = Hm.TextSecondary.toArgb32(); paint.textAlign = NativePaint.Align.CENTER
        xLabels.forEach { (i, label) ->
            if (i in 0 until n) drawContext.canvas.nativeCanvas.drawText(label, px(i), size.height - 4.dp.toPx(), paint)
        }
    }
}

private fun Color.toArgb32(): Int = this.toArgb()

/** Medidor vertical de nivel del río, con marcas de Alerta y Crítico. */
@Composable
fun RiverGauge(
    value: Double,
    alert: Double,
    critical: Double,
    risk: RiskLevel,
    modifier: Modifier = Modifier.size(width = 64.dp, height = 160.dp),
) {
    val scaleMax = max(critical * 1.35, value * 1.1)
    Canvas(modifier) {
        val r = 14.dp.toPx()
        val tank = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
        drawPath(tank, Hm.SurfaceDim)
        val frac = (value / scaleMax).toFloat().coerceIn(0f, 1f)
        val fillH = size.height * frac
        clipPath(tank) {
            drawRect(riskFg(risk).copy(alpha = 0.78f), Offset(0f, size.height - fillH), Size(size.width, fillH))
            drawRect(Color.White.copy(alpha = 0.35f), Offset(0f, size.height - fillH), Size(size.width, 3.dp.toPx()))
        }
        val yA = size.height * (1f - (alert / scaleMax).toFloat())
        val yC = size.height * (1f - (critical / scaleMax).toFloat())
        drawLine(Hm.AlertFg, Offset(0f, yA), Offset(size.width, yA), strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
        drawLine(Hm.CriticalFg, Offset(0f, yC), Offset(size.width, yC), strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
        drawPath(tank, Hm.Line, style = Stroke(width = 2.dp.toPx()))
    }
}

/** Minigráfica sin ejes (historial). */
@Composable
fun Sparkline(values: DoubleArray, color: Color, modifier: Modifier = Modifier.size(width = 96.dp, height = 40.dp)) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = values.min(); val hi = values.max()
        val span = max(1e-9, hi - lo)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1f - ((v - lo) / span).toFloat()) * 0.9f + size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Barra de zonas Normal / Alerta / Crítico con un marcador en la posición del caudal. */
@Composable
fun RiskZoneBar(value: Double, mean: Double, alert: Double, critical: Double, modifier: Modifier = Modifier.fillMaxWidth().height(22.dp)) {
    val scaleMax = max(critical * 1.5, value * 1.05)
    Canvas(modifier) {
        val barTop = size.height * 0.35f
        val barH = size.height * 0.3f
        val xa = size.width * (alert / scaleMax).toFloat()
        val xc = size.width * (critical / scaleMax).toFloat()
        drawRoundRect(Hm.NormalBorder, Offset(0f, barTop), Size(xa, barH), CornerRadius(barH / 2))
        drawRect(Hm.AlertBorder, Offset(xa, barTop), Size(xc - xa, barH))
        drawRoundRect(Hm.CriticalBorder, Offset(xc, barTop), Size(size.width - xc, barH), CornerRadius(barH / 2))
        val xv = (size.width * (value / scaleMax).toFloat()).coerceIn(0f, size.width)
        drawCircle(Color.White, size.height * 0.38f, Offset(xv, size.height / 2))
        drawCircle(Hm.Teal, size.height * 0.26f, Offset(xv, size.height / 2))
    }
}
