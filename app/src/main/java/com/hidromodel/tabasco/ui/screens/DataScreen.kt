package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.model.*
import com.hidromodel.tabasco.ui.components.*
import com.hidromodel.tabasco.ui.theme.Hm

@Composable
fun DataScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.data
    Column(Modifier.fillMaxSize()) {
        AppHeader("Datos históricos", "Caudal diario 2000–2025", onBack)
        if (data == null) {
            LoadingBox()
            return@Column
        }
        var pIdx by remember { mutableIntStateOf(data.zapataIndex) }
        var yIdx by remember { mutableIntStateOf(0) } // 0 = todos los años

        val p = data.points[pIdx]
        val s = p.stats
        val years = (p.dates.first().year..p.dates.last().year).toList()
        val year = if (yIdx == 0) null else years[yIdx - 1]

        // Serie a graficar
        val idxs = p.dates.indices.filter { year == null || p.dates[it].year == year }
        val flow = DoubleArray(idxs.size) { p.flow[idxs[it]] }
        val values: DoubleArray
        val labels: List<Pair<Int, String>>
        if (year == null) {
            val weeks = flow.asList().chunked(7).map { it.max() }
            values = weeks.toDoubleArray()
            labels = years.filter { (it - years.first()) % 5 == 0 }.map { y ->
                (idxs.indexOfFirst { p.dates[it].year == y } / 7) to "$y"
            }
        } else {
            values = flow
            labels = (1..12 step 2).map { m ->
                idxs.indexOfFirst { p.dates[it].monthValue == m } to monthAbbr(m)
            }
        }

        val daysAlert = flow.count { it > s.alert }
        val rank = years.map { y -> y to p.flow.indices.count { p.dates[it].year == y && p.flow[it] > s.alert } }
            .sortedByDescending { it.second }.take(6)
        val rankMax = rank.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        val refIdx = p.indexOf(REFERENCE_EVENT)

        ScreenBody {
            HCard {
                SectionLabel("Punto de monitoreo")
                Spacer(Modifier.height(8.dp))
                HDropdown(data.points.map { it.displayName }, pIdx) { pIdx = it }
                Spacer(Modifier.height(12.dp))
                SectionLabel("Periodo")
                Spacer(Modifier.height(8.dp))
                HDropdown(listOf("Todos los años (máximo semanal)") + years.map { "$it" }, yIdx) { yIdx = it }
            }

            HCard {
                Text("Caudal diario modelado", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    series = listOf(ChartSeries(values, Hm.Teal, filled = true, width = 1.8f)),
                    lines = listOf(
                        ChartLine(s.critical, Hm.CriticalFg, "Crítico"),
                        ChartLine(s.alert, Hm.AlertFg, "Alerta"),
                    ),
                    xLabels = labels,
                )
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Máximo del periodo", flow.max().m3s(), "m³/s", Hm.CriticalFg, Modifier.weight(1f).fillMaxHeight())
                MetricCard("Días en alerta", "$daysAlert", "de ${flow.size} días", Hm.AlertFg, Modifier.weight(1f).fillMaxHeight())
            }

            HCard {
                Text("Años con más días en alerta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                Text("Días con caudal sobre ${s.alert.m3s()} m³/s", fontSize = 12.sp, color = Hm.TextSecondary)
                Spacer(Modifier.height(6.dp))
                rank.forEach { (y, d) -> HBarRow("$y", "$d días", d.toFloat() / rankMax, Hm.AlertFg) }
            }

            if (refIdx != null) {
                val q = p.flow[refIdx]
                val r = riskOf(q, s)
                HCard(containerColor = riskBg(r), borderColor = riskBorder(r)) {
                    Text("Evento de referencia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = riskFg(r))
                    Text(REFERENCE_EVENT.es(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Hm.TextPrimary)
                    Text("Caudal modelado de ${q.m3s()} m³/s en ${p.name}.", fontSize = 14.sp, color = Hm.TextPrimary)
                }
            }
            Text(Narrative.thresholdsNote(s), fontSize = 11.sp, color = Hm.TextSecondary)
        }
    }
}
