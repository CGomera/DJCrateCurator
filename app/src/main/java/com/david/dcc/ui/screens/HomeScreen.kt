package com.david.dcc.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.Crate
import com.david.dcc.data.repo.ImportRepository
import kotlinx.coroutines.launch

class HomeVm(app: Application): AndroidViewModel(app) {
    private val db = AppDb.get(app)
    private val importRepo = ImportRepository(app, db)

    var crates by mutableStateOf<List<Crate>>(emptyList()); private set
    var loading by mutableStateOf(false); private set

    init { refresh() }

    fun refresh() = viewModelScope.launch { crates = db.crateDao().all() }

    fun createCrate(name: String) = viewModelScope.launch {
        db.crateDao().insert(Crate(name = name))
        refresh()
    }

    fun importCsvTo(crateId: Long, uri: Uri, onDone: (Int)->Unit) = viewModelScope.launch {
        loading = true
        val n = importRepo.importCsvToCrate(uri, crateId)
        loading = false
        onDone(n)
    }
}

@Composable
fun HomeScreen(
    launchCsvPicker: () -> Unit,
    setOnCsvPicked: (Uri) -> Unit,
    vm: HomeVm = viewModel()
) {
    var newCrateName by remember { mutableStateOf("") }
    var selectedCrate by remember { mutableStateOf<Crate?>(null) }

    Column(Modifier.padding(16.dp)) {
        Text("Crates", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = newCrateName,
                onValueChange = { newCrateName = it },
                label = { Text("Nombre del crate") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newCrateName.isNotBlank()) {
                    vm.createCrate(newCrateName); newCrateName = ""
                }
            }) { Text("Crear") }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(vm.crates) { crate ->
                ElevatedCard(onClick = { selectedCrate = crate }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(crate.name, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = {
                            selectedCrate = crate
                            launchCsvPicker()
                        }) { Text("Importar CSV") }
                    }
                }
            }
        }


        if (vm.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
