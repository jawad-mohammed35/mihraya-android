package com.mihraya.app.feature.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.mihraya.app.core.designsystem.components.*
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.hadith.*
import com.mihraya.app.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HadithHomeScreen(onNavigate: (Any) -> Unit) {
    val repo = LocalAppContainer.current.hadith
    val changed by repo.changes.collectAsStateWithLifecycle()
    val prefs by repo.preferences.collectAsStateWithLifecycle()
    val initializationError by repo.initializationError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editions by remember { mutableStateOf<List<HadithEdition>>(emptyList()) }
    var installed by remember { mutableStateOf<List<HadithEdition>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(changed) {
        try { editions = repo.catalogue(); installed = repo.installed() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "The Hadith library could not be opened. Please try again." }
    }
    val collections = editions.filter { it.language == "en" }
    val state = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = libraryPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Hadith") }
        item { MihrayaTextField(query, { query = it }, "Search collections") }
        item { SecondaryButton("Search Hadith", { onNavigate(HadithListRoute()) }, Modifier.fillMaxWidth(), enabled = installed.isNotEmpty()) }
        item { SecondaryButton("Bookmarks · ${prefs.bookmarks.size}", { onNavigate(HadithListRoute(bookmarks = true)) }, Modifier.fillMaxWidth()) }
        item {
            PrimaryButton("Random Hadith", {
                busy = true
                scope.launch {
                    try { repo.random(prefs.language)?.let { onNavigate(HadithReaderRoute(it.collection, it.text.id)) } }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "Unable to open a random Hadith. Please try again." }
                    finally { busy = false }
                }
            }, Modifier.fillMaxWidth(), icon = MihrayaIcons.Hadith, enabled = installed.isNotEmpty(), loading = busy)
        }
        message?.let { item { LibraryNote(it) } }
        initializationError?.let { item { LibraryNote(it); SecondaryButton("Retry", { scope.launch { repo.ensureBundled() } }) } }
        if (collections.isEmpty()) {
            item {
                GlassCard(contentPadding = PaddingValues(22.dp)) {
                    Icon(MihrayaIcons.Hadith, null, tint = Mihraya.colors.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Texts are not available yet", style = Mihraya.type.title, color = Mihraya.colors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Mihraya will offer collections once their complete texts, references, and translation permissions have been checked. No partial collections are included.", style = Mihraya.type.body, color = Mihraya.colors.textSecondary)
                }
            }
        }
        items(collections.filter { foldHadith(it.name).contains(foldHadith(query)) }, key = { it.key }) { edition ->
            GlassCard(onClick = { onNavigate(HadithCollectionRoute(edition.collection)) }) {
                Icon(MihrayaIcons.Hadith, null, tint = Mihraya.colors.accent)
                Text(edition.name, style = Mihraya.type.title, color = Mihraya.colors.textPrimary)
                Text("${edition.count} Hadith · ${if (installed.any { it.key == edition.key }) "Available offline" else "Download to read"}", style = Mihraya.type.caption, color = Mihraya.colors.accent)
                Text(edition.description, style = Mihraya.type.bodySmall, color = Mihraya.colors.textSecondary)
            }
        }
        if (collections.isNotEmpty() && collections.none { foldHadith(it.name).contains(foldHadith(query)) }) {
            item { LibraryNote("No matching collections.") }
        }
    }
}

