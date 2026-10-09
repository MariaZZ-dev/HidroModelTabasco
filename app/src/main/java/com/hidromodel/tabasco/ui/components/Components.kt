package com.hidromodel.tabasco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidromodel.tabasco.model.RiskLevel
import com.hidromodel.tabasco.ui.theme.Hm

/** Cifras con ancho fijo para que los números no "bailen" al animarse. */
val NumStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

fun riskFg(r: RiskLevel) = when (r) {
    RiskLevel.NORMAL -> Hm.NormalFg
    RiskLevel.ALERTA -> Hm.AlertFg
    RiskLevel.CRITICO -> Hm.CriticalFg
}
fun riskBg(r: RiskLevel) = when (r) {
    RiskLevel.NORMAL -> Hm.NormalBg
    RiskLevel.ALERTA -> Hm.AlertBg
    RiskLevel.CRITICO -> Hm.CriticalBg
}
fun riskBorder(r: RiskLevel) = when (r) {
    RiskLevel.NORMAL -> Hm.NormalBorder
    RiskLevel.ALERTA -> Hm.AlertBorder
    RiskLevel.CRITICO -> Hm.CriticalBorder
}

@Composable
fun HCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    containerColor: Color = Color.White,
    borderColor: Color = Hm.Line,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun RiskChip(risk: RiskLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(riskBg(risk))
            .border(1.dp, riskBorder(risk), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(riskFg(risk)))
        Spacer(Modifier.width(6.dp))
        Text(risk.label, color = riskFg(risk), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, fg: Color = Hm.Teal, bg: Color = Hm.TealSurface) {
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun AppHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Hm.TextPrimary)
                }
            } else {
                Box(
                    Modifier.padding(start = 4.dp).size(36.dp).clip(CircleShape).background(Hm.Teal),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.WaterDrop, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Hm.TextPrimary, maxLines = 1)
                Text(subtitle, fontSize = 12.sp, color = Hm.TextSecondary, maxLines = 1)
            }
        }
        HorizontalDivider(color = Hm.Line)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Hm.TextSecondary)
}

@Composable
fun MetricCard(label: String, value: String, sub: String, valueColor: Color, modifier: Modifier = Modifier) {
    HCard(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Hm.TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, style = NumStyle, fontSize = 26.sp, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 12.sp, color = Hm.TextSecondary)
    }
}

@Composable
fun ParamSlider(
    title: String,
    description: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Hm.TextPrimary)
                Text(description, fontSize = 12.sp, color = Hm.TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Hm.SurfaceDim)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text(valueText, style = NumStyle, fontSize = 15.sp, color = Hm.Teal) }
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Hm.Teal,
                activeTrackColor = Hm.Teal,
                inactiveTrackColor = Hm.Line,
            ),
        )
    }
}

@Composable
fun HDropdown(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, Hm.Line, shape)
                .clickable { open = true }.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                options.getOrElse(selected) { "" },
                Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Hm.TextPrimary,
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Hm.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}

@Composable
fun HSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(Hm.SurfaceDim).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (sel) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(o, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) Hm.Teal else Hm.TextSecondary)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Hm.Teal, contentColor = Color.White,
            disabledContainerColor = Hm.Line, disabledContentColor = Hm.TextDisabled,
        ),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Hm.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Hm.Teal),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Hm.Teal)
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

val NavItems = listOf(
    NavItem("inicio", "Inicio", Icons.Filled.Home),
    NavItem("configurar", "Simular", Icons.Filled.Tune),
    NavItem("resultados", "Resultados", Icons.Filled.Assessment),
    NavItem("historial", "Historial", Icons.Filled.History),
)

@Composable
fun BottomBar(selectedRoute: String, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavItems.forEach { item ->
            NavigationBarItem(
                selected = item.route == selectedRoute,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Hm.Teal,
                    selectedTextColor = Hm.Teal,
                    indicatorColor = Hm.TealSurface,
                    unselectedIconColor = Hm.TextSecondary,
                    unselectedTextColor = Hm.TextSecondary,
                ),
            )
        }
    }
}

/** Barra horizontal con etiqueta y valor (ventanas de lluvia, ranking de años). */
@Composable
fun HBarRow(label: String, valueText: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row {
            Text(label, Modifier.weight(1f), fontSize = 13.sp, color = Hm.TextSecondary)
            Text(valueText, style = NumStyle, fontSize = 13.sp, color = Hm.TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Hm.SurfaceDim)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}
