package com.david.dcc.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.david.dcc.data.model.TechnoStyle
import java.util.Locale
import com.david.dcc.data.model.SetlistExportFormat



class HomeVm(application: Application) : AndroidViewModel(application) {
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
    var libraryTracks by mutableStateOf<List<Track>>(emptyList())
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
        refreshLibrary()
        viewModelScope.launch { refreshAllTags() }
    }

    fun refreshAll() = viewModelScope.launch {
        loading = true
        crateSummaries = db.crateDao().allWithCounts()
        setlists = db.setlistDao().allWithCounts()
        libraryTracks = db.trackDao().all()
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

    fun refreshLibrary() = viewModelScope.launch {
        libraryTracks = db.trackDao().all()
    }
    fun updateCrate(crateId: Long, name: String, style: TechnoStyle, onDone: () -> Unit = {}) = viewModelScope.launch {
        val existing = db.crateDao().byId(crateId) ?: return@launch
        val trimmedName = name.trim().takeIf { it.isNotEmpty() } ?: return@launch
        val updated = existing.copy(name = trimmedName, colorCategory = style.id)
        db.crateDao().update(updated)
        refreshCratesOnly()
        selectedCrate?.let { current ->
            if (current.id == crateId) {
                selectCrate(updated)
            }
        }
        onDone()
    }

    fun deleteCrate(crate: Crate, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.crateDao().delete(crate)
        if (selectedCrate?.id == crate.id) {
            selectedCrate = null
            crateTracks = emptyList()
            recommendedTracks = emptyList()
            clearFilters()
        }
        refreshCratesOnly()
        onDone()
    }

    fun createCrate(name: String, style: TechnoStyle, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        db.crateDao().insert(Crate(name = name.trim(), colorCategory = style.id))
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

    fun exportSetlist(
        setlistId: Long,
        format: SetlistExportFormat,
        uri: Uri,
        onDone: (Int) -> Unit,
    ) = viewModelScope.launch {
        loading = true
        val count = importRepo.exportSetlistToCsv(setlistId, uri, format)
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

    suspend fun buildCrateShareText(crateId: Long): String? = withContext(Dispatchers.IO) {
        val crate = db.crateDao().byId(crateId) ?: return@withContext null
        val tracks = db.crateDao().tracksInCrate(crateId)
        val category = colorCategoryFor(crate)
        buildString {
            appendLine("Crate: ${crate.name}")
            appendLine("Estilo: ${category.name}")
            appendLine("Total de pistas: ${tracks.size}")
            appendLine()
            tracks.forEachIndexed { index, track ->
                appendLine("${index + 1}. ${track.artist} - ${track.title}")
                val details = listOfNotNull(
                    track.bpm?.let { "${it.toInt()} BPM" },
                    track.musicalKey?.takeIf { it.isNotBlank() },
                    track.genre?.takeIf { it.isNotBlank() },
                )
                if (details.isNotEmpty()) {
                    appendLine("   • ${details.joinToString(" · ")}")
                }
                track.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    appendLine("   • Nota: $comment")
                }
            }
        }
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

}

private fun normalizeKey(value: String?): String? = value?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

private enum class HomePage(val title: String) {
    Menu("Inicio"),
    CreateCrate("Crear un nuevo crate"),
    ViewCrates("Ver mis crates"),
    CreateSetlist("Crear setlist"),
    ViewSetlists("Ver mis setlists"),
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
    launchSetlistExporter: (String) -> Unit,
    registerOnSetlistExported: ((Uri) -> Unit) -> Unit,
    onOpenCrate: (Long) -> Unit,
    vm: HomeVm,
) {
    val snackbarScope = rememberCoroutineScope()
    val snackbarHost by rememberUpdatedState(snackbarHostState)
    val context = LocalContext.current

    val crateSummaries = vm.crateSummaries
    val selectedCrate = vm.selectedCrate
    val loading = vm.loading
    val libraryTracks = vm.libraryTracks
    val trackTags = vm.trackTags
    val allTags = vm.allTags
    val setlists = vm.setlists
    val setlistDetails = vm.setlistDetails

    val filteredTracks = vm.filteredTracks()

    var newCrateName by remember { mutableStateOf("") }
    var newCrateCategory by remember { mutableStateOf<TechnoStyle?>(null) }
    var colorFilter by remember { mutableStateOf<CrateColorCategory?>(null) }
    var crateToEdit by remember { mutableStateOf<Crate?>(null) }
    var crateToDelete by remember { mutableStateOf<Crate?>(null) }
    var editCrateName by remember { mutableStateOf("") }
    var editCrateCategory by remember { mutableStateOf<TechnoStyle?>(null) }
    var setlistName by remember { mutableStateOf("Set Ibiza 2025") }
    var globalSetlistName by remember { mutableStateOf("Set Global 2025") }
    var expandedSetlistId by remember { mutableStateOf<Long?>(null) }
    var currentPage by remember { mutableStateOf(HomePage.Menu) }
    val selectedTrackIds = remember { mutableStateListOf<Long>() }
    val globalSelectedTrackIds = remember { mutableStateListOf<Long>() }
    var tagEditorTarget by remember { mutableStateOf<Track?>(null) }
    var globalSearchQuery by remember { mutableStateOf("") }

    fun moveTrack(list: MutableList<Long>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
    }

    fun requestImport(crate: Crate) {
        registerOnCsvPicked { uri ->
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
        launchCsvPicker()
    }

    fun requestCsvExport(crate: Crate) {
        registerOnCsvExported { uri ->
            vm.exportCrateToCsv(crate.id, uri) { count ->
                snackbarScope.launch {
                    snackbarHost.showSnackbar("Exportadas $count pistas a CSV")
                }
            }
        }
        launchCsvExporter("${crate.name}.csv")
    }

    fun requestJsonExport(crate: Crate) {
        registerOnJsonExported { uri ->
            vm.exportCrateToJson(crate.id, uri) { count ->
                snackbarScope.launch {
                    snackbarHost.showSnackbar("Exportadas $count pistas en JSON multi-formato")
                }
            }
        }
        launchJsonExporter("${crate.name}_pro.json")
    }

    fun shareCrate(crate: Crate) {
        snackbarScope.launch {
            val shareText = vm.buildCrateShareText(crate.id)
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

    LaunchedEffect(selectedCrate?.id) {
        selectedTrackIds.clear()
        selectedCrate?.let { setlistName = "Set ${it.name}" }
        tagEditorTarget = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            if (currentPage == HomePage.Menu) {
                HomeHeroSection(
                    crateCount = crateSummaries.size,
                    setlistCount = setlists.size,
                    selectedCrateName = selectedCrate?.name,
                    onImport = selectedCrate?.let { { requestImport(it) } },
                    onExport = selectedCrate?.let { { requestCsvExport(it) } },
                )
            } else {
                PageHeader(
                    title = currentPage.title,
                    onBack = { currentPage = HomePage.Menu },
                )
            }
        }

        when (currentPage) {
            HomePage.Menu -> {
                item {
                    HomeNavigationMenu(
                        onNavigate = { destination -> currentPage = destination },
                    )
                }
            }
            HomePage.CreateCrate -> {
                item {
                    SectionCard(
                        title = "Crea un nuevo crate",
                        description = "Define nombre y estilo para preparar tus mezclas.",
                    ) {
                        val canCreateCrate = newCrateName.isNotBlank() && newCrateCategory != null
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newCrateName,
                                onValueChange = { newCrateName = it },
                                label = { Text("Nombre del crate") },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val style = newCrateCategory ?: return@Button
                                    vm.createCrate(newCrateName, style) {
                                        newCrateName = ""
                                        newCrateCategory = null
                                    }
                                },
                                enabled = canCreateCrate,
                            ) {
                                Text("Crear")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Color y estilo del crate", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TechnoStyle.values().forEach { style ->
                                val styleColor = Color(style.colorHex)
                                FilterChip(
                                    selected = newCrateCategory == style,
                                    onClick = {
                                        newCrateCategory = if (newCrateCategory == style) null else style
                                    },
                                    label = { Text(style.displayName) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(styleColor),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            HomePage.ViewCrates -> {
                item {
                    SectionCard(
                        title = "Tus crates",
                        description = "Filtra por color y accede rápido a cada crate.",
                    ) {
                        val crateColorCategories = remember(crateSummaries) {
                            crateSummaries.map { colorCategoryFor(it.crate) }.distinctBy { it.id ?: it.name }
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
                                        selected = colorFilter?.let { it.id == category.id } ?: false,
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
                                val crateCategory = colorCategoryFor(crate)
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
                                            Text(
                                                crate.name.take(1).uppercase(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.headlineMedium,
                                            )
                                        }
                                        Text(crate.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            crateCategory.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = crateCategory.color,
                                        )
                                        Text("${summary.trackCount} pistas", style = MaterialTheme.typography.bodySmall)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CrateActionChip(
                                                label = "Ver crate",
                                                icon = Icons.Filled.LibraryMusic,
                                                onClick = {
                                                    vm.selectCrate(crate)
                                                    onOpenCrate(crate.id)
                                                },
                                            )
                                            CrateActionChip(
                                                label = "Importar CSV",
                                                icon = Icons.Filled.CloudUpload,
                                                onClick = { requestImport(crate) },
                                            )
                                            CrateActionChip(
                                                label = "Exportar CSV",
                                                icon = Icons.Filled.CloudDownload,
                                                onClick = { requestCsvExport(crate) },
                                            )
                                            CrateActionChip(
                                                label = "Exportar crate",
                                                icon = Icons.Filled.InsertDriveFile,
                                                onClick = { requestJsonExport(crate) },
                                            )
                                            CrateActionChip(
                                                label = "Editar crate",
                                                icon = Icons.Filled.Edit,
                                                onClick = {
                                                    crateToEdit = crate
                                                    editCrateName = crate.name
                                                    editCrateCategory = TechnoStyle.fromId(crate.colorCategory)
                                                        ?: TechnoStyle.TECHNO_HOUSE
                                                },
                                            )
                                            CrateActionChip(
                                                label = "Borrar crate",
                                                icon = Icons.Filled.Delete,
                                                onClick = { crateToDelete = crate },
                                            )
                                            CrateActionChip(
                                                label = "Compartir crate",
                                                icon = Icons.Filled.Share,
                                                onClick = { shareCrate(crate) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectedCrate?.let { crate ->
                    item {
                        SectionCard(
                            title = "Tracks en ${crate.name}",
                            description = "Filtra, etiqueta y prepara setlists sin perderte.",
                        ) {
                            if (loading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = vm.searchQuery,
                                    onValueChange = { vm.searchQuery = it },
                                    label = { Text("Buscar") },
                                    modifier = Modifier.weight(1f),
                                )
                                Button(onClick = { vm.clearFilters() }) { Text("Limpiar filtros") }
                            }
                            Spacer(Modifier.height(8.dp))

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
                                            if (isSelected) {
                                                if (track.id !in selectedTrackIds) {
                                                    selectedTrackIds.add(track.id)
                                                }
                                            } else {
                                                selectedTrackIds.remove(track.id)
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
                                Button(onClick = { requestCsvExport(crate) }) { Text("Exportar CSV") }
                                Button(onClick = { requestImport(crate) }) { Text("Importar CSV") }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text("Integración Rekordbox / Serato / Traktor", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { requestJsonExport(crate) }) { Text("Exportar JSON compatible") }
                        }
                    }
                }
            }
            HomePage.CreateSetlist -> {
                if (selectedCrate == null) {
                    item {
                        SectionCard(
                            title = "Selecciona un crate primero",
                            description = "Para crear setlists con filtros necesitas escoger un crate activo.",
                        ) {
                            Button(onClick = { currentPage = HomePage.ViewCrates }) {
                                Text("Ir a mis crates")
                            }
                        }
                    }
                } else {
                    item {
                        SectionCard(
                            title = "Tracks en ${selectedCrate.name}",
                            description = "Filtra, etiqueta y prepara setlists sin perderte.",
                        ) {
                            if (loading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = vm.searchQuery,
                                    onValueChange = { vm.searchQuery = it },
                                    label = { Text("Buscar") },
                                    modifier = Modifier.weight(1f),
                                )
                                Button(onClick = { vm.clearFilters() }) { Text("Limpiar filtros") }
                            }
                            Spacer(Modifier.height(8.dp))

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
                                            if (isSelected) {
                                                if (track.id !in selectedTrackIds) {
                                                    selectedTrackIds.add(track.id)
                                                }
                                            } else {
                                                selectedTrackIds.remove(track.id)
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
                            SelectedTrackList(
                                title = "Orden del setlist",
                                tracks = selectedTrackIds.mapNotNull { trackId ->
                                    filteredTracks.find { it.id == trackId }
                                },
                                onMoveUp = { index -> moveTrack(selectedTrackIds, index, index - 1) },
                                onMoveDown = { index -> moveTrack(selectedTrackIds, index, index + 1) },
                                onRemove = { trackId -> selectedTrackIds.remove(trackId) },
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = setlistName,
                                onValueChange = { setlistName = it },
                                label = { Text("Nombre del setlist") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                onClick = {
                                    val trackLookup = filteredTracks.associateBy { it.id }
                                    val tracks = selectedTrackIds.mapNotNull { trackLookup[it] }
                                    if (tracks.isEmpty()) {
                                        snackbarScope.launch {
                                            snackbarHost.showSnackbar("Selecciona pistas para el setlist")
                                        }
                                        return@Button
                                    }
                                    vm.createSetlist(setlistName.trim(), tracks) {
                                        snackbarScope.launch {
                                            snackbarHost.showSnackbar(
                                                "Setlist \"$setlistName\" guardado (${tracks.size} pistas)",
                                            )
                                            selectedTrackIds.clear()
                                        }
                                    }
                                },
                            ) {
                                Text("Crear setlist")
                            }
                        }
                    }
                }

                item {
                    SectionCard(
                        title = "Crear setlist desde la biblioteca",
                        description = "Arma setlists sin depender de un crate específico.",
                    ) {
                        OutlinedTextField(
                            value = globalSearchQuery,
                            onValueChange = { globalSearchQuery = it },
                            label = { Text("Buscar en biblioteca") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        val filteredLibraryTracks = remember(libraryTracks, globalSearchQuery) {
                            val query = globalSearchQuery.trim().lowercase()
                            if (query.isEmpty()) {
                                libraryTracks
                            } else {
                                libraryTracks.filter { track ->
                                    track.title.lowercase().contains(query) || track.artist.lowercase().contains(query)
                                }
                            }
                        }
                        if (filteredLibraryTracks.isEmpty()) {
                            Text("No hay pistas que coincidan con tu búsqueda.")
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(filteredLibraryTracks.take(12), key = { it.id }) { track ->
                                    TrackSelectChip(
                                        track = track,
                                        selected = track.id in globalSelectedTrackIds,
                                        onToggle = {
                                            if (track.id in globalSelectedTrackIds) {
                                                globalSelectedTrackIds.remove(track.id)
                                            } else {
                                                globalSelectedTrackIds.add(track.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SelectedTrackList(
                            title = "Orden del setlist global",
                            tracks = globalSelectedTrackIds.mapNotNull { trackId ->
                                libraryTracks.firstOrNull { it.id == trackId }
                            },
                            onMoveUp = { index -> moveTrack(globalSelectedTrackIds, index, index - 1) },
                            onMoveDown = { index -> moveTrack(globalSelectedTrackIds, index, index + 1) },
                            onRemove = { trackId -> globalSelectedTrackIds.remove(trackId) },
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = globalSetlistName,
                            onValueChange = { globalSetlistName = it },
                            label = { Text("Nombre del setlist global") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            onClick = {
                                val trackLookup = libraryTracks.associateBy { it.id }
                                val tracks = globalSelectedTrackIds.mapNotNull { trackLookup[it] }
                                if (tracks.isEmpty()) {
                                    snackbarScope.launch { snackbarHost.showSnackbar("Selecciona pistas para el setlist global") }
                                    return@Button
                                }
                                vm.createSetlist(globalSetlistName.trim(), tracks) {
                                    snackbarScope.launch {
                                        snackbarHost.showSnackbar(
                                            "Setlist \"$globalSetlistName\" guardado (${tracks.size} pistas)",
                                        )
                                        globalSelectedTrackIds.clear()
                                    }
                                }
                            },
                        ) {
                            Text("Crear setlist global")
                        }
                    }
                }
            }
            HomePage.ViewSetlists -> {
                item {
                    SectionCard(
                        title = "Setlists guardados",
                        description = "Consulta, exporta y detalla tus playlists.",
                    ) {
                        setlists.forEach { summary ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                onClick = {
                                    expandedSetlistId = if (expandedSetlistId == summary.setlist.id) null else summary.setlist.id
                                    vm.loadSetlistTracks(summary.setlist.id)
                                },
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(summary.setlist.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${summary.trackCount} pistas", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        SetlistExportFormat.values().forEach { format ->
                                            AssistChip(
                                                onClick = {
                                                    registerOnSetlistExported { uri ->
                                                        vm.exportSetlist(summary.setlist.id, format, uri) { count ->
                                                            snackbarScope.launch {
                                                                snackbarHost.showSnackbar(
                                                                    "Exportadas $count pistas del setlist a CSV ${format.displayName}",
                                                                )
                                                            }
                                                        }
                                                    }

                                                    launchSetlistExporter("${summary.setlist.name}_${format.fileSuffix}.csv")
                                                },
                                                label = { Text("Exportar ${format.displayName}") },
                                            )
                                        }
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
                                                                    snackbarHost.showSnackbar(
                                                                        "Etiqueta \"${tag.name}\" eliminada de ${track.title}",
                                                                    )
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
                }
            }
        }
    }

    crateToEdit?.let { crate ->
        AlertDialog(
            onDismissRequest = { crateToEdit = null },
            title = { Text("Editar crate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editCrateName,
                        onValueChange = { editCrateName = it },
                        label = { Text("Nombre del crate") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Color y estilo del crate", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TechnoStyle.values().forEach { style ->
                                val styleColor = Color(style.colorHex)
                                FilterChip(
                                    selected = editCrateCategory == style,
                                    onClick = { editCrateCategory = style },
                                    label = { Text(style.displayName) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(styleColor),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val style = editCrateCategory ?: return@TextButton
                    vm.updateCrate(crate.id, editCrateName, style) {
                        crateToEdit = null
                        snackbarScope.launch { snackbarHost.showSnackbar("Crate actualizado") }
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { crateToEdit = null }) { Text("Cancelar") }
            },
        )
    }

    crateToDelete?.let { crate ->
        AlertDialog(
            onDismissRequest = { crateToDelete = null },
            title = { Text("Eliminar crate") },
            text = {
                Text("Se eliminará el crate \"${crate.name}\" y sus asignaciones de pistas.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCrate(crate) {
                        crateToDelete = null
                        snackbarScope.launch { snackbarHost.showSnackbar("Crate eliminado") }
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { crateToDelete = null }) { Text("Cancelar") }
            },
        )
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
    }
}

@Composable
private fun PageHeader(
    title: String,
    onBack: () -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Elige acciones rápidas desde el menú principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeNavigationMenu(
    onNavigate: (HomePage) -> Unit,
) {
    SectionCard(
        title = "¿Qué quieres hacer hoy?",
        description = "Navega por bloques para ir directo a las funciones principales.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeNavigationCard(
                title = "Crear un nuevo crate",
                description = "Añade un nuevo crate y define su estilo.",
                icon = Icons.Filled.Add,
                onClick = { onNavigate(HomePage.CreateCrate) },
            )
            HomeNavigationCard(
                title = "Ver mis crates",
                description = "Gestiona tus crates existentes y sus pistas.",
                icon = Icons.Filled.LibraryMusic,
                onClick = { onNavigate(HomePage.ViewCrates) },
            )
            HomeNavigationCard(
                title = "Crear setlist",
                description = "Construye un setlist desde crates o biblioteca.",
                icon = Icons.Filled.Edit,
                onClick = { onNavigate(HomePage.CreateSetlist) },
            )
            HomeNavigationCard(
                title = "Ver mis setlists",
                description = "Revisa y exporta setlists guardados.",
                icon = Icons.Filled.InsertDriveFile,
                onClick = { onNavigate(HomePage.ViewSetlists) },
            )
        }
    }
}

@Composable
private fun HomeNavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CrateActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null)
        },
    )
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

@Composable
private fun TrackSelectChip(
    track: Track,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    AssistChip(
        onClick = onToggle,
        label = {
            Text(
                "${track.artist} · ${track.title}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        },
    )
}

@Composable
private fun SelectedTrackList(
    title: String,
    tracks: List<Track>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Long) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    if (tracks.isEmpty()) {
        Text("Aún no hay pistas seleccionadas.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tracks.forEachIndexed { index, track ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.bodyMedium)
                        Text(track.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(
                        onClick = { onMoveUp(index) },
                        enabled = index > 0,
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Mover arriba")
                    }
                    IconButton(
                        onClick = { onMoveDown(index) },
                        enabled = index < tracks.lastIndex,
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Mover abajo")
                    }
                    TextButton(onClick = { onRemove(track.id) }) {
                        Text("Quitar")
                    }
                }
            }
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
private fun HomeHeroSection(
    crateCount: Int,
    setlistCount: Int,
    selectedCrateName: String?,
    onImport: (() -> Unit)?,
    onExport: (() -> Unit)?,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
        ),
    )

    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DJ Crate Curator", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Dale espacio a tus nuevas ideas mientras gestionas crates, etiquetas y setlists con más claridad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill(label = "Crates", value = crateCount.toString())
                    StatPill(label = "Setlists", value = setlistCount.toString())
                    selectedCrateName?.let { StatPill(label = "Crate activo", value = it) }
                }

                /**Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onExport?.invoke() }, enabled = onExport != null) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar rápido")
                }
                Button(onClick = { onImport?.invoke() }, enabled = onImport != null) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar pistas")
                }
                }**/
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
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

data class CrateColorCategory(val id: String?, val name: String, val color: Color) {
    fun matches(crate: Crate): Boolean {
        val crateStyle = crate.colorCategory
        return if (id.isNullOrBlank()) {
            crateStyle.isNullOrBlank()
        } else {
            crateStyle?.equals(id, ignoreCase = true) == true
        }
    }
}

private fun colorForCrate(crate: Crate): Color =
    TechnoStyle.fromId(crate.colorCategory)?.let { Color(it.colorHex) } ?: autoColorForCrate(crate)

private fun colorCategoryFor(crate: Crate): CrateColorCategory {
    val style = TechnoStyle.fromId(crate.colorCategory)
    return when {
        style != null -> CrateColorCategory(style.id, style.displayName, Color(style.colorHex))
        !crate.colorCategory.isNullOrBlank() -> {
            val customName = formatStyleName(crate.colorCategory)
            CrateColorCategory(crate.colorCategory, customName, autoColorForCrate(crate))
        }
        else -> CrateColorCategory(null, "Sin estilo", autoColorForCrate(crate))
    }
}

private fun autoColorForCrate(crate: Crate): Color {
    val hue = (crate.name.hashCode().absoluteValue % 360).toFloat()
    return Color.hsl(hue, 0.5f, 0.45f)
}

private fun formatStyleName(raw: String): String {
    val parts = raw
        .lowercase(Locale.getDefault())
        .split('-', '_')
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) {
        return raw
    }
    return parts.joinToString(" ") { part ->
        part.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }
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
