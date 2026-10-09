package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.model.*
import com.hidromodel.tabasco.ui.components.*
import com.hidromodel.tabasco.ui.theme.Hm
import java.time.format.DateTimeFormatter

@Composable
fun ConfigScreen(vm: AppViewModel, onStart: () -> Unit) {
    val data = vm.data
    val res = vm.result
    val cfg = vm.config

    // Si se entra en modo pronóstico y aún no hay datos, los pide automáticamente.
    LaunchedEffect(cfg.mode, data) {
        if (cfg.mode == RainMode.PRONOSTICO && data != null &&
            vm.forecast == null && !vm.forecastLoading && vm.forecastError == null
        ) vm.refreshForecast()
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader("Configurar escenario", "Lluvia y parámetros del modelo")
        if (data == null) {
            LoadingBox()
            return@Column
        }
        val point = data.points[cfg.pointIndex]
        val stats = point.stats
        val cal = data.calibrations[cfg.pointIndex]
        val events = data.events[cfg.pointIndex]

        ScreenBody {
            HCard {
                SectionLabel("Punto de monitoreo")
                Spacer(Modifier.height(8.dp))
                HDropdown(data.points.map { it.displayName }, cfg.pointIndex) { vm.selectPoint(it) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Promedio ${stats.mean.m3s()} · Alerta ${stats.alert.m3s()} · Crítico ${stats.critical.m3s()} m³/s",
                    fontSize = 12.sp, color = Hm.TextSecondary,
                )
            }

            SectionLabel("Fuente de la lluvia")
            HSegmented(
                listOf("Manual", "Histórico", "Pronóstico"),
                cfg.mode.ordinal,
                { vm.setMode(RainMode.values()[it]) },
            )

            when (cfg.mode) {
                RainMode.HISTORICO -> HCard {
                    SectionLabel("Evento a reproducir")
                    Spacer(Modifier.height(8.dp))
                    HDropdown(
                        events.map { it.label },
                        events.indexOfFirst { it.peakDate == cfg.eventPeak }.coerceAtLeast(0),
                    ) { vm.setEvent(events[it].peakDate) }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Se usa la lluvia real del punto durante el evento y se compara con el caudal modelado de referencia (GloFAS).",
                        fontSize = 12.sp, color = Hm.TextSecondary,
                    )
                }
                RainMode.PRONOSTICO -> ForecastCard(vm, cfg.pointIndex)
                RainMode.MANUAL -> Unit
            }

            if (res != null) PreviewCard(res.selected)

            if (cfg.mode == RainMode.MANUAL) {
                ParamSlider(
                    "Intensidad de lluvia", "Lluvia diaria sobre el punto",
                    "${cfg.intensity.toInt()} mm/día", cfg.intensity, 0f..300f, { vm.setIntensity(it) },
                )
                ParamSlider(
                    "Duración del evento", "Días de lluvia continua",
                    "${cfg.durationDays} días", cfg.durationDays.toFloat(), 1f..15f, { vm.setDuration(it) },
                )
                ParamSlider(
                    "Lluvia previa acumulada", "Total de los 30 días anteriores",
                    "${cfg.antecedent.toInt()} mm", cfg.antecedent, 50f..600f, { vm.setAntecedent(it) },
                )
            }
            ParamSlider(
                "Coeficiente de escurrimiento (C)", "Saturación del suelo; ${C_REF} = valor calibrado",
                cfg.runoffC.toDouble().dec(2), cfg.runoffC, 0.10f..0.95f, { vm.setRunoff(it) },
            )
            ParamSlider(
                "Constante de retención (k)", "Lentitud con que la cuenca desagua",
                "${cfg.k.toDouble().dec(1)} días", cfg.k, 5f..80f, { vm.setK(it) },
            )

            HCard(onClick = { vm.applyCalibration() }, containerColor = Hm.TealSurface, borderColor = Hm.TealSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sync, null, tint = Hm.Teal)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Calibrar con datos reales", fontWeight = FontWeight.SemiBold, color = Hm.Teal)
                        Text("Restaura k y C al ajuste de mínimos cuadrados 2000–2025", fontSize = 12.sp, color = Hm.TextSecondary)
                    }
                    InfoChip("k ${cal.k.dec(1)} · R² ${cal.r2.dec(2)}", fg = Hm.Teal, bg = Color.White)
                }
            }

            PrimaryButton("Iniciar simulación", onStart, icon = Icons.Filled.PlayArrow, enabled = res != null)
        }
    }
}

@Composable
private fun ForecastCard(vm: AppViewModel, pointIndex: Int) {
    val f = vm.forecast
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Pronóstico de lluvia (Open-Meteo)")
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        vm.forecastLoading -> "Consultando pronóstico…"
                        f != null -> "Actualizado: ${f.fetchedAt.format(fmt)}" + if (f.fromCache) " (guardado en el teléfono)" else ""
                        else -> "Sin pronóstico todavía"
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Hm.TextPrimary,
                )
            }
            if (vm.forecastLoading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Hm.Teal, strokeWidth = 2.dp)
            } else {
                Text(
                    "Actualizar", color = Hm.Teal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clickable { vm.refreshForecast() }.padding(8.dp),
                )
            }
        }
        vm.forecastError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = Hm.AlertFg, lineHeight = 17.sp)
        }
        val pf = f?.points?.getOrNull(pointIndex)
        if (pf != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Próximos ${pf.rain.size} días desde ${pf.startDate.es()}: ${pf.rain.sum().dec(0)} mm de lluvia prevista " +
                    "(máximo diario ${(pf.rain.maxOrNull() ?: 0.0).dec(0)} mm). " +
                    "Caudal inicial: ${pf.q0.m3s()} m³/s, el caudal modelado más reciente (GloFAS).",
                fontSize = 12.sp, color = Hm.TextSecondary, lineHeight = 17.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Es una proyección educativa del modelo, no un aviso oficial.",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Hm.TextSecondary,
        )
    }
}

@Composable
private fun PreviewCard(pr: PointResult) {
    val s = pr.point.stats
    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Respuesta rápida", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary, modifier = Modifier.weight(1f))
            RiskChip(pr.risk)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text("Caudal pico estimado", fontSize = 12.sp, color = Hm.TextSecondary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(pr.peak.m3s(), style = NumStyle, fontSize = 32.sp, color = riskFg(pr.risk))
                    Spacer(Modifier.width(4.dp))
                    Text("m³/s", fontSize = 13.sp, color = Hm.TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Día del pico", fontSize = 12.sp, color = Hm.TextSecondary)
                Text("Día ${pr.peakDay + 1}", style = NumStyle, fontSize = 22.sp, color = Hm.TextPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))
        RiskZoneBar(pr.peak, s.mean, s.alert, s.critical)
        Row(Modifier.fillMaxWidth()) {
            Text("Prom ${s.mean.m3s()}", fontSize = 11.sp, color = Hm.TextSecondary, modifier = Modifier.weight(1f))
            Text("Alerta ${s.alert.m3s()}", fontSize = 11.sp, color = Hm.AlertFg, modifier = Modifier.weight(1f))
            Text("Crítico ${s.critical.m3s()}", fontSize = 11.sp, color = Hm.CriticalFg)
        }
    }
}
