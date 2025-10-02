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
    private var onCsvPicked: ((Uri) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onCsvPicked?.invoke(it) }
        }

        setContent {
            val snackbar = remember { SnackbarHostState() }
            App(
                snackbarHostState = snackbar,
                launchCsvPicker = {
                    picker.launch(arrayOf("text/*", "text/comma-separated-values", "text/csv"))
                },
                setOnCsvPicked = { cb -> onCsvPicked = cb }
            )
        }
    }
}