@Composable
fun HadithCollectionScreen(collection: String, onBack: () -> Unit, onNavigate: (Any) -> Unit) {
    val repo = LocalAppContainer.current.hadith
    val changed by repo.changes.collectAsStateWithLifecycle()
    val prefs by repo.preferences.collectAsStateWithLifecycle()
    val downloads by repo.downloads.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var catalogue by remember { mutableStateOf<List<HadithEdition>>(emptyList()) }
    var installed by remember { mutableStateOf<List<HadithEdition>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<HadithChapter>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var removal by remember { mutableStateOf<HadithEdition?>(null) }
    LaunchedEffect(collection, changed) {
        try {
            catalogue = repo.catalogue().filter { it.collection == collection }
            installed = repo.installed().filter { it.collection == collection }
            chapters = repo.chapters(collection)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "Unable to open this collection." }
    }
    val base = catalogue.firstOrNull { it.language == "en" }
    val ready = installed.any { it.language == "en" }
    Column(Modifier.fillMaxSize()) {
        TopBar(base?.name ?: "Collection", onBack)
        LazyColumn(state = rememberLazyListState(), contentPadding = libraryPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            base?.let { edition -> item {
                GlassCard {
                    Text(edition.description, style = Mihraya.type.body, color = Mihraya.colors.textPrimary)
                    SourceInfo(edition)
                }
            } }
            message?.let { item { LibraryNote(it) } }
            if (ready) {
                prefs.lastRead[collection]?.let { id -> item {
                    PrimaryButton("Continue reading", { onNavigate(HadithReaderRoute(collection, id)) }, Modifier.fillMaxWidth())
                } }
                item { SecondaryButton("Search this collection", { onNavigate(HadithListRoute(collection)) }, Modifier.fillMaxWidth()) }
                item { MihrayaTextField(query, { query = it }, "Find a chapter") }
                items(chapters.filter { foldHadith(it.name).contains(foldHadith(query)) }, key = { "chapter:${it.id}" }) { chapter ->
                    GlassCard(onClick = { onNavigate(HadithListRoute(collection, chapter.id)) }) {
                        Text(chapter.name, style = Mihraya.type.titleSmall, color = Mihraya.colors.textPrimary)
                        if (chapter.arabicName.isNotBlank()) Text(chapter.arabicName, modifier = Modifier.fillMaxWidth(), style = Mihraya.type.body.copy(fontFamily = ArabicFonts.Amiri, textDirection = TextDirection.Rtl, textAlign = TextAlign.Right, lineHeight = 36.sp), color = Mihraya.colors.textSecondary)
                        Text("${chapter.count} Hadith", style = Mihraya.type.caption, color = Mihraya.colors.textTertiary)
                    }
                }
            }
            item { SectionLabel("Offline editions") }
            items(catalogue, key = { it.key }) { edition ->
                val matchingWork = downloads.filter { edition.key in it.tags }
                val work = matchingWork.firstOrNull { !it.state.isFinished } ?: matchingWork.lastOrNull()
                val running = work != null && !work.state.isFinished
                val exists = installed.any { it.key == edition.key }
                GlassCard {
                    Text(edition.languageName, style = Mihraya.type.titleSmall, color = Mihraya.colors.textPrimary)
                    Text("${String.format(java.util.Locale.ROOT, "%.1f", edition.bytes / 1048576.0)} MB · ${if (exists) "Available offline" else "Not downloaded"}", style = Mihraya.type.caption, color = Mihraya.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    when {
                        running -> SecondaryButton(if (work!!.progress.getInt("percent", 0) >= 100) "Installing · Cancel" else "${work.progress.getInt("percent", 0)}% · Cancel", { repo.cancel(edition) })
                        exists && edition.bundledAsset.isNotBlank() -> Text("Included with Mihraya", style = Mihraya.type.label, color = Mihraya.colors.accent)
                        exists -> SecondaryButton("Remove ${edition.languageName}", { removal = edition })
                        else -> PrimaryButton("Download ${edition.languageName}", { repo.download(edition) }, enabled = edition.language == "en" || ready)
                    }
                    if (work?.state == WorkInfo.State.FAILED) LibraryNote(work.outputData.getString("error") ?: "Download failed. Please retry.")
                }
            }
        }
    }
    removal?.let { edition ->
        // Removing a base edition also removes its translations; user bookmarks stay in their own store.
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removal = null },
            title = { Text("Remove downloaded text?") },
            text = { Text(if (edition.language == "en") "This removes English, Arabic, and downloaded translations for this collection. Your bookmarks and reading position are kept." else "Remove ${edition.languageName} from this device? Your bookmarks are kept.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                removal = null
                scope.launch { try { repo.remove(edition) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "Could not remove download. Please retry." } }
            }) { Text("Remove") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removal = null }) { Text("Keep") } },
        )
    }
}

