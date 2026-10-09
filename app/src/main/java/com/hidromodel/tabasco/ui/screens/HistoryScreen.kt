package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
fun HistoryScreen(vm: AppViewModel) {
    val history = vm.history
    var selected by remember { mutableStateOf(listOf<Long>()) }
    // Limpia selecciones de corridas ya borradas
    val valid = selected.filter { id -> history.any { it.id == id } }

    Column(Modifier.fillMaxSize()) {
        AppHeader("Historial", "Corridas guardadas")
        ScreenBody {
            if (history.isEmpty()) {
                HCard {
                    Text("Aún no hay corridas guardadas", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Hm.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Configura un escenario, simúlalo y toca \"Guardar corrida\" en Resultados. " +
                            "Aquí podrás compararlas de dos en dos.",
                        fontSize = 14.sp, color = Hm.TextSecondary, lineHeight = 20.sp,
                    )
                }
            } else {
                if (valid.size == 2) {
                    val a = history.first { it.id == valid[0] }
                    val b = history.first { it.id == valid[1] }
                    HCard {
                        Text("Comparación de corridas", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Legend(listOf("${a.pointName} · ${a.createdAt}" to Hm.Teal, "${b.pointName} · ${b.createdAt}" to Hm.AlertFg))
                        Spacer(Modifier.height(8.dp))
                        LineChart(
                            series = listOf(
                                ChartSeries(a.series, Hm.Teal, filled = true),
                                ChartSeries(b.series, Hm.AlertFg),
                            ),
                            xLabels = xLabelsFor(maxOf(a.series.size, b.series.size), null),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Pico A: ${a.peak.m3s()} m³/s (día ${a.peakDay + 1}) · Pico B: ${b.peak.m3s()} m³/s (día ${b.peakDay + 1})",
                            fontSize = 12.sp, color = Hm.TextSecondary,
                        )
                    }
                } else {
                    Text("Marca dos corridas para compararlas.", fontSize = 13.sp, color = Hm.TextSecondary)
                }

                history.forEach { e ->
                    val checked = e.id in valid
                    HCard(borderColor = if (checked) Hm.Teal else Hm.Line) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    selected = if (on) (valid + e.id).takeLast(2) else valid - e.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Hm.Teal),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(e.pointName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                                Text("${e.mode} · ${e.createdAt}", fontSize = 12.sp, color = Hm.TextSecondary)
                            }
                            RiskChip(e.risk)
                            IconButton(onClick = { vm.deleteRun(e.id) }) {
                                Icon(Icons.Filled.Delete, "Eliminar corrida", tint = Hm.TextSecondary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Sparkline(e.series, riskFg(e.risk))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("${e.peak.m3s()} m³/s", style = NumStyle, fontSize = 18.sp, color = Hm.TextPrimary)
                                Text("pico el día ${e.peakDay + 1}", fontSize = 12.sp, color = Hm.TextSecondary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(e.summary, fontSize = 12.sp, color = Hm.TextSecondary)
                    }
                }
            }
        }
    }
}
