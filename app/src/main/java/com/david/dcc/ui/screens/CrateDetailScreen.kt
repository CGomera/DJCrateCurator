package com.david.dcc.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class CrateDetailVm(app: Application, private val crateId: Long) : AndroidViewModel(app) {
    private val db = AppDb.get(app)

    var crate by mutableStateOf<Crate?>(null); private set
    var tracks by mutableStateOf<List<Track>>(emptyList()); private set
    var loading by mutableStateOf(true); private set

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
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                CrateDetailVm(application, crateId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrateDetailScreen(
    crateId: Long,
    onNavigateUp: () -> Unit,
    vm: CrateDetailVm = viewModel(factory = CrateDetailVm.Factory(crateId))
) {
    val latestNavigateUp by rememberUpdatedState(onNavigateUp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.crate?.name ?: "Crate") },
                navigationIcon = {
                    IconButton(onClick = { latestNavigateUp() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (vm.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!vm.loading && vm.tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text("No hay pistas en este crate todavía", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.tracks) { track ->
                        ListItem(
                            headlineContent = { Text(track.title) },
                            supportingContent = {
                                val artist = track.artist?.takeIf { it.isNotBlank() }
                                if (artist != null) {
                                    Text(artist)
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}