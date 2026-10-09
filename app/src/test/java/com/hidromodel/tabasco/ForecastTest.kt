package com.hidromodel.tabasco

import com.hidromodel.tabasco.data.CsvLoader
import com.hidromodel.tabasco.hydro.ReservoirModel
import com.hidromodel.tabasco.hydro.ScenarioEngine
import com.hidromodel.tabasco.model.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val rainDates = (0 until 16).map { today.plusDays(it.toLong()) }
    private val floodDates = (-2 until 16).map { today.plusDays(it.toLong()) }
    private val flood: List<Double?> = (0 until 18).map { 2000.0 + it * 90.0 }
    private val rain: List<Double?> = List(16) { if (it == 7) null else 10.0 }

    @Test
    fun caudal_inicial_es_el_del_dia_anterior() {
        val pf = ForecastBuilder.build(rainDates, rain, floodDates, flood)
        assertEquals(2090.0, pf.q0, 1e-9)
        assertEquals(0.0, pf.rain[7], 1e-9)   // lluvia faltante = 0
        assertNotNull(pf.glofas)
    }

    @Test
    fun hueco_en_glofas_descarta_la_referencia() {
        val f = flood.toMutableList().also { it[10] = null }
        assertNull(ForecastBuilder.build(rainDates, rain, floodDates, f).glofas)
    }

    @Test(expected = IllegalStateException::class)
    fun sin_caudal_inicial_falla() {
        ForecastBuilder.build(rainDates, rain, floodDates, List(18) { null })
    }

    @Test
    fun el_motor_simula_el_horizonte_del_pronostico() {
        val pts = File("src/main/assets/tabasco_lluvia_caudal.csv").inputStream().use { CsvLoader.load(it) }
        val cals = pts.map { ReservoirModel.calibrate(it.rain, it.flow) }
        val pf = ForecastBuilder.build(rainDates, rain, floodDates, flood)
        val fc = ForecastData(LocalDateTime.now(), pts.map { pf })
        val cfg = ScenarioConfig(pointIndex = 1, mode = RainMode.PRONOSTICO, k = cals[1].k.toFloat())
        val r = ScenarioEngine.run(pts, cals, cfg, fc)
        assertEquals(16, r.selected.horizon)
        assertEquals(today, r.selected.startDate)
    }
}
