# Modelo y metodología (apoyo para el reporte)

## Sistema modelado
Respuesta del caudal de un río a la lluvia acumulada en su cuenca, en tres puntos:
Grijalva–Villahermosa, Usumacinta–Emiliano Zapata y Usumacinta–Tenosique.

## Datos
- Lluvia diaria (mm) y caudal diario (m³/s), 2000–2025, de Open-Meteo (API histórica y API de inundaciones, que usa GloFAS).
- **El caudal es modelado, no medido** por estaciones. Se usa como referencia hidrológica para calibrar.
- Cada punto es una celda de ~5 km elegida automáticamente como la de mayor caudal medio cerca de la coordenada objetivo.

## Modelo (simulación continua)
Embalse lineal de un solo almacenamiento:

    Q[t] = Q[t-1] + (g · P[t] − Q[t-1]) / k

- P: lluvia diaria (mm). Q: caudal (m³/s). k: constante de retención (días). g: ganancia (m³/s por mm/día sostenido).
- Escenario manual: lluvia constante `intensidad` durante `duración` días, empezando el día 2.
  Estado inicial: Q0 = g · (lluvia previa de 30 días / 30), el caudal de equilibrio de la lluvia media previa.
- El deslizador de coeficiente de escurrimiento C escala la ganancia: g_efectiva = g_calibrada · C / 0.68.
  C = 0.68 reproduce la calibración.

## Calibración
Mínimos cuadrados sobre toda la serie 2000–2025. Para cada k (5 a 120 días, paso 0.1) el modelo es lineal en
(Q0, g), así que se resuelve en forma cerrada y se elige la k con menor error cuadrático.

| Punto | k (días) | g | R² |
|---|---|---|---|
| Grijalva – Villahermosa | 34.5 | 280 | 0.70 |
| Usumacinta – Emiliano Zapata | 29.3 | 502 | 0.82 |
| Usumacinta – Tenosique | 33.2 | 452 | 0.77 |

## Modo pronóstico
- Lluvia: pronóstico diario de Open-Meteo a 16 días en la celda de cada punto (la misma que se usó para calibrar).
- Caudal inicial: caudal modelado GloFAS del día anterior al primer día pronosticado.
- El modelo de embalse se corre con esa lluvia y se compara con el pronóstico de caudal GloFAS del mismo punto.
- Es una proyección educativa: el pronóstico de lluvia tiene incertidumbre y el modelo es simple. No sustituye avisos oficiales (CONAGUA, Protección Civil).

## Riesgo (clasificación por umbrales)
- Alerta = percentil 95 del caudal 2000–2025. Crítico = percentil 99.
- Son umbrales estadísticos definidos por el proyecto, no cotas oficiales de inundación.

## Validación
- Evento de referencia: 7 nov 2020 (pico del caudal modelado en los tres puntos). Confirmar la fecha con fuentes oficiales.
- En modo "evento histórico" el modelo se corre con la lluvia real y se compara contra el caudal modelado.
  El modelo subestima picos grandes (p. ej. ~6,100 vs ~9,980 m³/s en Emiliano Zapata) y en la ventana de Villahermosa el ajuste es pobre.

## Limitaciones (declararlas en el reporte)
1. Un solo punto de lluvia no representa toda la cuenca alta (Chiapas y Guatemala aportan agua al Usumacinta).
   Mejora futura: promediar la lluvia de varios puntos de la cuenca.
2. Las presas (Peñitas, Malpaso) regulan los ríos y no están en el modelo.
3. Caudal modelado ≠ medido. 4. Sin dato de cotas ni de áreas inundadas.
5. La lluvia constante del escenario manual es una simplificación.

## Mapeo con la materia
- Unidad 1 (simulación de eventos): embalse lineal, escenarios y eventos históricos.
- Unidad 2 (visualización): hidrograma, medidor de nivel, semáforo de riesgo, series históricas.
- Unidad 3 (predicción): calibración por regresión, clasificación por umbrales, ventanas de lluvia acumulada.
- Unidad 4 (metodología): este documento (sistema, datos, modelo, calibración, validación, limitaciones).
