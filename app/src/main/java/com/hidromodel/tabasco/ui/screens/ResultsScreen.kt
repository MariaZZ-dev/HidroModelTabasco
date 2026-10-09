package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.model.*
import com.hidromodel.tabasco.ui.components.*
import com.hidromodel.tabasco.ui.theme.Hm

@Composable
fun ResultsScreen(vm: AppViewModel, onCompare: () -> Unit) {
    val data = vm.data
    val res = vm.result
    Column(Modifier.fillMaxSize()) {
        AppHeader("Resultados y predicción", res?.selected?.point?.displayName ?: "Cargando…")
        if (data == null || res == null) {
            NoResultNotice(data == null || vm.forecastLoading)
            return@Column
        }
        val cfg = res.config
        val pr = res.selected
        val s = pr.point.stats
        val cal = data.calibrations[cfg.pointIndex]
        val vsMean = (pr.peak / s.mean - 1.0) * 100.0

        ScreenBody {
            // Banner de riesgo
            HCard(containerColor = riskBg(pr.risk), borderColor = riskBorder(pr.risk)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = riskFg(pr.risk))
                    Spacer(Modifier.width(10.dp))
                    Text(Narrative.riskTitle(pr.risk), Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = riskFg(pr.risk))
                    RiskChip(pr.risk)
                }
                Spacer(Modifier.height(8.dp))
                Text(Narrative.riskText(pr), fontSize = 14.sp, color = Hm.TextPrimary, lineHeight = 20.sp)
            }

            // Hidrograma
            HCard {
                Text("Hidrograma de caudales", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Legend(
                    listOf(
                        "Simulado (k=${pr.k.dec(1)})" to Hm.Teal,
                        (if (pr.observed == null) "Caudal medio" else "Referencia GloFAS") to Hm.TextDisabled,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                val reference = pr.observed ?: DoubleArray(pr.horizon) { s.mean }
                LineChart(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    series = listOf(
                        ChartSeries(pr.simulated, Hm.Teal, filled = true),
                        ChartSeries(reference, Hm.TextDisabled, dashed = true, width = 2f),
                    ),
                    lines = listOf(
                        ChartLine(s.critical, Hm.CriticalFg, "Crítico"),
                        ChartLine(s.alert, Hm.AlertFg, "Alerta"),
                    ),
                    xLabels = xLabelsFor(pr.horizon, pr.startDate),
                    rain = pr.rain,
                    peakIndex = pr.peakDay,
                    peakLabel = "${pr.peak.m3s()} m³/s",
                )
                Spacer(Modifier.height(4.dp))
                Text(Narrative.thresholdsNote(s), fontSize = 11.sp, color = Hm.TextSecondary)
            }

            // Métricas
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Caudal pico", pr.peak.m3s(),
                    (if (vsMean >= 0) "+" else "") + "${vsMean.dec(0)}% vs promedio", Hm.Teal, Modifier.weight(1f).fillMaxHeight())
                MetricCard("Día del pico", "Día ${pr.peakDay + 1}",
                    pr.startDate?.plusDays(pr.peakDay.toLong())?.es() ?: "de ${pr.horizon} días simulados",
                    Hm.TextPrimary, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Días en alerta", "${pr.daysAlert}", "caudal > ${s.alert.m3s()} m³/s", Hm.AlertFg, Modifier.weight(1f).fillMaxHeight())
                MetricCard("Ajuste del modelo", "R² ${cal.r2.dec(2)}",
                    if (pr.windowR2 != null) "ventana del evento: ${pr.windowR2.dec(2)}" else "calibración 2000–2025",
                    Hm.Teal, Modifier.weight(1f).fillMaxHeight())
            }

            // Diagnóstico
            HCard {
                Text("Diagnóstico hidrológico", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Hm.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(Narrative.diagnosis(pr, cfg), fontSize = 14.sp, color = Hm.TextPrimary, lineHeight = 21.sp)
            }

            // Lluvia acumulada
            HCard {
                Text("Lluvia acumulada por ventana", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                Text("Máximo acumulado dentro del escenario", fontSize = 12.sp, color = Hm.TextSecondary)
                Spacer(Modifier.height(6.dp))
                val n = pr.horizon
                val sizes = (listOf(3, 7, 14, 30).filter { it < n } + n).distinct()
                val windows = sizes.map { it to Narrative.maxWindowRain(pr.rain, it) }
                val top = windows.maxOf { it.second }.coerceAtLeast(1.0)
                windows.forEach { (w, v) ->
                    val label = if (w == n && n != 30) "Total del periodo ($w días)" else "Ventana de $w días"
                    HBarRow(label, "${v.dec(0)} mm", (v / top).toFloat(), if (w == n) Hm.Teal else Hm.RainBlue)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Comparar", onCompare, Modifier.weight(1f), Icons.Filled.CompareArrows)
                PrimaryButton("Guardar corrida", { vm.saveRun() }, Modifier.weight(1f), Icons.Filled.Save)
            }
        }
    }
}
