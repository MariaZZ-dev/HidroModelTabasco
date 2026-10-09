package com.hidromodel.tabasco.data

import android.content.Context
import com.hidromodel.tabasco.model.ForecastData
import com.hidromodel.tabasco.model.PointForecast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** Guarda el último pronóstico descargado para poder mostrarlo sin internet. */
class ForecastStore(context: Context) {
    private val file = File(context.filesDir, "pronostico.json")

    fun load(): ForecastData? = try {
        if (!file.exists()) null else {
            val o = JSONObject(file.readText())
            val pts = o.getJSONArray("points")
            ForecastData(
                fetchedAt = LocalDateTime.parse(o.getString("fetchedAt")),
                points = (0 until pts.length()).map { parse(pts.getJSONObject(it)) },
                fromCache = true,
            )
        }
    } catch (e: Exception) {
        null
    }

    fun save(d: ForecastData) {
        val pts = JSONArray()
        d.points.forEach { pts.put(toJson(it)) }
        val o = JSONObject().put("fetchedAt", d.fetchedAt.toString()).put("points", pts)
        file.writeText(o.toString())
    }

    private fun toJson(p: PointForecast) = JSONObject().apply {
        put("start", p.startDate.toString())
        put("q0", p.q0)
        put("rain", JSONArray().also { a -> p.rain.forEach { a.put(it) } })
        p.glofas?.let { g -> put("glofas", JSONArray().also { a -> g.forEach { a.put(it) } }) }
    }

    private fun parse(o: JSONObject): PointForecast {
        val r = o.getJSONArray("rain")
        val g = if (o.has("glofas")) o.getJSONArray("glofas") else null
        return PointForecast(
            startDate = LocalDate.parse(o.getString("start")),
            rain = DoubleArray(r.length()) { r.getDouble(it) },
            glofas = g?.let { arr -> DoubleArray(arr.length()) { arr.getDouble(it) } },
            q0 = o.getDouble("q0"),
        )
    }
}
