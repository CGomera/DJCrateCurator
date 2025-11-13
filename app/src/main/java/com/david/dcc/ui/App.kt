package com.david.dcc.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.navigation.navArgument
import com.david.dcc.ui.screens.CrateDetailScreen

import com.david.dcc.ui.screens.HomeScreen
import com.david.dcc.ui.screens.HomeVm


@Composable
fun App(
    snackbarHostState: SnackbarHostState,
    launchCsvPicker: () -> Unit,
    setOnCsvPicked: (cb: (Uri) -> Unit) -> Unit,
    launchCsvExporter: (String) -> Unit,
    setOnCsvExported: (cb: (Uri) -> Unit) -> Unit,
    launchJsonExporter: (String) -> Unit,
    setOnJsonExported: (cb: (Uri) -> Unit) -> Unit,
    launchSetlistExporter: (String) -> Unit,
    setOnSetlistExported: (cb: (Uri) -> Unit) -> Unit,
) {
    val nav = rememberNavController()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
        ) {
            composable("home") {
                val homeVm: HomeVm = viewModel()
                HomeScreen(
                    snackbarHostState = snackbarHostState,
                    launchCsvPicker = launchCsvPicker,
                    registerOnCsvPicked = setOnCsvPicked,
                    launchCsvExporter = launchCsvExporter,
                    registerOnCsvExported = setOnCsvExported,
                    launchJsonExporter = launchJsonExporter,
                    registerOnJsonExported = setOnJsonExported,
                    launchSetlistExporter = launchSetlistExporter,
                    registerOnSetlistExported = setOnSetlistExported,
                    onOpenCrate = { crateId -> nav.navigate("crate/$crateId") },
                    vm = homeVm,
                )
            }

            composable(
                route = "crate/{crateId}",
                arguments = listOf(navArgument("crateId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val crateId = backStackEntry.arguments?.getLong("crateId") ?: return@composable
                CrateDetailScreen(
                    crateId = crateId,
                    onNavigateUp = { nav.popBackStack() },
                )
            }
        }
    }
}