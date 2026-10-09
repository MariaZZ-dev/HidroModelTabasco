package com.hidromodel.tabasco.model

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min

/** Coeficiente de escurrimiento de referencia: con C = C_REF el modelo usa la calibración tal cual. */
const val C_REF = 0.68
/** Duración (días) de cada escenario simulado. */
const val HORIZON = 30
/** En los eventos históricos, el día del pico queda en esta posición de la ventana (0 = primer día). */
const val EVENT_PEAK_INDEX = 20
/** Evento de referencia (pico de caudal modelado en los tres puntos). */
val REFERENCE_EVENT: LocalDate = LocalDate.of(2020, 11, 7)

enum class RiskLevel(val label: String) {
    NORMAL("Normal"), ALERTA("Alerta"), CRITICO("Crítico")
}

enum class RainMode { MANUAL, HISTORICO, PRONOSTICO }

/** Parámetros del embalse lineal: Q[t] = Q[t-1] + (gain * lluvia[t] - Q[t-1]) / k */
data class Calibration(val k: Double, val gain: Double, val q0: Double, val r2: Double)

data class PointStats(
    val mean: Double,
    val alert: Double,      // percentil 95 del caudal 2000-2025
    val critical: Double,   // percentil 99 del caudal 2000-2025
    val max: Double,
    val maxDate: LocalDate,
)

class RiverPoint(
    val key: String,
    val river: String,
    val name: String,
    val dates: List<LocalDate>,
    val rain: DoubleArray,
    val flow: DoubleArray,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
) {
    private val dateIndex: Map<LocalDate, Int> = HashMap<LocalDate, Int>().also { m ->
        dates.forEachIndexed { i, d -> m[d] = i }
    }

    fun indexOf(date: LocalDate): Int? = dateIndex[date]

    val stats: PointStats = run {
        val sorted = flow.sortedArray()
        var maxI = 0
        flow.forEachIndexed { i, v -> if (v > flow[maxI]) maxI = i }
        PointStats(
            mean = flow.average(),
            alert = percentile(sorted, 0.95),
            critical = percentile(sorted, 0.99),
            max = flow[maxI],
            maxDate = dates[maxI],
        )
    }

    val displayName: String get() = "$name · Río $river"

    private fun percentile(sorted: DoubleArray, p: Double): Double {
        val pos = p * (sorted.size - 1)
        val lo = floor(pos).toInt()
        val hi = min(lo + 1, sorted.size - 1)
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo)
    }
}

data class ScenarioConfig(
    val pointIndex: Int = 1,
    val mode: RainMode = RainMode.MANUAL,
    val intensity: Float = 60f,        // mm/día
    val durationDays: Int = 4,         // días de lluvia continua
    val antecedent: Float = 150f,      // lluvia acumulada de los 30 días previos (mm)
    val runoffC: Float = C_REF.toFloat(),
    val k: Float = 30f,                // constante de retención (días)
    val eventPeak: LocalDate = REFERENCE_EVENT,
)

class PointResult(
    val point: RiverPoint,
    val k: Double,
    val gain: Double,
    val rain: DoubleArray,
    val simulated: DoubleArray,
    val observed: DoubleArray?,
    val q0: Double,
    val startDate: LocalDate?,
    val windowR2: Double?,
) {
    val horizon: Int get() = simulated.size
    val peak: Double = simulated.max()
    val peakDay: Int = simulated.indexOfFirst { it == peak }
    val daysAlert: Int = simulated.count { it > point.stats.alert }
    val daysCritical: Int = simulated.count { it >= point.stats.critical }
    val risk: RiskLevel = riskOf(peak, point.stats)
}

class ScenarioResult(val config: ScenarioConfig, val results: List<PointResult>) {
    val selected: PointResult get() = results[config.pointIndex]
}

fun riskOf(q: Double, s: PointStats): RiskLevel = when {
    q >= s.critical -> RiskLevel.CRITICO
    q > s.alert -> RiskLevel.ALERTA
    else -> RiskLevel.NORMAL
}

data class HistoricEvent(val peakDate: LocalDate, val label: String, val peakFlow: Double)

data class HistoryEntry(
    val id: Long,
    val createdAt: String,
    val pointName: String,
    val river: String,
    val mode: String,
    val summary: String,
    val peak: Double,
    val peakDay: Int,
    val risk: RiskLevel,
    val series: DoubleArray,
    val alert: Double,
    val critical: Double,
)
