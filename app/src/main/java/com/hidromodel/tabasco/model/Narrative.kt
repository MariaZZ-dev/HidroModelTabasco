package com.hidromodel.tabasco.model

/** Textos explicativos generados a partir de los números (sin afirmaciones que el modelo no respalde). */
object Narrative {

    fun riskTitle(r: RiskLevel) = when (r) {
        RiskLevel.NORMAL -> "Riesgo: Normal"
        RiskLevel.ALERTA -> "Riesgo: Alerta hidrológica"
        RiskLevel.CRITICO -> "Riesgo: Crítico"
    }

    fun riskText(pr: PointResult): String {
        val s = pr.point.stats
        return when (pr.risk) {
            RiskLevel.NORMAL ->
                "El caudal proyectado (${pr.peak.m3s()} m³/s) se mantiene por debajo del umbral de alerta (${s.alert.m3s()} m³/s)."
            RiskLevel.ALERTA ->
                "El caudal proyectado (${pr.peak.m3s()} m³/s) supera el umbral de alerta (${s.alert.m3s()} m³/s) durante ${pr.daysAlert} día(s), sin llegar al crítico (${s.critical.m3s()} m³/s)."
            RiskLevel.CRITICO ->
                "El caudal proyectado (${pr.peak.m3s()} m³/s) supera el umbral crítico (${s.critical.m3s()} m³/s) durante ${pr.daysCritical} día(s)."
        }
    }

    fun thresholdsNote(s: PointStats) =
        "Alerta = percentil 95 y Crítico = percentil 99 del caudal modelado 2000–2025. Son umbrales estadísticos, no cotas oficiales."

    fun diagnosis(pr: PointResult, cfg: ScenarioConfig): String {
        val s = pr.point.stats
        val pct = (pr.peak / s.mean - 1.0) * 100.0
        val rel = if (pct >= 0) "${pct.dec(0)}% por encima" else "${(-pct).dec(0)}% por debajo"
        return when (cfg.mode) {
            RainMode.MANUAL ->
                "Con ${cfg.intensity.toDouble().dec(0)} mm/día durante ${cfg.durationDays} día(s) y ${cfg.antecedent.toDouble().dec(0)} mm " +
                    "acumulados en los 30 días previos, el modelo proyecta un pico de ${pr.peak.m3s()} m³/s el día ${pr.peakDay + 1} " +
                    "en ${pr.point.name} ($rel del caudal medio de ${s.mean.m3s()} m³/s). " +
                    "La constante de retención k = ${pr.k.dec(1)} días hace que el caudal tarde en bajar después de que la lluvia termina."
            RainMode.HISTORICO -> {
                val obsPeak = pr.observed?.max() ?: 0.0
                val diff = if (obsPeak > 0) (pr.peak / obsPeak - 1.0) * 100.0 else 0.0
                val sign = if (diff >= 0) "+" else ""
                "Reproducción del evento con la lluvia real del punto desde ${pr.startDate?.es() ?: "—"}. " +
                    "El modelo estima un pico de ${pr.peak.m3s()} m³/s frente a ${obsPeak.m3s()} m³/s del caudal modelado de referencia (GloFAS), " +
                    "una diferencia de $sign${diff.dec(0)}%. Un solo punto de lluvia no captura toda la cuenca alta, por eso el modelo puede subestimar picos grandes."
            }
            RainMode.PRONOSTICO -> {
                val start = pr.startDate
                val total = pr.rain.sum()
                val maxRain = pr.rain.maxOrNull() ?: 0.0
                val maxIdx = pr.rain.indexOfFirst { it == maxRain }
                val peakDate = start?.plusDays(pr.peakDay.toLong())?.es() ?: "el día ${pr.peakDay + 1}"
                val rainDay = start?.plusDays(maxIdx.toLong())?.es() ?: "el día ${maxIdx + 1}"
                val ref = pr.observed?.let { o ->
                    val gp = o.max()
                    val d = if (gp > 0) (pr.peak / gp - 1.0) * 100.0 else 0.0
                    val sg = if (d >= 0) "+" else ""
                    " El pronóstico GloFAS de referencia para el mismo punto marca un pico de ${gp.m3s()} m³/s (diferencia del modelo: $sg${d.dec(0)}%)."
                } ?: ""
                "Con la lluvia pronosticada por Open-Meteo para los próximos ${pr.rain.size} días (${total.dec(0)} mm en total; " +
                    "máximo diario de ${maxRain.dec(0)} mm el $rainDay) y partiendo del caudal modelado actual de ${pr.q0.m3s()} m³/s, " +
                    "el modelo proyecta un pico de ${pr.peak.m3s()} m³/s el $peakDate.$ref " +
                    "Es una proyección educativa, no un aviso oficial: para alertas consulta a CONAGUA y a Protección Civil."
            }
        }
    }

    /** Suma máxima de lluvia en una ventana de w días dentro del escenario. */
    fun maxWindowRain(rain: DoubleArray, w: Int): Double {
        if (rain.isEmpty()) return 0.0
        val len = minOf(w, rain.size)
        var cur = 0.0
        for (i in 0 until len) cur += rain[i]
        var best = cur
        for (i in len until rain.size) {
            cur += rain[i] - rain[i - len]
            if (cur > best) best = cur
        }
        return best
    }
}
