package com.hidromodel.tabasco.model

import java.time.LocalDate
import java.util.Locale

private val MESES = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

fun Double.m3s(): String = String.format(Locale.US, "%,.0f", this)
fun Double.dec(n: Int = 1): String = String.format(Locale.US, "%.${n}f", this)
fun LocalDate.es(): String = "$dayOfMonth ${MESES[monthValue - 1]} $year"
fun LocalDate.esShort(): String = "$dayOfMonth ${MESES[monthValue - 1]}"
fun monthAbbr(m: Int): String = MESES[m - 1]
