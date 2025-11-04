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

    private var onCsvPicked: (Uri) -> Unit = {}
    private var onCsvExported: (Uri) -> Unit = {}
    private var onJsonExported: (Uri) -> Unit = {}
    private var onSetlistExported: (Uri) -> Unit = {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val picker = registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let(onCsvPicked)
        }

        val csvExporter = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri: Uri? ->
            uri?.let(onCsvExported)
            onCsvExported = {}
        }

        val jsonExporter = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            uri?.let(onJsonExported)
            onJsonExported = {}
        }


        val setlistExporter = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri: Uri? ->
            uri?.let(onSetlistExported)
            onSetlistExported = {}
        }


        setContent {
            val snackbar = remember { SnackbarHostState() }

            App(
                snackbarHostState = snackbar,
                launchCsvPicker = {
                    picker.launch(
                        arrayOf("text/*", "text/csv", "text/comma-separated-values"),
                    )
                },
                setOnCsvPicked = { callback -> onCsvPicked = callback },
                launchCsvExporter = { suggestedName -> csvExporter.launch(suggestedName) },
                setOnCsvExported = { callback -> onCsvExported = callback },
                launchJsonExporter = { suggestedName -> jsonExporter.launch(suggestedName) },
                setOnJsonExported = { callback -> onJsonExported = callback },
                launchSetlistExporter = { suggestedName -> setlistExporter.launch(suggestedName) },
                setOnSetlistExported = { callback -> onSetlistExported = callback },
            )
        }

    }
}
