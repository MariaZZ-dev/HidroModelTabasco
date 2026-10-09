package com.hidromodel.tabasco.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hidromodel.tabasco.AppViewModel
import com.hidromodel.tabasco.ui.components.BottomBar
import com.hidromodel.tabasco.ui.screens.*
import com.hidromodel.tabasco.ui.theme.Hm

@Composable
fun HidroApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "inicio"
    val barRoute = when (route) {
        "simulacion" -> "configurar"
        "datos" -> "inicio"
        else -> route
    }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    fun goTab(r: String) {
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Hm.Canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomBar(barRoute) { goTab(it) } },
    ) { inner ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(inner)) {
            composable("inicio") {
                HomeScreen(
                    vm,
                    onNew = { goTab("configurar") },
                    onData = { nav.navigate("datos") },
                    onForecast = { vm.openForecast(); goTab("configurar") },
                    onReplay = { vm.replayReference(); goTab("configurar") },
                )
            }
            composable("configurar") { ConfigScreen(vm, onStart = { nav.navigate("simulacion") }) }
            composable("simulacion") {
                SimulationScreen(vm, onBack = { nav.popBackStack() }, onResults = { goTab("resultados") })
            }
            composable("resultados") { ResultsScreen(vm, onCompare = { goTab("historial") }) }
            composable("historial") { HistoryScreen(vm) }
            composable("datos") { DataScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
