package com.hidromodel.tabasco.data

import com.hidromodel.tabasco.model.RiverPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate

/** Lee tabasco_lluvia_caudal.csv: punto,fecha,lluvia_mm,caudal_m3s,lat,lon */
object CsvLoader {

    private class Acc {
        val dates = ArrayList<LocalDate>()
        val rain = ArrayList<Double>()
        val flow = ArrayList<Double>()
        var lat = 0.0
        var lon = 0.0
    }

    fun load(input: InputStream): List<RiverPoint> {
        val groups = LinkedHashMap<String, Acc>()
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
            br.readLine() // encabezado
            while (true) {
                val line = br.readLine() ?: break
                if (line.isBlank()) continue
                val c = line.split(',')
                if (c.size < 4) continue
                val acc = groups.getOrPut(c[0]) { Acc() }
                acc.dates += LocalDate.parse(c[1])
                acc.rain += c[2].toDouble()
                acc.flow += c[3].toDouble()
                if (c.size >= 6) { acc.lat = c[4].toDoubleOrNull() ?: acc.lat; acc.lon = c[5].toDoubleOrNull() ?: acc.lon }
            }
        }
        return groups.map { (key, a) ->
            val parts = key.split(" - ")
            val order = a.dates.indices.sortedBy { a.dates[it] }
            RiverPoint(
                key = key,
                river = parts.getOrElse(0) { key },
                name = parts.getOrElse(1) { key },
                dates = order.map { a.dates[it] },
                rain = DoubleArray(order.size) { a.rain[order[it]] },
                flow = DoubleArray(order.size) { a.flow[order[it]] },
                lat = a.lat,
                lon = a.lon,
            )
        }
    }
}
