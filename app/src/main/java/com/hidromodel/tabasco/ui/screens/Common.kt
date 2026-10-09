package com.hidromodel.tabasco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.model.esShort
import com.hidromodel.tabasco.ui.theme.Hm
import java.time.LocalDate

@Composable
fun ScreenBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun Legend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, color = Hm.TextSecondary)
            }
        }
    }
}

/** Etiquetas del eje X: números de día, o fechas si se conoce el día de inicio. */
fun xLabelsFor(n: Int, start: LocalDate?): List<Pair<Int, String>> {
    if (n < 2) return emptyList()
    val idx = listOf(0, (n - 1) / 4, (n - 1) / 2, 3 * (n - 1) / 4, n - 1).distinct()
    return idx.map { i -> i to (if (start != null) start.plusDays(i.toLong()).esShort() else "${i + 1}") }
}

/** Aviso cuando todavía no hay resultado (p. ej. modo Pronóstico sin datos descargados). */
@Composable
fun NoResultNotice(loading: Boolean) {
    if (loading) {
        com.hidromodel.tabasco.ui.components.LoadingBox()
    } else {
        ScreenBody {
            com.hidromodel.tabasco.ui.components.HCard {
                Text("Todavía no hay un resultado", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 16.sp, color = Hm.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Si elegiste la fuente Pronóstico, ve a Simular y toca \"Actualizar\" para descargar la lluvia prevista. " +
                        "También puedes cambiar a Manual o Histórico.",
                    fontSize = 14.sp, color = Hm.TextSecondary, lineHeight = 20.sp,
                )
            }
        }
    }
}
