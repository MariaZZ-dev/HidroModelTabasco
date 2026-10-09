package com.hidromodel.tabasco.data

import android.content.Context
import com.hidromodel.tabasco.model.HistoryEntry
import com.hidromodel.tabasco.model.RiskLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Guarda el historial de corridas en un archivo JSON local (luego se puede sustituir por Room o Supabase). */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "historial.json")

    fun load(): List<HistoryEntry> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        file.writeText(arr.toString())
    }

    private fun toJson(e: HistoryEntry) = JSONObject().apply {
        put("id", e.id); put("createdAt", e.createdAt); put("pointName", e.pointName)
        put("river", e.river); put("mode", e.mode); put("summary", e.summary)
        put("peak", e.peak); put("peakDay", e.peakDay); put("risk", e.risk.name)
        put("alert", e.alert); put("critical", e.critical)
        put("series", JSONArray().also { a -> e.series.forEach { a.put(it) } })
    }

    private fun parse(o: JSONObject): HistoryEntry {
        val s = o.getJSONArray("series")
        return HistoryEntry(
            id = o.getLong("id"), createdAt = o.getString("createdAt"), pointName = o.getString("pointName"),
            river = o.getString("river"), mode = o.getString("mode"), summary = o.getString("summary"),
            peak = o.getDouble("peak"), peakDay = o.getInt("peakDay"),
            risk = RiskLevel.valueOf(o.getString("risk")),
            series = DoubleArray(s.length()) { s.getDouble(it) },
            alert = o.getDouble("alert"), critical = o.getDouble("critical"),
        )
    }
}
