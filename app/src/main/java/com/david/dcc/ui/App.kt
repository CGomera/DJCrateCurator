//comentario para hacer commit
package com.david.dcc.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.david.dcc.ui.screens.HomeScreen

@Composable
fun App(
    snackbarHostState: SnackbarHostState,
    launchCsvPicker: () -> Unit,
    setOnCsvPicked: (cb: (Uri) -> Unit) -> Unit
) {
    val nav = rememberNavController()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    snackbarHostState = snackbarHostState,
                    launchCsvPicker = launchCsvPicker,
                    registerOnCsvPicked = setOnCsvPicked,
                    vm = viewModel()
                )
            }
        }
    }
}