@Composable
fun HadithListScreen(route: HadithListRoute, onBack: () -> Unit, onNavigate: (Any) -> Unit) {
    val repo = LocalAppContainer.current.hadith
    val prefs by repo.preferences.collectAsStateWithLifecycle()
    val changed by repo.changes.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableIntStateOf(50) }
    var results by remember { mutableStateOf<List<HadithHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) { limit = 50 }
    LaunchedEffect(query, limit, prefs.language, changed, route) {
        if (!route.bookmarks) {
            loading = true
            delay(280)
            try { results = repo.search(query, route.collection, route.chapter, prefs.language, limit); message = null }
            catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "Search could not be completed. Please retry." }
            finally { loading = false }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(if (route.bookmarks) "Hadith bookmarks" else "Hadith", onBack)
        LazyColumn(state = rememberLazyListState(), contentPadding = libraryPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MihrayaTextField(query, { query = it }, if (route.bookmarks) "Search collection or reference" else "Search Arabic, translation, or reference") }
            message?.let { item { LibraryNote(it) } }
            if (route.bookmarks) {
                val bookmarks = prefs.bookmarks.filter { foldHadith("${it.name} ${it.reference}").contains(foldHadith(query)) }
                if (bookmarks.isEmpty()) item { LibraryNote("No matching bookmarks. Save a Hadith from its reader to keep it here.") }
                items(bookmarks, key = { "${it.collection}:${it.id}" }) { bookmark ->
                    GlassCard(onClick = { onNavigate(HadithReaderRoute(bookmark.collection, bookmark.id)) }) {
                        Text(bookmark.name, style = Mihraya.type.titleSmall, color = Mihraya.colors.textPrimary)
                        Text(bookmark.reference, style = Mihraya.type.bodySmall, color = Mihraya.colors.accent)
                        SecondaryButton("Remove bookmark", { repo.removeBookmark(bookmark) })
                    }
                }
            } else {
                if (loading) item { LibraryNote("Searching…") }
                else if (results.isEmpty()) item { LibraryNote("No Hadith found in downloaded collections.") }
                items(results.take(limit), key = { "${it.collection}:${it.text.id}" }) { hit ->
                    GlassCard(onClick = { onNavigate(HadithReaderRoute(hit.collection, hit.text.id)) }) {
                        Text(hit.text.reference, style = Mihraya.type.titleSmall, color = Mihraya.colors.accent)
                        Text(if (hit.text.translationAvailable) hit.text.translation else "Translation unavailable in this source · Arabic text available", maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = Mihraya.type.body, color = Mihraya.colors.textPrimary)
                    }
                }
                if (results.size > limit) item { SecondaryButton("Load more", { limit += 50 }, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
fun HadithReaderScreen(collection: String, id: String, onBack: () -> Unit) {
    val repo = LocalAppContainer.current.hadith
    val prefs by repo.preferences.collectAsStateWithLifecycle()
    val changed by repo.changes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hit by remember { mutableStateOf<HadithHit?>(null) }
    var edition by remember { mutableStateOf<HadithEdition?>(null) }
    var chapter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var preferencesOpen by rememberSaveable { mutableStateOf(false) }
    var chainOpen by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scroll = rememberLazyListState()
    LaunchedEffect(collection, id, prefs.language, changed) {
        loading = true
        try {
            hit = repo.read(collection, id, prefs.language)
            hit?.let { h ->
                edition = repo.installed().firstOrNull { it.collection == collection && it.language == h.language }
                chapter = repo.chapters(collection).firstOrNull { it.id == h.text.chapter }?.name.orEmpty()
                repo.markRead(h)
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = "The Hadith could not be opened. Please try again." }
        finally { loading = false }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.sideChrome)) {
        TopBar("Hadith", onBack, subtitle = hit?.name)
        LazyColumn(state = scroll, contentPadding = libraryPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            message?.let { item { LibraryNote(it) } }
            val h = hit
            if (h == null) {
                item { LibraryNote(if (loading) "Opening…" else "Download this collection to read this Hadith. Your bookmark is still saved.") }
            } else {
                item {
                    Text(h.text.reference, style = Mihraya.type.title, color = Mihraya.colors.accent)
                    Text(chapter, style = Mihraya.type.bodySmall, color = Mihraya.colors.textSecondary)
                    Text("Hadith ${h.text.number}", style = Mihraya.type.caption, color = Mihraya.colors.textTertiary)
                    if (h.text.heading.isNotBlank()) Text(h.text.heading, style = Mihraya.type.bodySmall, color = Mihraya.colors.textSecondary)
                    if (h.text.headingArabic.isNotBlank()) Text(h.text.headingArabic, modifier = Modifier.fillMaxWidth(), style = Mihraya.type.body.copy(fontFamily = ArabicFonts.Amiri, textDirection = TextDirection.Rtl, textAlign = TextAlign.Right, lineHeight = 36.sp), color = Mihraya.colors.textSecondary)
                }
                item {
                    val bookmarked = prefs.bookmarks.any { it.collection == collection && it.id == id }
                    // Wrap controls on narrow screens and when system font scaling is large.
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(if (bookmarked) "Bookmarked" else "Bookmark", { repo.bookmark(h) })
                        SecondaryButton("Copy", {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(h.text.reference, shareText(h, edition)))
                            message = "Hadith copied."
                        })
                        SecondaryButton("Share", {
                            try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText(h, edition)) }, "Share Hadith")) }
                            catch (e: android.content.ActivityNotFoundException) { message = "No sharing app is available." }
                        })
                        SecondaryButton("Reading preferences", { preferencesOpen = !preferencesOpen })
                    }
                }
                if (preferencesOpen) item {
                    GlassCard {
                        Text("Arabic size · ${prefs.arabicSize}", color = Mihraya.colors.textPrimary)
                        Slider(prefs.arabicSize.toFloat(), { size -> repo.edit { it.copy(arabicSize = size.toInt().coerceIn(20, 44)) } }, valueRange = 20f..44f)
                        Text("Translation size · ${prefs.translationSize}", color = Mihraya.colors.textPrimary)
                        Slider(prefs.translationSize.toFloat(), { size -> repo.edit { it.copy(translationSize = size.toInt().coerceIn(14, 30)) } }, valueRange = 14f..30f)
                        var languages by remember { mutableStateOf<List<HadithEdition>>(emptyList()) }
                        LaunchedEffect(changed) { languages = repo.installed().filter { it.collection == collection } }
                        languages.forEach { lang -> SecondaryButton((if (prefs.language == lang.language) "✓ " else "") + lang.languageName, { repo.edit { it.copy(language = lang.language) } }) }
                    }
                }
                item {
                    Text(h.text.arabic, modifier = Modifier.fillMaxWidth(), color = Mihraya.colors.textPrimary,
                        style = Mihraya.type.body.copy(fontFamily = ArabicFonts.Amiri, fontSize = prefs.arabicSize.sp, lineHeight = (prefs.arabicSize * 2.2f).sp, textDirection = TextDirection.Rtl, textAlign = TextAlign.Right))
                }
                item {
                    GlassCard {
                        Text(edition?.languageName ?: "English", style = Mihraya.type.overline, color = Mihraya.colors.accent)
                        if (h.language != prefs.language) Text(if (h.text.translationAvailable) "Preferred translation unavailable; showing English." else "Preferred translation unavailable; showing the original Arabic.", style = Mihraya.type.caption, color = Mihraya.colors.textTertiary)
                        Spacer(Modifier.height(12.dp))
                        Text(if (h.text.translationAvailable) h.text.translation else "Translation unavailable for this narration in the source edition. The original Arabic is shown above.", color = Mihraya.colors.textPrimary, style = Mihraya.type.body.copy(fontSize = prefs.translationSize.sp, lineHeight = (prefs.translationSize * 1.7f).sp, textDirection = if (edition?.rtl == true) TextDirection.Rtl else TextDirection.Ltr))
                    }
                }
                if (h.text.grade.isNotBlank()) item { LibraryNote("Grading (as provided by source)\n${h.text.grade}") }
                if (h.text.narrator.isNotBlank()) item { LibraryNote("Narrator (as provided by source)\n${h.text.narrator}") }
                if (h.text.isnad.isNotBlank()) {
                    item { SecondaryButton(if (chainOpen) "Hide narration chain" else "Narration chain", { chainOpen = !chainOpen }) }
                    if (chainOpen) item { LibraryNote(h.text.isnad) }
                }
                edition?.let { item { SourceInfo(it) } }
            }
        }
    }
}

