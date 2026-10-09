package com.hidromodel.tabasco.hydro

import com.hidromodel.tabasco.model.*
import java.time.LocalDate

object ScenarioEngine {

    fun run(
        points: List<RiverPoint>,
        cals: List<Calibration>,
        cfg: ScenarioConfig,
        forecast: ForecastData? = null,
    ): ScenarioResult {
        val results = points.mapIndexed { i, p ->
            runPoint(p, cals[i], cfg, i == cfg.pointIndex, forecast?.points?.getOrNull(i))
        }
        return ScenarioResult(cfg, results)
    }

    private fun runPoint(
        p: RiverPoint,
        cal: Calibration,
        cfg: ScenarioConfig,
        selected: Boolean,
        fc: PointForecast?,
    ): PointResult {
        // Solo el punto seleccionado usa los deslizadores; los demás usan su calibración.
        val k = if (selected) cfg.k.toDouble() else cal.k
        val gain = if (selected) cal.gain * cfg.runoffC / C_REF else cal.gain

        if (cfg.mode == RainMode.PRONOSTICO) {
            val f = requireNotNull(fc) { "Falta el pronóstico para ${p.name}" }
            val sim = ReservoirModel.simulate(f.rain, k, gain, f.q0)
            return PointResult(p, k, gain, f.rain, sim, f.glofas, f.q0, f.startDate, null)
        }

        return if (cfg.mode == RainMode.MANUAL) {
            val rain = DoubleArray(HORIZON) { d ->
                if (d >= 1 && d < 1 + cfg.durationDays) cfg.intensity.toDouble() else 0.0
            }
            // Estado inicial: caudal de equilibrio para la lluvia media de los 30 días previos.
            val q0 = gain * cfg.antecedent.toDouble() / 30.0
            val sim = ReservoirModel.simulate(rain, k, gain, q0)
            PointResult(p, k, gain, rain, sim, null, q0, null, null)
        } else {
            val peakIdx = p.indexOf(cfg.eventPeak) ?: EVENT_PEAK_INDEX
            val start = (peakIdx - EVENT_PEAK_INDEX).coerceIn(0, p.flow.size - HORIZON)
            val rain = p.rain.copyOfRange(start, start + HORIZON)
            val obs = p.flow.copyOfRange(start, start + HORIZON)
            val q0 = obs[0]
            val sim = ReservoirModel.simulate(rain, k, gain, q0)
            PointResult(p, k, gain, rain, sim, obs, q0, p.dates[start], ReservoirModel.r2(sim, obs))
        }
    }
}

object HistoricEvents {
    /** Evento de referencia + los 4 mayores picos del punto (separados al menos 60 días). */
    fun forPoint(p: RiverPoint): List<HistoricEvent> {
        val out = ArrayList<HistoricEvent>()
        val refIdx = p.indexOf(REFERENCE_EVENT)
        if (refIdx != null) {
            out += HistoricEvent(REFERENCE_EVENT, "${REFERENCE_EVENT.es()} · referencia", p.flow[refIdx])
        }
        val order = p.flow.indices.sortedByDescending { p.flow[it] }
        val taken = ArrayList<Int>()
        refIdx?.let { taken += it }
        for (i in order) {
            if (out.size >= 5) break
            if (i < EVENT_PEAK_INDEX || i > p.flow.size - 10) continue
            if (taken.any { kotlin.math.abs(it - i) < 60 }) continue
            taken += i
            out += HistoricEvent(p.dates[i], "${p.dates[i].es()} · ${p.flow[i].m3s()} m³/s", p.flow[i])
        }
        return out
    }
}
