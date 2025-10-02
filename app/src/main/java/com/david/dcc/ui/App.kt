package com.david.dcc.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.david.dcc.ui.screens.HomeScreen

@Composable
fun App(
    snackbarHostState: SnackbarHostState,
    launchCsvPicker: () -> Unit,
    setOnCsvPicked: ((Uri) -> Unit) -> Unit     // 👈 CORRECTO: recibe una función (Uri)->Unit
) {
    val nav = rememberNavController()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable(route = "home") {
                HomeScreen(
                    launchCsvPicker = TODO(),
                    setOnCsvPicked = TODO(),
                    vm = TODO()
                    // si de momento no usas estos callbacks en la pantalla,
                    // puedes quitarlos de HomeScreen() o dejarlos para futuro
                )
            }
        }
    }
}
