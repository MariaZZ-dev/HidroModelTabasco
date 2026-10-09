package com.hidromodel.tabasco.hydro

import com.hidromodel.tabasco.model.Calibration

/**
 * Modelo de embalse lineal (un solo almacenamiento):
 *   Q[t] = Q[t-1] + (gain * P[t] - Q[t-1]) / k
 * P = lluvia diaria (mm), Q = caudal (m³/s), k = constante de retención (días),
 * gain = m³/s que produce cada mm/día sostenido de lluvia (incluye área y escurrimiento).
 */
object ReservoirModel {

    fun simulate(rain: DoubleArray, k: Double, gain: Double, q0: Double): DoubleArray {
        var q = q0
        return DoubleArray(rain.size) { t ->
            q += (gain * rain[t] - q) / k
            q
        }
    }

    /**
     * Calibración por mínimos cuadrados. Para cada k de la rejilla, el modelo es lineal en
     * (q0, gain): Q[t] = q0 * d[t] + gain * u[t], así que se resuelve en forma cerrada
     * y se elige la k con menor error cuadrático.
     */
    fun calibrate(
        rain: DoubleArray,
        obs: DoubleArray,
        kMin: Double = 5.0,
        kMax: Double = 120.0,
        step: Double = 0.1,
    ): Calibration {
        val n = rain.size
        val meanObs = obs.average()
        var sst = 0.0
        for (y in obs) sst += (y - meanObs) * (y - meanObs)

        var best = Calibration(30.0, 0.0, 0.0, Double.NEGATIVE_INFINITY)
        var bestSse = Double.POSITIVE_INFINITY
        val u = DoubleArray(n)
        val d = DoubleArray(n)

        var i = 0
        while (true) {
            val k = kMin + i * step
            if (k > kMax + 1e-9) break
            i++
            val r = 1.0 - 1.0 / k
            var uPrev = 0.0
            var dPrev = 1.0
            for (t in 0 until n) {
                uPrev = r * uPrev + rain[t] / k
                dPrev *= r
                u[t] = uPrev
                d[t] = dPrev
            }
            var sdd = 0.0; var sdu = 0.0; var suu = 0.0; var sdy = 0.0; var suy = 0.0
            for (t in 0 until n) {
                sdd += d[t] * d[t]; sdu += d[t] * u[t]; suu += u[t] * u[t]
                sdy += d[t] * obs[t]; suy += u[t] * obs[t]
            }
            val det = sdd * suu - sdu * sdu
            var q0 = 0.0
            var gain = 0.0
            if (kotlin.math.abs(det) > 1e-12) {
                q0 = (sdy * suu - suy * sdu) / det
                gain = (sdd * suy - sdu * sdy) / det
            }
            if (gain < 0.0) { gain = 0.0; q0 = if (sdd > 0) sdy / sdd else 0.0 }
            if (q0 < 0.0) { q0 = 0.0; gain = if (suu > 0) suy / suu else 0.0 }
            var sse = 0.0
            for (t in 0 until n) {
                val e = q0 * d[t] + gain * u[t] - obs[t]
                sse += e * e
            }
            if (sse < bestSse) {
                bestSse = sse
                best = Calibration(k, gain, q0, if (sst > 0) 1.0 - sse / sst else 0.0)
            }
        }
        return best
    }

    fun r2(sim: DoubleArray, obs: DoubleArray): Double {
        val m = obs.average()
        var sse = 0.0; var sst = 0.0
        for (i in obs.indices) {
            sse += (sim[i] - obs[i]) * (sim[i] - obs[i])
            sst += (obs[i] - m) * (obs[i] - m)
        }
        return if (sst > 0) 1.0 - sse / sst else 0.0
    }
}