private fun shareText(hit: HadithHit, edition: HadithEdition?) = buildString {
    append(hit.text.arabic); append("\n\n")
    if (hit.text.narrator.isNotBlank()) append("${hit.text.narrator}\n")
    append(if (hit.text.translationAvailable) hit.text.translation else "[Translation unavailable in the source edition]")
    append("\n\n"); append(hit.text.reference)
    if (hit.text.grade.isNotBlank()) append("\n${hit.text.grade}")
    edition?.let { append("\n\n${it.attribution}\n${it.source}\nEdition: ${it.revision} · ${it.numbering}") }
}

@Composable
private fun SourceInfo(edition: HadithEdition) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryNote("Hadith API · ${edition.languageName} · ${edition.revision.take(7)}")
        SecondaryButton(if (expanded) "Hide source details" else "Source and edition", { expanded = !expanded })
        if (expanded) LibraryNote("${edition.attribution}\n${edition.source}\nEdition: ${edition.revision}\nNumbering: ${edition.numbering}\nTranslations available: ${edition.translatedCount} of ${edition.count}\n${edition.license}\n${edition.verification}")
    }
}

@Composable
private fun LibraryNote(text: String) {
    Text(text, style = Mihraya.type.bodySmall, color = Mihraya.colors.textSecondary)
}

@Composable
private fun libraryPadding() = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomInset.current + 24.dp)
