package com.david.dcc.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.david.dcc.data.db.AppDb
import com.david.dcc.data.model.Crate
import com.david.dcc.data.model.CrateTrack
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


class HomeVm(application: Application) : ViewModel() {
    private val db = AppDb.get(application)
    private val importRepo = ImportRepository(application, db)
    private val tagDao = db.tagDao()

    var crateSummaries by mutableStateOf<List<CrateWithTrackCount>>(emptyList())
        private set
    var selectedCrate by mutableStateOf<Crate?>(null)
        private set
    var crateTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var recommendedTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var setlists by mutableStateOf<List<SetlistWithCount>>(emptyList())
        private set
    var setlistDetails by mutableStateOf<Map<Long, List<SetlistTrackView>>>(emptyMap())
        private set
    var loading by mutableStateOf(false)
        private set

    var allTags by mutableStateOf<List<Tag>>(emptyList())
        private set
    var trackTags by mutableStateOf<Map<Long, List<Tag>>>(emptyMap())
        private set

    var searchQuery by mutableStateOf("")
    var genreFilter by mutableStateOf<String?>(null)
    var artistFilter by mutableStateOf<String?>(null)
    var keyFilter by mutableStateOf<String?>(null)
    var bpmBounds by mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
        private set
    var bpmRange by mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
    var availableGenres by mutableStateOf<List<String>>(emptyList())
        private set
    var availableArtists by mutableStateOf<List<String>>(emptyList())
        private set
    var availableKeys by mutableStateOf<List<String>>(emptyList())
        private set

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
        selectedCrate?.let { current ->
            crateSummaries.firstOrNull { it.crate.id == current.id }?.crate?.let { selectCrate(it) }
        }
    }

    fun refreshCratesOnly() = viewModelScope.launch {
        crateSummaries = db.crateDao().allWithCounts()
        selectedCrate?.let { current ->
            crateSummaries.firstOrNull { it.crate.id == current.id }?.crate?.let { selectCrate(it) }
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
        keyFilter = null
        bpmBounds = null
        bpmRange = null

        loading = true
        val tracks = db.crateDao().tracksInCrate(crate.id)
        crateTracks = tracks
        refreshTrackTags(tracks.map { it.id })
        loading = false

        updateFilterSources(tracks)
        loadRecommendations(tracks)
    }

    private fun updateFilterSources(tracks: List<Track>) {
        availableGenres = tracks.mapNotNull { it.genre?.takeIf(String::isNotBlank)?.trim() }.distinct().sorted()
        availableArtists = tracks.mapNotNull { it.artist.takeIf(String::isNotBlank) }.distinct().sorted()
        availableKeys = tracks.mapNotNull { it.musicalKey?.takeIf(String::isNotBlank)?.trim() }
            .distinct()
            .sorted()
        val bpms = tracks.mapNotNull { it.bpm?.toFloat() }
        if (bpms.isNotEmpty()) {
            val min = bpms.minOrNull() ?: 0f
            val max = bpms.maxOrNull() ?: min
            val bounds = min..max
            bpmBounds = bounds
            bpmRange = bounds
        } else {
            bpmBounds = null
            bpmRange = null
        }
    }

    private fun loadRecommendations(tracks: List<Track>) = viewModelScope.launch {
        if (tracks.isEmpty()) {
            recommendedTracks = emptyList()
            return@launch
        }

        val crateTrackIds = tracks.map { it.id }.toSet()
        val genres = tracks.mapNotNull { it.genre?.lowercase() }.toSet()
        val bpms = tracks.mapNotNull { it.bpm }
        val keys = tracks.mapNotNull { it.musicalKey?.let(::normalizeKey) }.toSet()
        val averageBpm = bpms.takeIf { it.isNotEmpty() }?.average()

        val candidates = db.trackDao().all()
            .filter { it.id !in crateTrackIds }
            .map { track ->
                val genreScore = if (track.genre?.lowercase() in genres) 0.0 else 40.0
                val bpmScore = when {
                    averageBpm == null || track.bpm == null -> 30.0
                    else -> abs(track.bpm - averageBpm)
                }
                val keyScore = keyCompatibilityScore(keys, track.musicalKey)
                track to (genreScore + bpmScore + keyScore)
            }

            .sortedBy { it.second }
            .map { it.first }
            .take(12)

        recommendedTracks = candidates
    }

    fun filteredTracks(): List<Track> {
        val q = searchQuery.trim().lowercase()
        val genre = genreFilter
        val artist = artistFilter
        val key = keyFilter?.let(::normalizeKey)
        val bpmRangeSnapshot = bpmRange

        return crateTracks.filter { track ->
            val matchesQuery = q.isEmpty() ||
                    track.title.lowercase().contains(q) ||
                    track.artist.lowercase().contains(q)
            val matchesGenre = genre.isNullOrEmpty() || track.genre == genre
            val matchesArtist = artist.isNullOrEmpty() || track.artist == artist
            val matchesKey = key == null || normalizeKey(track.musicalKey) == key
            val matchesBpm = when {
                bpmRangeSnapshot == null -> true
                track.bpm == null -> false
                else -> track.bpm!!.toFloat() in bpmRangeSnapshot
            }
            matchesQuery && matchesGenre && matchesArtist && matchesKey && matchesBpm
        }
    }

    fun clearFilters() {
        searchQuery = ""
        genreFilter = null
        artistFilter = null
        keyFilter = null
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

    fun exportSetlistToRekordbox(setlistId: Long, uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
        loading = true
        val count = importRepo.exportSetlistToRekordboxCsv(setlistId, uri)
        loading = false
        onDone(count)
    }

    fun createSetlist(name: String, tracks: List<Track>, onDone: () -> Unit) = viewModelScope.launch {
        if (name.isBlank() || tracks.isEmpty()) return@launch
        loading = true
        val setlistId = db.setlistDao().insert(Setlist(name = name.trim()))
        db.setlistDao().clearItems(setlistId)
        tracks.forEachIndexed { index, track ->
            db.setlistDao().upsertItem(
                SetlistItem(
                    setlistId = setlistId,
                    position = index,
                    trackId = track.id,
                ),
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
        db.crateDao().addTrack(CrateTrack(crate.id, track.id))
        selectCrate(crate)
        refreshCratesOnly()
        onDone()
    }

    internal suspend fun assignExistingTag(trackId: Long, tag: Tag): TagOperationResult {
        if (trackTags[trackId].orEmpty().any { it.id == tag.id }) {
            return TagOperationResult.AlreadyAssigned(tag)
        }
        withContext(Dispatchers.IO) {
            tagDao.insertTrackTag(TrackTag(trackId = trackId, tagId = tag.id))
        }
        refreshTrackTags(listOf(trackId))
        return TagOperationResult.Assigned(tag, created = false)
    }

    internal suspend fun createAndAssignTag(trackId: Long, name: String): TagOperationResult {
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

internal suspend fun removeTagFromTrack(trackId: Long, tag: Tag): Boolean {
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

private fun keyCompatibilityScore(preferredKeys: Set<String>, candidateKey: String?): Double {
    if (preferredKeys.isEmpty()) return 20.0
    val candidate = normalizeKey(candidateKey) ?: return 20.0
    if (candidate in preferredKeys) return 0.0
    val candidateNumber = candidate.takeWhile { it.isDigit() }
    val sameNumber = preferredKeys.any { it.startsWith(candidateNumber) }
    return if (sameNumber) 8.0 else 16.0
}
    companion object {
        fun factory(application: Application) = viewModelFactory {
            initializer { HomeVm(application) }
        }
    }
}

private fun normalizeKey(value: String?): String? = value?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

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
    launchSetlistExporter: (String) -> Unit,
    registerOnSetlistExported: ((Uri) -> Unit) -> Unit,
    vm: HomeVm,
) {
    val snackbarScope = rememberCoroutineScope()
    val snackbarHost by rememberUpdatedState(snackbarHostState)

    val crateSummaries = vm.crateSummaries
    val selectedCrate = vm.selectedCrate
    val loading = vm.loading
    val trackTags = vm.trackTags
    val allTags = vm.allTags
    val setlists = vm.setlists
    val setlistDetails = vm.setlistDetails

    val filteredTracks = vm.filteredTracks()

    var newCrateName by remember { mutableStateOf("") }
    var colorFilter by remember { mutableStateOf<CrateColorCategory?>(null) }
    var setlistName by remember { mutableStateOf("Set Ibiza 2025") }
    var expandedSetlistId by remember { mutableStateOf<Long?>(null) }
    var selectedTrackIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var tagEditorTarget by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(registerOnCsvPicked) {
        registerOnCsvPicked { uri ->
            val crate = vm.selectedCrate
            if (crate == null) {
                snackbarScope.launch {
                    snackbarHost.showSnackbar("Selecciona un crate antes de importar")
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
                    snackbarHost.showSnackbar(message)

                }

            }
        }
    }

    LaunchedEffect(selectedCrate?.id) {
        selectedTrackIds = emptySet()
        selectedCrate?.let { setlistName = "Set ${it.name}" }
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
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newCrateName.isNotBlank()) {
                    vm.createCrate(newCrateName) { newCrateName = "" }
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
                        label = { Text("Todos") },
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
                                    .background(category.color),
                            )
                        },
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
                        },
                    )
                ElevatedCard(
                    modifier = cardModifier,
                    onClick = { vm.selectCrate(crate) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(60.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorForCrate(crate)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(crate.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                        Text(crate.name, style = MaterialTheme.typography.titleMedium)
                        Text("${summary.trackCount} pistas", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        selectedCrate?.let { crate ->
            Text("Tracks en ${crate.name}", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = vm.searchQuery,
                    onValueChange = { vm.searchQuery = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { vm.clearFilters() }) { Text("Limpiar filtros") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterDropdown(
                    value = vm.genreFilter,
                    options = vm.availableGenres,
                    label = "Género",
                    onSelected = { vm.genreFilter = it },
                )
                FilterDropdown(
                    value = vm.artistFilter,
                    options = vm.availableArtists,
                    label = "Artista",
                    onSelected = { vm.artistFilter = it },
                )
                FilterDropdown(
                    value = vm.keyFilter,
                    options = vm.availableKeys,
                    label = "Tonalidad",
                    onSelected = { vm.keyFilter = it },
                )
            }

            vm.bpmBounds?.let { bounds ->
                Spacer(Modifier.height(8.dp))
                Text("Rango BPM", style = MaterialTheme.typography.labelMedium)
                RangeSlider(
                    value = vm.bpmRange ?: bounds,
                    onValueChange = { vm.bpmRange = it },
                    valueRange = bounds,
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = Modifier.heightIn(min = 120.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    val tags = trackTags[track.id].orEmpty()

                    TrackCard(
                        track = track,
                        tags = tags,
                        selected = track.id in selectedTrackIds,
                        onSelectedChange = { isSelected ->
                            selectedTrackIds = if (isSelected) {
                                selectedTrackIds + track.id
                            } else {
                                selectedTrackIds - track.id

                            }
                        },
                        onRemoveTag = { tag ->
                            snackbarScope.launch {
                                if (vm.removeTagFromTrack(track.id, tag)) {
                                    snackbarHost.showSnackbar("Etiqueta \"${tag.name}\" eliminada de ${track.title}")
                                }
                            }
                        },
                        onEditTags = { tagEditorTarget = track },
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val tracks = filteredTracks.filter { it.id in selectedTrackIds }
                if (tracks.isEmpty()) {
                    snackbarScope.launch { snackbarHost.showSnackbar("Selecciona pistas para el setlist") }
                } else {
                    vm.createSetlist(setlistName.trim(), tracks) {
                        snackbarScope.launch {
                            snackbarHost.showSnackbar("Setlist \"$setlistName\" guardado (${tracks.size} pistas)")
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
            if (vm.recommendedTracks.isEmpty()) {
                Text("Añade más pistas para recibir recomendaciones basadas en BPM, género y tonalidad.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.recommendedTracks) { track ->
                        RecommendationCard(track = track) {
                            vm.addTrackToCrate(track, crate) {
                                snackbarScope.launch {
                                    snackbarHost.showSnackbar("${track.title} añadido a ${crate.name}")
                                }
                            }
                        }
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
                            snackbarScope.launch { snackbarHost.showSnackbar("Exportadas $count pistas a CSV") }
                        }
                    }
                    launchCsvExporter("${crate.name}.csv")
                }) {
                    Text("Subir CSV a Drive")
                }
                Button(onClick = { launchCsvPicker() }) {
                    Text("Descargar CSV desde Drive")
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Integración Rekordbox / Serato / Traktor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                registerOnJsonExported { uri ->
                    vm.exportCrateToJson(crate.id, uri) { count ->
                        snackbarScope.launch {
                            snackbarHost.showSnackbar("Exportadas $count pistas en JSON multi-formato")
                        }
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
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {
                                registerOnSetlistExported { uri ->
                                    vm.exportSetlistToRekordbox(summary.setlist.id, uri) { count ->
                                        snackbarScope.launch {
                                            snackbarHost.showSnackbar("Exportadas $count pistas del setlist a CSV Rekordbox")
                                        }
                                    }
                                }
                                launchSetlistExporter("${summary.setlist.name}_rekordbox.csv")
                            }, label = { Text("Exportar Rekordbox") })
                            AssistChip(onClick = { expandedSetlistId = null }, label = { Text("Cerrar detalle") })
                        }

                        if (expandedSetlistId == summary.setlist.id) {
                            val tracks = setlistDetails[summary.setlist.id].orEmpty()
                            Spacer(Modifier.height(8.dp))
                            tracks.forEach { trackView ->
                                trackView.track?.let { track ->
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                    ) {
                                        Text(
                                            "${trackView.position + 1}. ${track.artist} - ${track.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val tagsForTrack = trackTags[track.id].orEmpty()
                                        TagPillRow(
                                            tags = tagsForTrack,
                                            onRemoveTag = { tag ->
                                                snackbarScope.launch {
                                                    if (vm.removeTagFromTrack(track.id, tag)) {
                                                        snackbarHost.showSnackbar("Etiqueta \"${tag.name}\" eliminada de ${track.title}")
                                                    }
                                                }
                                            },
                                            onEditTags = { tagEditorTarget = track },

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
                onDismiss = { tagEditorTarget = null },
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
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
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
    onEditTags: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        onClick = { onSelectedChange(!selected) },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .height(72.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorForTrack(track)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = track.title.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
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
                track.musicalKey?.takeIf { it.isNotBlank() }?.let {
                    AssistChip(onClick = {}, label = { Text(it) }, enabled = false)
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
    onEditTags: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = true,
                onClick = {},
                label = { Text(tag.name) },
                trailingIcon = {
                    IconButton(onClick = { onRemoveTag(tag) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Eliminar etiqueta ${tag.name}")
                    }
                },
            )
        }
        AssistChip(
            onClick = onEditTags,
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            label = { Text(if (tags.isEmpty()) "Añadir etiquetas" else "Editar etiquetas") },
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
    onDismiss: () -> Unit,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                    label = { Text(tag.name) },
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
                    enabled = !busy,
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
                enabled = !busy && newTagName.isNotBlank(),
            ) {
                Text("Crear y asignar")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) {
                Text("Cerrar")
            }
        },
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
                track.musicalKey?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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
    val hue = crate.name.hashCode().absoluteValue % 360
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

internal sealed interface TagOperationResult {
    data class Assigned(val tag: Tag, val created: Boolean) : TagOperationResult
    data class AlreadyAssigned(val tag: Tag) : TagOperationResult
    data object InvalidInput : TagOperationResult
}

private fun TagOperationResult.message(track: Track): String = when (this) {
    is TagOperationResult.Assigned -> {
        val base = "Etiqueta \"${tag.name}\""
        if (created) "$base creada y añadida a ${track.title}" else "$base añadida a ${track.title}"
    }
    is TagOperationResult.AlreadyAssigned -> "La etiqueta \"${tag.name}\" ya estaba asignada a ${track.title}"
    TagOperationResult.InvalidInput -> "Escribe un nombre de etiqueta válido"
}