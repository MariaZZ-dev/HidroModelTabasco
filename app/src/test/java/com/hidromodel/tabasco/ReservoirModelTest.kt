package com.hidromodel.tabasco

import com.hidromodel.tabasco.data.CsvLoader
import com.hidromodel.tabasco.hydro.ReservoirModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservoirModelTest {

    private val csv = File("src/main/assets/tabasco_lluvia_caudal.csv")

    @Test
    fun carga_tres_puntos_con_datos_completos() {
        val pts = csv.inputStream().use { CsvLoader.load(it) }
        assertEquals(3, pts.size)
        pts.forEach { assertEquals(9497, it.flow.size) }
    }

    @Test
    fun umbrales_de_emiliano_zapata() {
        val p = csv.inputStream().use { CsvLoader.load(it) }.first { it.name.contains("Emiliano") }
        assertEquals(5271.0, p.stats.alert, 5.0)
        assertEquals(7046.0, p.stats.critical, 5.0)
    }

    @Test
    fun calibracion_recupera_parametros_conocidos() {
        val p = csv.inputStream().use { CsvLoader.load(it) }.first { it.name.contains("Emiliano") }
        val c = ReservoirModel.calibrate(p.rain, p.flow)
        assertEquals(29.3, c.k, 0.3)
        assertTrue(c.r2 > 0.8)
    }

    @Test
    fun sin_lluvia_el_caudal_decae() {
        val sim = ReservoirModel.simulate(DoubleArray(20), k = 30.0, gain = 100.0, q0 = 1000.0)
        assertTrue(sim.last() < sim.first())
    }
}
