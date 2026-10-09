package com.hidromodel.tabasco.data

import com.hidromodel.tabasco.model.ForecastBuilder
import com.hidromodel.tabasco.model.ForecastData
import com.hidromodel.tabasco.model.PointForecast
import com.hidromodel.tabasco.model.RiverPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Descarga el pronóstico de Open-Meteo para cada punto:
 *  - lluvia diaria prevista (API de pronóstico)
 *  - caudal modelado GloFAS: el de ayer sirve como caudal inicial y el futuro como referencia (API de inundaciones)
 */
object ForecastRepository {
    private const val RAIN_URL = "https://api.open-meteo.com/v1/forecast"
    private const val FLOOD_URL = "https://flood-api.open-meteo.com/v1/flood"
    const val FORECAST_DAYS = 10

    suspend fun fetch(points: List<RiverPoint>): ForecastData = coroutineScope {
        val list = points.map { p -> async(Dispatchers.IO) { fetchPoint(p) } }.awaitAll()
        ForecastData(LocalDateTime.now(), list)
    }

    private fun fetchPoint(p: RiverPoint): PointForecast {
        val rainUrl = "$RAIN_URL?latitude=${p.lat}&longitude=${p.lon}" +
            "&daily=precipitation_sum&forecast_days=$FORECAST_DAYS&timezone=America%2FMexico_City"
        val floodUrl = "$FLOOD_URL?latitude=${p.lat}&longitude=${p.lon}" +
            "&daily=river_discharge&past_days=2&forecast_days=$FORECAST_DAYS"

        val rd = JSONObject(httpGet(rainUrl)).getJSONObject("daily")
        val fd = JSONObject(httpGet(floodUrl)).getJSONObject("daily")
        return ForecastBuilder.build(
            rainDates = dates(rd.getJSONArray("time")),
            rain = values(rd.getJSONArray("precipitation_sum")),
            floodDates = dates(fd.getJSONArray("time")),
            flood = values(fd.getJSONArray("river_discharge")),
        )
    }

    private fun dates(a: JSONArray): List<LocalDate> = (0 until a.length()).map { LocalDate.parse(a.getString(it)) }

    private fun values(a: JSONArray): List<Double?> =
        (0 until a.length()).map { if (a.isNull(it)) null else a.getDouble(it) }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("HTTP $code")
            return body
        } finally {
            c.disconnect()
        }
    }
}
