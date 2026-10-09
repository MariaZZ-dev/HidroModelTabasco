package com.hidromodel.tabasco.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Pronóstico de un punto: lluvia diaria prevista, caudal inicial y (si existe) el pronóstico GloFAS de referencia. */
class PointForecast(
    val startDate: LocalDate,
    val rain: DoubleArray,
    val glofas: DoubleArray?,
    val q0: Double,
)

class ForecastData(
    val fetchedAt: LocalDateTime,
    val points: List<PointForecast>,
    val fromCache: Boolean = false,
)

object ForecastBuilder {
    /**
     * Alinea por fecha la lluvia pronosticada con el caudal GloFAS.
     * El caudal inicial es el del día anterior al primer día pronosticado (o el de ese día si falta).
     */
    fun build(
        rainDates: List<LocalDate>,
        rain: List<Double?>,
        floodDates: List<LocalDate>,
        flood: List<Double?>,
    ): PointForecast {
        require(rainDates.isNotEmpty()) { "Sin pronóstico de lluvia" }
        val start = rainDates.first()
        val fmap = HashMap<LocalDate, Double>()
        floodDates.forEachIndexed { i, d -> flood.getOrNull(i)?.let { fmap[d] = it } }
        val q0 = fmap[start.minusDays(1)] ?: fmap[start]
            ?: throw IllegalStateException("Sin caudal inicial para el punto")
        val rainArr = DoubleArray(rainDates.size) { rain.getOrNull(it) ?: 0.0 }
        val vals = rainDates.map { fmap[it] }
        val glofas = if (vals.all { it != null }) vals.map { it!! }.toDoubleArray() else null
        return PointForecast(start, rainArr, glofas, q0)
    }
}
