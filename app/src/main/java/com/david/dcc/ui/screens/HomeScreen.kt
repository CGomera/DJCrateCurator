package com.david.dcc.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.Crate
import com.david.dcc.data.model.CrateWithTrackCount
import com.david.dcc.data.model.Setlist
import com.david.dcc.data.model.SetlistItem
import com.david.dcc.data.model.SetlistTrackView
import com.david.dcc.data.model.SetlistWithCount
import com.david.dcc.data.model.Tag
import com.david.dcc.data.model.Track
import com.david.dcc.data.model.TrackTag
import com.david.dcc.data.repo.ImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.absoluteValue


class HomeVm(app: Application) : AndroidViewModel(app) {
        private val db = AppDb.get(app)
        private val importRepo = ImportRepository(app, db)
        private val tagDao = db.tagDao()

        var crateSummaries by mutableStateOf<List<CrateWithTrackCount>>(emptyList()); private set
        var selectedCrate by mutableStateOf<Crate?>(null); private set
        var crateTracks by mutableStateOf<List<Track>>(emptyList()); private set
        var recommendedTracks by mutableStateOf<List<Track>>(emptyList()); private set
        var setlists by mutableStateOf<List<SetlistWithCount>>(emptyList()); private set
        var setlistDetails by mutableStateOf<Map<Long, List<SetlistTrackView>>>(emptyMap()); private set
        var loading by mutableStateOf(false); private set

        var allTags by mutableStateOf<List<Tag>>(emptyList()); private set
        var trackTags by mutableStateOf<Map<Long, List<Tag>>>(emptyMap()); private set

        var searchQuery by mutableStateOf("")
        var genreFilter by mutableStateOf<String?>(null)
        var artistFilter by mutableStateOf<String?>(null)
        var bpmBounds by mutableStateOf<ClosedFloatingPointRange<Float>?>(null); private set
        var bpmRange by mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
        var availableGenres by mutableStateOf<List<String>>(emptyList()); private set
        var availableArtists by mutableStateOf<List<String>>(emptyList()); private set


            init {
                refreshAll()
                viewModelScope.launch { refreshAllTags() }
            }

                fun refreshAll() = viewModelScope.launch {
                    loading = true
                    crateSummaries = db.crateDao().allWithCounts()
                    setlists = db.setlistDao().allWithCounts()
                    refreshAllTags()
                    loading = false
                    selectedCrate?.let { crate ->
                        crateSummaries.firstOrNull { it.crate.id == crate.id }?.crate?.let { selectCrate(it) }
                    }
                }

                fun refreshCratesOnly() = viewModelScope.launch {
                    crateSummaries = db.crateDao().allWithCounts()
                    selectedCrate?.let { crate ->
                        crateSummaries.firstOrNull { it.crate.id == crate.id }?.crate?.let { selectCrate(it) }
                    }
                }

                fun createCrate(name: String, onDone: () -> Unit = {}) = viewModelScope.launch {
                    if (name.isBlank()) return@launch
                    db.crateDao().insert(Crate(name = name.trim()))
                    refreshCratesOnly()
                    onDone()
                }

                fun selectCrate(crate: Crate) = viewModelScope.launch {
                    selectedCrate = crate
                    searchQuery = ""
                    genreFilter = null
                    artistFilter = null
                    bpmBounds = null
                    bpmRange = null

                    loading = true
                    val tracks = db.crateDao().tracksInCrate(crate.id)
                    crateTracks = tracks
                    refreshTrackTags(tracks.map { it.id })
                    loading = false

                    availableGenres = tracks.mapNotNull { it.genre?.takeIf { g -> g.isNotBlank() }?.trim() }.distinct().sorted()
                    availableArtists = tracks.mapNotNull { it.artist.takeIf { a -> a.isNotBlank() } }.distinct().sorted()
                    val bpms = tracks.mapNotNull { it.bpm?.toFloat() }
                    if (bpms.isNotEmpty()) {
                        val min = bpms.minOrNull() ?: 0f
                        val max = bpms.maxOrNull() ?: min
                        val range = min..max
                        bpmBounds = range
                        bpmRange = range
                    }

                    loadRecommendations(tracks)
                }

