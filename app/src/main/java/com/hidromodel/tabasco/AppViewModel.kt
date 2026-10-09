package com.hidromodel.tabasco

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hidromodel.tabasco.data.CsvLoader
import com.hidromodel.tabasco.data.ForecastRepository
import com.hidromodel.tabasco.data.ForecastStore
import com.hidromodel.tabasco.data.HistoryStore
import com.hidromodel.tabasco.hydro.HistoricEvents
import com.hidromodel.tabasco.hydro.ReservoirModel
import com.hidromodel.tabasco.hydro.ScenarioEngine
import com.hidromodel.tabasco.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppData(
    val points: List<RiverPoint>,
    val calibrations: List<Calibration>,
    val events: List<List<HistoricEvent>>,
) {
    val totalRecords: Int get() = points.sumOf { it.flow.size }
    val zapataIndex: Int get() = points.indexOfFirst { it.name.contains("Emiliano") }.let { if (it < 0) 0 else it }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = HistoryStore(app)
    private val forecastStore = ForecastStore(app)

    var data by mutableStateOf<AppData?>(null); private set
    var loadError by mutableStateOf<String?>(null); private set
    var config by mutableStateOf(ScenarioConfig()); private set
    var history by mutableStateOf<List<HistoryEntry>>(emptyList()); private set
    var message by mutableStateOf<String?>(null); private set
    var forecast by mutableStateOf<ForecastData?>(null); private set
    var forecastLoading by mutableStateOf(false); private set
    var forecastError by mutableStateOf<String?>(null); private set

    /** Resultado del escenario actual; se recalcula solo cuando cambian los parámetros. */
    val result: ScenarioResult? by derivedStateOf {
        val d = data
        val f = forecast?.takeIf { d != null && it.points.size == d.points.size }
        when {
            d == null -> null
            config.mode == RainMode.PRONOSTICO && f == null -> null
            else -> ScenarioEngine.run(d.points, d.calibrations, config, f)
        }
    }

    init {
        history = store.load()
        forecast = forecastStore.load()
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.Default) {
                    val pts = getApplication<Application>().assets
                        .open("tabasco_lluvia_caudal.csv").use { CsvLoader.load(it) }
                    val cals = pts.map { ReservoirModel.calibrate(it.rain, it.flow) }
                    AppData(pts, cals, pts.map { HistoricEvents.forPoint(it) })
                }
                data = loaded
                val i = loaded.zapataIndex
                config = ScenarioConfig(pointIndex = i, k = loaded.calibrations[i].k.toFloat())
            } catch (e: Exception) {
                loadError = e.message ?: "No se pudieron cargar los datos"
            }
        }
    }

    private fun update(f: (ScenarioConfig) -> ScenarioConfig) { config = f(config) }

    fun selectPoint(i: Int) {
        val d = data ?: return
        update {
            it.copy(
                pointIndex = i,
                k = d.calibrations[i].k.toFloat(),
                runoffC = C_REF.toFloat(),
                eventPeak = d.events[i].firstOrNull()?.peakDate ?: REFERENCE_EVENT,
            )
        }
    }
    fun setMode(m: RainMode) {
        update { it.copy(mode = m) }
        if (m == RainMode.PRONOSTICO) refreshForecast()
    }

    /** Atajo del Inicio: abre el simulador en modo pronóstico. */
    fun openForecast() = setMode(RainMode.PRONOSTICO)

    /** Descarga el pronóstico; si falla, conserva el último guardado. */
    fun refreshForecast() {
        val d = data ?: return
        if (forecastLoading) return
        forecastLoading = true
        forecastError = null
        viewModelScope.launch {
            try {
                val f = ForecastRepository.fetch(d.points)
                forecast = f
                forecastStore.save(f)
            } catch (e: Exception) {
                forecastError = if (forecast != null)
                    "No se pudo actualizar (sin conexión o servicio no disponible). Se muestra el último pronóstico guardado."
                else
                    "No se pudo obtener el pronóstico. Revisa tu conexión a internet e inténtalo de nuevo."
            } finally {
                forecastLoading = false
            }
        }
    }
    fun setIntensity(v: Float) = update { it.copy(intensity = Math.round(v).toFloat()) }
    fun setDuration(v: Float) = update { it.copy(durationDays = Math.round(v)) }
    fun setAntecedent(v: Float) = update { it.copy(antecedent = Math.round(v / 5f) * 5f) }
    fun setRunoff(v: Float) = update { it.copy(runoffC = Math.round(v * 100f) / 100f) }
    fun setK(v: Float) = update { it.copy(k = Math.round(v * 10f) / 10f) }
    fun setEvent(d: LocalDate) = update { it.copy(eventPeak = d) }

    /** Restaura k y C a los valores calibrados con los datos reales del punto seleccionado. */
    fun applyCalibration() {
        val d = data ?: return
        update { it.copy(k = d.calibrations[it.pointIndex].k.toFloat(), runoffC = C_REF.toFloat()) }
    }

    /** Atajo del Inicio: reproduce el evento del 7 nov 2020 en Emiliano Zapata. */
    fun replayReference() {
        val d = data ?: return
        val i = d.zapataIndex
        config = ScenarioConfig(
            pointIndex = i, mode = RainMode.HISTORICO, eventPeak = REFERENCE_EVENT,
            k = d.calibrations[i].k.toFloat(),
        )
    }

    fun saveRun() {
        val r = result ?: return
        val pr = r.selected
        val cfg = r.config
        val summary = when (cfg.mode) {
            RainMode.MANUAL ->
                "${cfg.intensity.toInt()} mm/día × ${cfg.durationDays} d · previa ${cfg.antecedent.toInt()} mm · C ${cfg.runoffC} · k ${cfg.k}"
            RainMode.HISTORICO -> "Evento ${cfg.eventPeak.es()} · k ${cfg.k}"
            RainMode.PRONOSTICO -> "Pronóstico desde ${pr.startDate?.es() ?: "—"} · k ${cfg.k}"
        }
        val entry = HistoryEntry(
            id = System.currentTimeMillis(),
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            pointName = pr.point.name, river = pr.point.river,
            mode = when (cfg.mode) { RainMode.MANUAL -> "Manual"; RainMode.HISTORICO -> "Histórico"; RainMode.PRONOSTICO -> "Pronóstico" },
            summary = summary, peak = pr.peak, peakDay = pr.peakDay, risk = pr.risk,
            series = pr.simulated, alert = pr.point.stats.alert, critical = pr.point.stats.critical,
        )
        history = listOf(entry) + history
        store.save(history)
        message = "Corrida guardada en el historial"
    }

    fun deleteRun(id: Long) {
        history = history.filterNot { it.id == id }
        store.save(history)
    }

    fun consumeMessage() { message = null }
}
