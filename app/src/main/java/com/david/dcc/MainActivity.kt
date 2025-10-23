package com.david.dcc

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.david.dcc.ui.App

class MainActivity : ComponentActivity() {

    // En vez de nullable, dale un valor por defecto que no hace nada:
    private var onCsvPicked: ((Uri) -> Unit) = {}
    private var onCsvExported: ((Uri) -> Unit) = {}
    private var onJsonExported: ((Uri) -> Unit) = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val picker = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { onCsvPicked(it) }   // <- sin '!!' ni nullables
        }

        val csvExporter = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri: Uri? ->
            uri?.let { onCsvExported(it) }
            onCsvExported = {}
        }

        val jsonExporter = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let { onJsonExported(it) }
            onJsonExported = {}
        }



        setContent {
            val snackbar = remember { SnackbarHostState() }

            App(
                snackbarHostState = snackbar,
                launchCsvPicker = {
                    picker.launch(
                        arrayOf("text/*", "text/csv", "text/comma-separated-values")
                    )
                },
                setOnCsvPicked = { cb -> onCsvPicked = cb }, // se asigna desde la UI
                launchCsvExporter = { suggestedName -> csvExporter.launch(suggestedName) },
                setOnCsvExported = { cb -> onCsvExported = cb },
                launchJsonExporter = { suggestedName -> jsonExporter.launch(suggestedName) },
                setOnJsonExported = { cb -> onJsonExported = cb }            )
        }
    }
}
