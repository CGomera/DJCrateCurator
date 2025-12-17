package com.david.dcc.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.Crate
import com.david.dcc.data.model.Track
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.david.dcc.ui.screens.HomeVm

class CrateDetailVm(app: Application, private val crateId: Long) : AndroidViewModel(app) {
    private val db = AppDb.get(app)

    var crate by mutableStateOf<Crate?>(null)
        private set
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        loading = true
        crate = db.crateDao().byId(crateId)
        tracks = db.crateDao().tracksInCrate(crateId)
        loading = false
    }

    companion object {
        fun Factory(crateId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                CrateDetailVm(application, crateId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrateDetailScreen(
    crateId: Long,
    onNavigateUp: () -> Unit,
    snackbarHostState: SnackbarHostState,
    launchCsvExporter: (String) -> Unit,
    registerOnCsvExported: ((Uri) -> Unit) -> Unit,
    launchJsonExporter: (String) -> Unit,
    registerOnJsonExported: ((Uri) -> Unit) -> Unit,
    vm: CrateDetailVm = viewModel(factory = CrateDetailVm.Factory(crateId)),
    homeVm: HomeVm = viewModel(),) {
    val latestNavigateUp by rememberUpdatedState(onNavigateUp)
    val snackbarHost by rememberUpdatedState(snackbarHostState)
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(crateId) { vm.refresh() }

    fun requestCsvExport(crate: Crate) {
        registerOnCsvExported { uri ->
            homeVm.exportCrateToCsv(crate.id, uri) { count ->
                snackbarScope.launch { snackbarHost.showSnackbar("Exportadas $count pistas a CSV") }
            }
        }
        launchCsvExporter("${crate.name}.csv")
    }

    fun requestJsonExport(crate: Crate) {
        registerOnJsonExported { uri ->
            homeVm.exportCrateToJson(crate.id, uri) {
                snackbarScope.launch { snackbarHost.showSnackbar("Exportadas $it pistas en JSON multi-formato") }
            }
        }
        launchJsonExporter("${crate.name}_pro.json")
    }

    fun shareCrate(crate: Crate) {
        snackbarScope.launch {
            val shareText = homeVm.buildCrateShareText(crate.id)
            if (shareText.isNullOrBlank()) {
                snackbarHost.showSnackbar("No se pudo generar la información del crate")
                return@launch
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Crate ${crate.name}")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(Intent.createChooser(intent, "Compartir crate"))
            }.onFailure {
                snackbarHost.showSnackbar("No se encontró ninguna app para compartir")
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.crate?.name ?: "Crate") },
                navigationIcon = {
                    IconButton(onClick = { latestNavigateUp() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }

                },
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (vm.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            vm.crate?.let { crate ->
                Spacer(Modifier.size(12.dp))
                Text("Acciones del crate", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                     AssistChip(
                        onClick = { requestCsvExport(crate) },
                        label = { Text("Exportar CSV") },
                        leadingIcon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
                    )
                    AssistChip(
                        onClick = { requestJsonExport(crate) },
                        label = { Text("Exportar crate") },
                        leadingIcon = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null) },
                    )
                    AssistChip(
                        onClick = { shareCrate(crate) },
                        label = { Text("Compartir crate") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    )
                }
                Spacer(Modifier.size(12.dp))
            }
            if (!vm.loading && vm.tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No hay pistas en este crate todavía", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.tracks) { track ->
                        ListItem(
                            headlineContent = { Text(track.title) },
                            supportingContent = {
                                val artist = track.artist.takeIf { it.isNotBlank() }
                                if (artist != null) {
                                    Text(artist)
                                }
                            },
                        )
                        Divider()
                    }
                    item { Spacer(Modifier.size(16.dp)) }
                }
            }
        }
    }
}