                private fun loadRecommendations(tracks: List<Track>) = viewModelScope.launch {
                    if (tracks.isEmpty()) {
                        recommendedTracks = emptyList()
                        return@launch
                    }
                    val crateTrackIds = tracks.map { it.id }.toSet()
                    val genres = tracks.mapNotNull { it.genre?.lowercase() }.toSet()
                    val bpmValues = tracks.mapNotNull { it.bpm }
                    val averageBpm = if (bpmValues.isNotEmpty()) bpmValues.average() else null
                    val all = db.trackDao().all()
                    val scored = all.filter { it.id !in crateTrackIds }.map { track ->
                        val genreScore = if (track.genre?.lowercase() in genres) 0 else 50
                        val bpmScore = if (averageBpm != null && track.bpm != null) {
                            abs(track.bpm - averageBpm)
                        } else {
                            60.0
                        }
                        track to (genreScore + bpmScore)
                    }
                    recommendedTracks = scored.sortedBy { it.second }.map { it.first }.take(12)
                }

                fun filteredTracks(): List<Track> {
                    val q = searchQuery.trim().lowercase()
                    val genre = genreFilter
                    val artist = artistFilter
                    val bpmRange = bpmRange
                    return crateTracks.filter { track ->
                        val matchesQuery = q.isEmpty() || track.title.lowercase().contains(q) || track.artist.lowercase().contains(q)
                        val matchesGenre = genre.isNullOrEmpty() || track.genre == genre
                        val matchesArtist = artist.isNullOrEmpty() || track.artist == artist
                        val matchesBpm = when {
                            bpmRange == null -> true
                            track.bpm == null -> false
                            else -> track.bpm!!.toFloat() in bpmRange
                        }
                        matchesQuery && matchesGenre && matchesArtist && matchesBpm
                    }
                }

                fun clearFilters() {
                    searchQuery = ""
                    genreFilter = null
                    artistFilter = null
                    bpmRange = bpmBounds
                }

                fun importCsv(crateId: Long, uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
                    loading = true
                    val count = importRepo.importCsvToCrate(uri, crateId)
                    loading = false
                    refreshCratesOnly()
                    onDone(count)
                }

                fun exportCrateToCsv(crateId: Long, uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
                    loading = true
                    val count = importRepo.exportCrateToCsv(uri, crateId)
                    loading = false
                    onDone(count)
                }

                fun exportCrateToJson(crateId: Long, uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
                    loading = true
                    val count = importRepo.exportCrateToJson(uri, crateId)
                    loading = false
                    onDone(count)
                }

                fun createSetlist(name: String, tracks: List<Track>, onDone: () -> Unit) = viewModelScope.launch {
                    if (name.isBlank() || tracks.isEmpty()) return@launch
                    loading = true
                    val setlistId = db.setlistDao().insert(Setlist(name = name))
                    db.setlistDao().clearItems(setlistId)
                    tracks.forEachIndexed { index, track ->
                        db.setlistDao().upsertItem(
                            SetlistItem(
                                setlistId = setlistId,
                                position = index,
                                trackId = track.id
                            )
                        )
                    }
                    setlists = db.setlistDao().allWithCounts()
                    loading = false
                    setlistDetails = setlistDetails - setlistId
                    onDone()
                }

                fun loadSetlistTracks(setlistId: Long) = viewModelScope.launch {
                    if (setlistDetails.containsKey(setlistId)) return@launch
                    val tracks = db.setlistDao().tracksForSetlist(setlistId)
                    setlistDetails = setlistDetails + (setlistId to tracks)
                    refreshTrackTags(tracks.mapNotNull { it.track?.id })
                }

                fun addTrackToCrate(track: Track, crate: Crate, onDone: () -> Unit) = viewModelScope.launch {
                    db.crateDao().addTrack(com.david.dcc.data.model.CrateTrack(crate.id, track.id))
                    selectCrate(crate)
                    refreshCratesOnly()
                    onDone()
                }

