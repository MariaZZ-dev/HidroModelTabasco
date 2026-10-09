package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.AppData
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.model.*
import com.hidromodel.tabasco.ui.components.*
import com.hidromodel.tabasco.ui.theme.Hm

@Composable
fun HomeScreen(vm: AppViewModel, onNew: () -> Unit, onData: () -> Unit, onForecast: () -> Unit, onReplay: () -> Unit) {
    val data = vm.data
    Column(Modifier.fillMaxSize()) {
        AppHeader("HidroModel Tabasco", "Simulador de inundaciones")
        ScreenBody {
            HCard {
                InfoChip("Cuencas Grijalva y Usumacinta")
                Spacer(Modifier.height(12.dp))
                Text("HidroModel Tabasco", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Hm.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Simula cómo la lluvia acumulada se convierte en caudal de río y en riesgo de inundación, " +
                        "con un modelo de embalse lineal calibrado con 26 años de datos.",
                    fontSize = 14.sp, color = Hm.TextSecondary, lineHeight = 20.sp,
                )
            }

            HCard(containerColor = Hm.Teal, borderColor = Hm.Teal, onClick = onNew) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Hm.Teal) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Nuevo escenario", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Configurar lluvia y parámetros del modelo", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                }
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard("Datos históricos", "Caudal diario 2000–2025", Icons.Filled.Timeline, true, onData, Modifier.weight(1f).fillMaxHeight())
                QuickCard("Pronóstico actual", "Lluvia prevista a 10 días", Icons.Filled.Cloud, true, onForecast, Modifier.weight(1f).fillMaxHeight())
            }

            if (data == null) {
                LoadingBox()
            } else {
                CalibrationCard(data)
                ReferenceEventCard(data, onReplay)
            }

            HCard(containerColor = Hm.TealSurface, borderColor = Hm.TealSurface) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = Hm.Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Los caudales provienen del modelo hidrológico GloFAS (vía Open-Meteo): son valores simulados, " +
                            "no mediciones de estaciones. La lluvia es la diaria de Open-Meteo en cada punto. " +
                            "El modo Pronóstico usa internet; la app no emite alertas oficiales.",
                        fontSize = 13.sp, color = Hm.TextPrimary, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, sub: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    HCard(modifier = modifier, onClick = if (enabled) onClick else null) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (enabled) Hm.TealSurface else Hm.SurfaceDim),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (enabled) Hm.Teal else Hm.TextDisabled) }
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (enabled) Hm.TextPrimary else Hm.TextDisabled)
        Text(sub, fontSize = 12.sp, color = if (enabled) Hm.TextSecondary else Hm.TextDisabled)
    }
}

@Composable
private fun CalibrationCard(data: AppData) {
    val first = data.points.first().dates.first().year
    val last = data.points.first().dates.last().year
    HCard {
        Text("Base de calibración", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Hm.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${data.points.size} puntos · ${last - first + 1} años de datos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Hm.TextPrimary)
        Text("$first–$last · ${data.totalRecords.toDouble().m3s()} registros diarios", fontSize = 13.sp, color = Hm.TextSecondary)
        Spacer(Modifier.height(12.dp))
        data.points.forEachIndexed { i, p ->
            val c = data.calibrations[i]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                    .background(Hm.Canvas).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Hm.TealSurface),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.WaterDrop, null, tint = Hm.Teal, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                    Text("Río ${p.river}", fontSize = 12.sp, color = Hm.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${p.stats.mean.m3s()} m³/s", style = NumStyle, fontSize = 15.sp, color = Hm.Teal)
                    Text("R² ${c.r2.dec(2)} · k ${c.k.dec(1)} d", fontSize = 12.sp, color = Hm.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ReferenceEventCard(data: AppData, onReplay: () -> Unit) {
    val p = data.points[data.zapataIndex]
    val idx = p.indexOf(REFERENCE_EVENT) ?: return
    val from = (idx - EVENT_PEAK_INDEX).coerceAtLeast(0)
    val to = (from + HORIZON).coerceAtMost(p.flow.size)
    val window = p.flow.copyOfRange(from, to)
    val peak = p.flow[idx]
    val risk = riskOf(peak, p.stats)
    HCard(containerColor = riskBg(risk), borderColor = riskBorder(risk)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = riskFg(risk), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Evento de referencia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = riskFg(risk), modifier = Modifier.weight(1f))
            RiskChip(risk)
        }
        Spacer(Modifier.height(6.dp))
        Text(REFERENCE_EVENT.es(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Hm.TextPrimary)
        Text(
            "Pico de caudal modelado de ${peak.m3s()} m³/s en ${p.name}, por encima del umbral crítico (${p.stats.critical.m3s()} m³/s).",
            fontSize = 14.sp, color = Hm.TextPrimary, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(8.dp))
        LineChart(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            series = listOf(ChartSeries(window, riskFg(risk), filled = true)),
            lines = listOf(ChartLine(p.stats.alert, Hm.AlertFg, "Alerta")),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Reproducir este evento", onReplay)
    }
}
