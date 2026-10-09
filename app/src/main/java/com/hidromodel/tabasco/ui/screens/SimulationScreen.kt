package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.model.*
import com.hidromodel.tabasco.ui.components.*
import com.hidromodel.tabasco.ui.theme.Hm
import kotlinx.coroutines.delay

@Composable
fun SimulationScreen(vm: AppViewModel, onBack: () -> Unit, onResults: () -> Unit) {
    val result = vm.result
    Column(Modifier.fillMaxSize()) {
        AppHeader("Simulación", result?.selected?.point?.displayName ?: "Cargando…", onBack)
        if (result == null) NoResultNotice(vm.data == null || vm.forecastLoading) else SimulationBody(result, onResults)
    }
}

@Composable
private fun SimulationBody(result: ScenarioResult, onResults: () -> Unit) {
    val cfg = result.config
    val horizon = result.results[0].horizon
    var day by remember(cfg) { mutableIntStateOf(0) }
    var playing by remember(cfg) { mutableStateOf(true) }
    var speed by remember { mutableIntStateOf(1) }
    var sel by remember(cfg) { mutableIntStateOf(cfg.pointIndex) }

    LaunchedEffect(playing, speed, cfg) {
        while (playing && day < horizon - 1) {
            delay(900L / speed)
            day++
        }
        if (day >= horizon - 1) playing = false
    }

    val pr = result.results[sel]
    val s = pr.point.stats
    val q = pr.simulated[day]
    val prev = if (day == 0) pr.q0 else pr.simulated[day - 1]
    val deltaPct = if (prev > 0) (q / prev - 1.0) * 100.0 else 0.0
    val risk = riskOf(q, s)

    ScreenBody {
        // Controles
        HCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Día ${day + 1} / $horizon", style = NumStyle, fontSize = 20.sp, color = Hm.TextPrimary)
                    val modeText = when (cfg.mode) {
                        RainMode.MANUAL -> "Escenario manual"
                        RainMode.HISTORICO -> "Evento histórico"
                        RainMode.PRONOSTICO -> "Pronóstico Open-Meteo"
                    }
                    val dateText = result.results[sel].startDate?.plusDays(day.toLong())?.es()
                    Text(if (dateText != null) "$modeText · $dateText" else modeText, fontSize = 12.sp, color = Hm.TextSecondary)
                }
                IconButton(
                    onClick = {
                        if (day >= horizon - 1) { day = 0; playing = true } else playing = !playing
                    },
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Hm.Teal),
                ) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Reproducir o pausar", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                HSegmented(listOf("1x", "2x", "5x"), when (speed) { 1 -> 0; 2 -> 1; else -> 2 }, {
                    speed = when (it) { 0 -> 1; 1 -> 2; else -> 5 }
                }, Modifier.width(140.dp))
            }
        }

        // Medidor
        HCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pr.point.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                RiskChip(risk)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RiverGauge(q, s.alert, s.critical, risk)
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("Caudal en el día ${day + 1}", fontSize = 12.sp, color = Hm.TextSecondary)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(q.m3s(), style = NumStyle, fontSize = 36.sp, color = riskFg(risk))
                        Spacer(Modifier.width(4.dp))
                        Text("m³/s", fontSize = 14.sp, color = Hm.TextSecondary, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    val sign = if (deltaPct >= 0) "+" else ""
                    Text("$sign${deltaPct.dec(1)}% respecto al día anterior", fontSize = 12.sp, color = Hm.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Alerta ${s.alert.m3s()}  ·  Crítico ${s.critical.m3s()}", fontSize = 12.sp, color = Hm.TextSecondary)
                }
            }
        }

        // Hidrograma
        HCard {
            Text("Lluvia y caudal simulado", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Legend(listOf("Lluvia (mm)" to Hm.RainBlue, "Caudal (m³/s)" to Hm.Teal))
            Spacer(Modifier.height(8.dp))
            LineChart(
                series = listOf(ChartSeries(pr.simulated, Hm.Teal, filled = true)),
                lines = listOf(
                    ChartLine(s.critical, Hm.CriticalFg, "Crítico"),
                    ChartLine(s.alert, Hm.AlertFg, "Alerta"),
                ),
                xLabels = xLabelsFor(horizon, pr.startDate),
                rain = pr.rain,
                cursor = day,
            )
        }

        // Puntos de monitoreo
        HCard {
            Text("Puntos de monitoreo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
            Text("Toca un punto para verlo arriba. Los puntos no seleccionados usan su calibración.", fontSize = 12.sp, color = Hm.TextSecondary)
            Spacer(Modifier.height(8.dp))
            result.results.forEachIndexed { i, r ->
                val qi = r.simulated[day]
                val ri = riskOf(qi, r.point.stats)
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape)
                        .background(if (i == sel) Hm.TealSurface else Color.White)
                        .border(1.dp, if (i == sel) Hm.Teal else Hm.Line, shape)
                        .clickable { sel = i }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(riskFg(ri)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.point.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                        Text("Río ${r.point.river} · ${qi.m3s()} m³/s", fontSize = 12.sp, color = Hm.TextSecondary)
                    }
                    RiskChip(ri)
                }
            }
        }

        // Diagnóstico
        HCard(containerColor = riskBg(pr.risk), borderColor = riskBorder(pr.risk)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = riskFg(pr.risk), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Diagnóstico: ${pr.point.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Hm.TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                MiniStat("Retención", "k = ${pr.k.dec(1)} d", Modifier.weight(1f))
                MiniStat("Pico estimado", "Día ${pr.peakDay + 1}", Modifier.weight(1f))
                MiniStat("Máximo", "${pr.peak.m3s()}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(Narrative.riskText(pr), fontSize = 14.sp, color = Hm.TextPrimary, lineHeight = 20.sp)
        }

        if (day >= horizon - 1) {
            PrimaryButton("Ver resultados y predicción", onResults)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Hm.TextSecondary)
        Text(value, style = NumStyle, fontSize = 16.sp, color = Hm.TextPrimary)
    }
}