                suspend fun assignExistingTag(trackId: Long, tag: Tag): TagOperationResult {
                    if (trackTags[trackId].orEmpty().any { it.id == tag.id }) {
                        return TagOperationResult.AlreadyAssigned(tag)
                    }
                    withContext(Dispatchers.IO) {
                        tagDao.insertTrackTag(TrackTag(trackId = trackId, tagId = tag.id))
                    }
                    refreshTrackTags(listOf(trackId))
                    return TagOperationResult.Assigned(tag, created = false)
                }

                suspend fun createAndAssignTag(trackId: Long, name: String): TagOperationResult {
                    val label = name.trim()
                    if (label.isEmpty()) return TagOperationResult.InvalidInput

                    var created = false
                    val tag = withContext(Dispatchers.IO) {
                        val existing = tagDao.findByName(label)
                        if (existing != null) {
                            existing
                        } else {
                            val newId = tagDao.insert(Tag(name = label))
                            if (newId == -1L) {
                                tagDao.findByName(label)
                            } else {
                                created = true
                                Tag(id = newId, name = label)
                            }
                        }
                    } ?: return TagOperationResult.InvalidInput

                    if (trackTags[trackId].orEmpty().any { it.id == tag.id }) {
                        if (created) {
                            refreshAllTags()
                        }
                        return TagOperationResult.AlreadyAssigned(tag)
                    }

                    withContext(Dispatchers.IO) {
                        tagDao.insertTrackTag(TrackTag(trackId = trackId, tagId = tag.id))
                    }
                    refreshAllTags()
                    refreshTrackTags(listOf(trackId))
                    return TagOperationResult.Assigned(tag, created = created)
                }

                suspend fun removeTagFromTrack(trackId: Long, tag: Tag): Boolean {
                    if (trackTags[trackId].orEmpty().none { it.id == tag.id }) return false
                    withContext(Dispatchers.IO) {
                        tagDao.deleteTrackTag(trackId, tag.id)
                    }
                    refreshTrackTags(listOf(trackId))
                    return true
                }

                private suspend fun refreshAllTags() {
                    val tags = withContext(Dispatchers.IO) { tagDao.all() }
                    allTags = tags
                }

                private suspend fun refreshTrackTags(trackIds: List<Long>) {
                    if (trackIds.isEmpty()) return
                    val assignments = withContext(Dispatchers.IO) { tagDao.tagsForTrackIds(trackIds) }
                    val updated = trackTags.toMutableMap().apply {
                        trackIds.forEach { remove(it) }
                    }
                    assignments.groupBy { it.trackId }.forEach { (trackId, tags) ->
                        updated[trackId] = tags.map { it.tag }.sortedBy { it.name.lowercase() }
                    }
                    trackTags = updated
                }
            }

            @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
            @Composable
            fun HomeScreen(
                snackbarHostState: SnackbarHostState,
                launchCsvPicker: () -> Unit,
                registerOnCsvPicked: ((Uri) -> Unit) -> Unit,
                launchCsvExporter: (String) -> Unit,
            registerOnCsvExported: ((Uri) -> Unit) -> Unit,
            launchJsonExporter: (String) -> Unit,
            registerOnJsonExported: ((Uri) -> Unit) -> Unit,
            vm: HomeVm = viewModel(),
            ) {

            val snackbarScope = rememberCoroutineScope()
            val snackbar by rememberUpdatedState(snackbarHostState)
            val crateSummaries by remember { derivedStateOf { vm.crateSummaries } }
            val selectedCrate by remember { derivedStateOf { vm.selectedCrate } }
            val filteredTracks by remember { derivedStateOf { vm.filteredTracks() } }
            val recommendedTracks by remember { derivedStateOf { vm.recommendedTracks } }
            val setlists by remember { derivedStateOf { vm.setlists } }
            val loading by remember { derivedStateOf { vm.loading } }
            val trackTags by remember { derivedStateOf { vm.trackTags } }
            val allTags by remember { derivedStateOf { vm.allTags } }

            var newCrateName by remember { mutableStateOf("") }
            var colorFilter by remember { mutableStateOf<CrateColorCategory?>(null) }
            var setlistName by remember { mutableStateOf("Set Ibiza 2025") }
            var expandedSetlistId by remember { mutableStateOf<Long?>(null) }
            var selectedTrackIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
            var tagEditorTarget by remember { mutableStateOf<Track?>(null) }

            LaunchedEffect(registerOnCsvPicked) {
                registerOnCsvPicked { uri ->
                    val crate = selectedCrate
                    if (crate == null) {
                            snackbarScope.launch {
                                snackbar.showSnackbar("Selecciona un crate antes de importar")
                            }
                            return@registerOnCsvPicked
                        }

                                vm.importCsv(crate.id, uri) { count ->
                                    snackbarScope.launch {
                                        val message = if (count == 0) {
                                            "No se importaron pistas en \"${crate.name}\""
                                        } else {
                                            "Se importaron $count pistas en \"${crate.name}\""
                                        }
                                        snackbar.showSnackbar(message)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(selectedCrate?.id) {
                            selectedTrackIds = emptySet()
                            selectedCrate?.let {
                                setlistName = "Set ${it.name}"
                            }
                            tagEditorTarget = null
                        }

                        Column(Modifier.padding(16.dp)) {
                            Text("Crates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newCrateName,
                                        onValueChange = { newCrateName = it },
                                        label = { Text("Nombre del crate") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        if (newCrateName.isNotBlank()) {
                                            vm.createCrate(newCrateName) {
                                                newCrateName = ""
                                            }
                                        }
                                    }) {
                                        Text("Crear")
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                val crateColorCategories = remember(crateSummaries) {
                                    crateSummaries.map { colorCategoryFor(it.crate) }.distinctBy { it.name }
                                }
                                if (crateColorCategories.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            FilterChip(
                                                selected = colorFilter == null,
                                                onClick = { colorFilter = null },
                                                label = { Text("Todos") }
                                            )
                                        }
                                        items(crateColorCategories) { category ->
                                            FilterChip(
                                                selected = colorFilter == category,
                                                onClick = { colorFilter = category },
                                                label = { Text(category.name) },
                                                leadingIcon = {
                                                    Box(
                                                        Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(category.color)
                                                    )
                                                }
                                            )
                                        }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    items(crateSummaries.filter { colorFilter?.matches(it.crate) ?: true }) { summary ->
                                                        val crate = summary.crate
                                                        val isSelectedCrate = selectedCrate?.id == crate.id
                                                        val cardModifier = Modifier
                                                            .width(240.dp)
                                                            .heightIn(min = 160.dp)
                                                            .then(
                                                                if (isSelectedCrate) {
                                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                        ElevatedCard(
                                                            modifier = cardModifier,
                                                            onClick = { vm.selectCrate(crate) },
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Column(
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .padding(16.dp),
                                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(colorForCrate(crate)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(crate.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                                }
                                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                    Text(
                                                                        crate.name,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text("${summary.trackCount} pistas", style = MaterialTheme.typography.bodySmall)
                                                                }
                                                                TextButton(onClick = {
                                                                    vm.selectCrate(crate)
                                                                    launchCsvPicker()
                                                            }) {
                                                            Icon(Icons.Filled.Add, contentDescription = null)
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Importar CSV")
                                                        }
                                                        }
                                                    }
                                                }
                                            }

                                                    Spacer(Modifier.height(24.dp))

                                                    selectedCrate?.let { crate ->
                                                Text(
                                                    "${crate.name} · Gestión avanzada",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(Modifier.height(8.dp))

                                                Column(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                                        .padding(16.dp)
                                                ) {
                                                    Text("Búsqueda y filtros", style = MaterialTheme.typography.titleMedium)
                                                    Spacer(Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = vm.searchQuery,
                                                        onValueChange = { vm.searchQuery = it },
                                                        label = { Text("Busca por título o artista") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                                                    )
                                                    Spacer(Modifier.height(12.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                                        FilterDropdown(value = vm.genreFilter, options = vm.availableGenres, label = "Género") { vm.genreFilter = it }
                                                        FilterDropdown(value = vm.artistFilter, options = vm.availableArtists, label = "Artista") { vm.artistFilter = it }
                                                    }
                                                    vm.bpmBounds?.let { bounds ->
                                                        Spacer(Modifier.height(12.dp))
                                                        Text(
                                                            "Filtra por BPM (${bounds.start.toInt()} - ${bounds.endInclusive.toInt()})",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        RangeSlider(
                                                            value = vm.bpmRange ?: bounds,
                                                            onValueChange = { vm.bpmRange = it },
                                                            valueRange = bounds
                                                        )
                                                    }
                                                    Spacer(Modifier.height(12.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AssistChip(onClick = { vm.clearFilters() }, label = { Text("Limpiar filtros") })
                                                        AssistChip(onClick = { launchCsvPicker() }, label = { Text("Importar desde CSV/Drive") })
                                                    }
                                                }

                                                Spacer(Modifier.height(16.dp))

                                                Text("Interfaz tipo crate", style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(8.dp))

                                                LazyVerticalGrid(
                                                    columns = GridCells.Adaptive(180.dp),
                                                    modifier = Modifier.heightIn(max = 420.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(filteredTracks, key = { it.id }) { track ->
                                                        val isSelected = track.id in selectedTrackIds
                                                        val tagsForTrack = trackTags[track.id].orEmpty()
                                                        TrackCard(
                                                            track = track,
                                                            tags = tagsForTrack,
                                                            selected = isSelected,
                                                            onSelectedChange = { checked ->
                                                                selectedTrackIds = if (checked) {
                                                                    selectedTrackIds + track.id
                                                                } else {
                                                                    selectedTrackIds - track.id
                                                                }
                                                            },
                                                            onRemoveTag = { tag ->
                                                                snackbarScope.launch {
                                                                    if (vm.removeTagFromTrack(track.id, tag)) {
                                                                        snackbar.showSnackbar("Etiqueta \"${tag.name}\" eliminada de ${track.title}")
                                                                    }
                                                                }
                                                            },
                                                            onEditTags = { tagEditorTarget = track }
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(16.dp))

                                                Text("Modo setlist", style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = setlistName,
                                                    onValueChange = { setlistName = it },
                                                    label = { Text("Nombre del setlist") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Button(onClick = {
                                                    val tracks = filteredTracks.filter { it.id in selectedTrackIds }
                                                    if (tracks.isEmpty()) {
                                                        snackbarScope.launch { snackbar.showSnackbar("Selecciona pistas para el setlist") }
                                                    } else {
                                                        vm.createSetlist(setlistName.trim(), tracks) {
                                                            snackbarScope.launch {
                                                                snackbar.showSnackbar("Setlist \"$setlistName\" guardado (${tracks.size} pistas)")
                                                                selectedTrackIds = emptySet()
                                                            }
                                                        }
                                                    }
                                                }) {
                                                    Text("Guardar setlist personalizado")
                                                }

                                                Spacer(Modifier.height(16.dp))

                                                Text("Recomendaciones inteligentes", style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(8.dp))
                                                if (recommendedTracks.isEmpty()) {
                                                    Text("Añade más pistas para recibir recomendaciones basadas en BPM y género.")
                                                } else {
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        items(recommendedTracks) { track ->
                                                            RecommendationCard(track = track, onAdd = {
                                                                vm.addTrackToCrate(track, crate) {
                                                                    snackbarScope.launch { snackbar.showSnackbar("${track.title} añadido a ${crate.name}") }
                                                                }
                                                            })
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.height(16.dp))

                                                Text("Sincronización con Drive", style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Button(onClick = {
                                                        registerOnCsvExported { uri ->
                                                            vm.exportCrateToCsv(crate.id, uri) { count ->
                                                                snackbarScope.launch { snackbar.showSnackbar("Exportadas $count pistas a CSV") }
                                                            }
                                                        }
                                                        launchCsvExporter("${crate.name}.csv")
                                                    }) { Text("Subir CSV a Drive") }
                                                    Button(onClick = { launchCsvPicker() }) { Text("Descargar CSV desde Drive") }
                                                }

                                                Spacer(Modifier.height(12.dp))

                                                Text("Integración Rekordbox / Serato / Traktor", style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(8.dp))
                                                Button(onClick = {
                                                    registerOnJsonExported { uri ->
                                                        vm.exportCrateToJson(crate.id, uri) { count ->
                                                            snackbarScope.launch { snackbar.showSnackbar("Exportadas $count pistas en JSON multi-formato") }
                                                        }
                                                    }
                                                    launchJsonExporter("${crate.name}_pro.json")
                                                }) {
                                                    Text("Exportar JSON compatible")
                                                }
                                            }

                                                    Spacer(Modifier.height(24.dp))

                                                    Text("Setlists guardados", style = MaterialTheme.typography.headlineSmall)
                                            Spacer(Modifier.height(8.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(setlists) { summary ->
                                                    ElevatedCard(onClick = {
                                                        expandedSetlistId = if (expandedSetlistId == summary.setlist.id) null else summary.setlist.id
                                                        vm.loadSetlistTracks(summary.setlist.id)
                                                    }) {
                                                        Column(Modifier.padding(16.dp)) {
                                                            Text(summary.setlist.name, style = MaterialTheme.typography.titleMedium)
                                                            Text("${summary.trackCount} pistas", style = MaterialTheme.typography.bodySmall)
                                                            if (expandedSetlistId == summary.setlist.id) {
                                                                val tracks = vm.setlistDetails[summary.setlist.id].orEmpty()
                                                                Spacer(Modifier.height(8.dp))
                                                                tracks.forEach { trackView ->
                                                                    trackView.track?.let { track ->
                                                                        Column(
                                                                            Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(vertical = 6.dp)
                                                                        ) {
                                                                            Text(
                                                                                "${trackView.position + 1}. ${track.artist} - ${track.title}",
                                                                                style = MaterialTheme.typography.bodySmall,
                                                                                maxLines = 2,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                            val tagsForTrack = trackTags[track.id].orEmpty()
                                                                            TagPillRow(
                                                                                tags = tagsForTrack,
                                                                                onRemoveTag = { tag ->
                                                                                    snackbarScope.launch {
                                                                                        if (vm.removeTagFromTrack(track.id, tag)) {
                                                                                            snackbar.showSnackbar("Etiqueta \"${tag.name}\" eliminada de ${track.title}")
                                                                                        }
                                                                                    }
                                                                                },
                                                                                onEditTags = { tagEditorTarget = track }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            tagEditorTarget?.let { track ->
                                                val assignedTags = trackTags[track.id].orEmpty()
                                                val availableTags = allTags.filter { tag -> assignedTags.none { it.id == tag.id } }
                                                TagEditorDialog(
                                                    track = track,
                                                    assignedTags = assignedTags,
                                                    availableTags = availableTags,
                                                    snackbarHostState = snackbarHostState,
                                                    onAddExistingTag = { tag -> vm.assignExistingTag(track.id, tag) },
                                                    onCreateTag = { name -> vm.createAndAssignTag(track.id, name) },
                                                    onDismiss = { tagEditorTarget = null }
                                                )
                                            }

                                            if (loading) {
                                                Spacer(Modifier.height(16.dp))
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            }
                                        }
                                    }

                                    @OptIn(ExperimentalMaterial3Api::class)
                                    @Composable
                                    private fun FilterDropdown(
                                        value: String?,
                                        options: List<String>,
                                        label: String,
                                        onSelected: (String?) -> Unit
                                    ) {
                                        var expanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                            OutlinedTextField(
                                                value = value.orEmpty(),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(label) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.menuAnchor()
                                            )
                                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                DropdownMenuItem(text = { Text("Sin filtro") }, onClick = {
                                                    onSelected(null)
                                                    expanded = false
                                                })
                                                options.forEach { option ->
                                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                                        onSelected(option)
                                                        expanded = false
                                                    })
                                                }
                                            }
                                        }
                                    }

                                    @Composable
                                    private fun TrackCard(
                                        track: Track,
                                        tags: List<Tag>,
                                        selected: Boolean,
                                        onSelectedChange: (Boolean) -> Unit,
                                        onRemoveTag: (Tag) -> Unit,
                                        onEditTags: () -> Unit
                                    ) {
                                        ElevatedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 200.dp),
                                            onClick = { onSelectedChange(!selected) }
                                        ) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .height(72.dp)
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(colorForTrack(track)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = track.title.take(1).uppercase(),
                                                        style = MaterialTheme.typography.headlineMedium,
                                                        color = Color.White
                                                    )
                                                }
                                                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    track.genre?.takeIf { it.isNotBlank() }?.let {
                                                        AssistChip(onClick = {}, label = { Text(it) }, enabled = false)
                                                    }
                                                    track.bpm?.let {
                                                        AssistChip(onClick = {}, label = { Text("${it.toInt()} BPM") }, enabled = false)
                                                    }
                                                }
                                                TagPillRow(tags = tags, onRemoveTag = onRemoveTag, onEditTags = onEditTags)
                                                Checkbox(checked = selected, onCheckedChange = onSelectedChange)
                                            }
                                        }
                                    }

                                    @OptIn(ExperimentalLayoutApi::class)
                                    @Composable
                                    private fun TagPillRow(
                                        tags: List<Tag>,
                                        onRemoveTag: (Tag) -> Unit,
                                        onEditTags: () -> Unit
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            tags.forEach { tag ->
                                                InputChip(
                                                    selected = true,
                                                    onClick = {},
                                                    label = { Text(tag.name) },
                                                    trailingIcon = {
                                                        IconButton(onClick = { onRemoveTag(tag) }) {
                                                            Icon(
                                                                Icons.Filled.Close,
                                                                contentDescription = "Eliminar etiqueta ${tag.name}"
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            AssistChip(
                                                onClick = onEditTags,
                                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                                label = {
                                                    Text(if (tags.isEmpty()) "Añadir etiquetas" else "Editar etiquetas")
                                                }
                                            )
                                        }
                                    }

                                    @OptIn(ExperimentalLayoutApi::class)
                                    @Composable
                                    private fun TagEditorDialog(
                                        track: Track,
                                        assignedTags: List<Tag>,
                                        availableTags: List<Tag>,
                                        snackbarHostState: SnackbarHostState,
                                        onAddExistingTag: suspend (Tag) -> TagOperationResult,
                                        onCreateTag: suspend (String) -> TagOperationResult,
                                        onDismiss: () -> Unit
                                    ) {
                                        val tagEditorScope = rememberCoroutineScope()
                                        var newTagName by remember(track.id) { mutableStateOf("") }
                                        var busy by remember(track.id) { mutableStateOf(false) }

                                        AlertDialog(
                                            onDismissRequest = { if (!busy) onDismiss() },
                                            title = { Text("Etiquetas para ${track.title}") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    if (assignedTags.isEmpty()) {
                                                        Text("Este track aún no tiene etiquetas asignadas.")
                                                    } else {
                                                        Text("Etiquetas asignadas")
                                                        FlowRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            assignedTags.forEach { tag ->
                                                                AssistChip(onClick = {}, label = { Text(tag.name) }, enabled = false)
                                                            }
                                                        }
                                                    }

                                                    Divider()
                                                    Text("Añadir etiquetas existentes")
                                                    if (availableTags.isEmpty()) {
                                                        Text("No quedan etiquetas disponibles. Crea una nueva.")
                                                    } else {
                                                        FlowRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            availableTags.forEach { tag ->
                                                                FilterChip(
                                                                    selected = false,
                                                                    enabled = !busy,
                                                                    onClick = {
                                                                        if (busy) return@FilterChip
                                                                        tagEditorScope.launch {
                                                                            busy = true
                                                                            try {
                                                                                val result = onAddExistingTag(tag)
                                                                                snackbarHostState.showSnackbar(result.message(track))
                                                                            } finally {
                                                                                busy = false
                                                                            }
                                                                        }
                                                                    },
                                                                    label = { Text(tag.name) }
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Divider()
                                                    OutlinedTextField(
                                                        value = newTagName,
                                                        onValueChange = { newTagName = it },
                                                        label = { Text("Nueva etiqueta") },
                                                        singleLine = true,
                                                        enabled = !busy
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        if (busy || newTagName.isBlank()) return@TextButton
                                                        tagEditorScope.launch {
                                                            busy = true
                                                            try {
                                                                val result = onCreateTag(newTagName)
                                                                snackbarHostState.showSnackbar(result.message(track))
                                                                if (result is TagOperationResult.Assigned) {
                                                                    newTagName = ""
                                                                }
                                                            } finally {
                                                                busy = false
                                                            }
                                                        }
                                                    },
                                                    enabled = !busy && newTagName.isNotBlank()
                                                ) {
                                                    Text("Crear y asignar")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { if (!busy) onDismiss() }) { Text("Cerrar") }
                                            }
                                        )
                                    }

                                    @Composable
                                    private fun RecommendationCard(track: Track, onAdd: () -> Unit) {
                                        ElevatedCard(modifier = Modifier.width(220.dp)) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(track.artist, style = MaterialTheme.typography.bodySmall)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    track.genre?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                                    track.bpm?.let { Text("${it.toInt()} BPM", style = MaterialTheme.typography.bodySmall) }
                                                }
                                                Button(onClick = onAdd) { Text("Añadir al crate") }
                                            }
                                        }
                                    }

                                    data class CrateColorCategory(val name: String, val color: Color) {
                                        fun matches(crate: Crate): Boolean = colorCategoryFor(crate).name == name
                                    }

                                    private fun colorForCrate(crate: Crate): Color {
                                        val hue = (crate.name.hashCode().absoluteValue % 360).toFloat()
                                        return Color.hsl(hue, 0.5f, 0.45f)
                                    }

                                    private fun colorCategoryFor(crate: Crate): CrateColorCategory {
                                        val hue = (crate.name.hashCode().absoluteValue % 360)
                                        val category = when (hue) {
                                            in 0..45, in 315..360 -> "Rojo"
                                            in 46..90 -> "Naranja"
                                            in 91..150 -> "Amarillo"
                                            in 151..210 -> "Verde"
                                            in 211..270 -> "Azul"
                                            else -> "Morado"
                                        }
                                        return CrateColorCategory(category, colorForCrate(crate))
                                    }

                                    private fun colorForTrack(track: Track): Color {
                                        val base = track.genre?.hashCode() ?: track.artist.hashCode()
                                        val hue = (base.absoluteValue % 360).toFloat()
                                        return Color.hsl(hue, 0.45f, 0.5f)
                                    }

                                    sealed interface TagOperationResult {
                                        data class Assigned(val tag: Tag, val created: Boolean) : TagOperationResult
                                        data class AlreadyAssigned(val tag: Tag) : TagOperationResult
                                        object InvalidInput : TagOperationResult
                                    }

                                    private fun TagOperationResult.message(track: Track): String = when (this) {
                                        is TagOperationResult.Assigned -> {
                                            val base = "Etiqueta \"${tag.name}\""
                                            if (created) "$base creada y añadida a ${track.title}" else "$base añadida a ${track.title}"
                                        }
                                        is TagOperationResult.AlreadyAssigned -> "La etiqueta \"${tag.name}\" ya estaba asignada a ${track.title}"
                                        TagOperationResult.InvalidInput -> "Escribe un nombre de etiqueta válido"
                                    }