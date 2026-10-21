package com.mihraya.app.feature.quran

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaDialog
import com.mihraya.app.core.designsystem.components.MihrayaSheet
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.frosted
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.Surah
import com.mihraya.app.data.quran.Verse
import com.mihraya.app.data.quran.VerseRef
import com.mihraya.app.data.quran.arabicDigits
import com.mihraya.app.data.settings.QuranScript
import com.mihraya.app.data.settings.ReaderLayout
import com.mihraya.app.data.settings.ReaderPreferences
import com.mihraya.app.feature.home.ProgressLine
import com.mihraya.app.ui.LocalBottomInset
import com.mihraya.app.ui.mihrayaViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PARAGRAPH_SIZE = 6

@OptIn(FlowPreview::class)
@Composable
fun ReaderScreen(
    surah: Int,
    ayah: Int,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onManageTranslations: () -> Unit,
) {
    val vm = mihrayaViewModel { ReaderViewModel(it, surah, ayah) }
    val content by vm.content.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val colors = Mihraya.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haze = remember { HazeState() }
    val view = LocalView.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = rememberHaptics()

    var chromeVisible by remember { mutableStateOf(true) }
    var showPreferences by remember { mutableStateOf(false) }
    var showGoTo by remember { mutableStateOf(false) }
    var selectedVerse by remember { mutableStateOf<Verse?>(null) }

    val current = content
    val layout = current?.prefs?.layout ?: ReaderLayout.VerseByVerse

    DisposableEffect(current?.prefs?.keepScreenOn) {
        view.keepScreenOn = current?.prefs?.keepScreenOn == true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(current?.surah?.number, layout) {
        val c = current ?: return@LaunchedEffect
        val target = vm.consumePendingAyah()
        listState.scrollToItem(if (target != null) itemIndexFor(target, c.prefs.layout) else 0)
    }

    val visibleAyah by remember(current, layout) {
        derivedStateOf {
            val count = current?.surah?.verseCount ?: 1
            ayahForIndex(listState.firstVisibleItemIndex, layout).coerceIn(1, count)
        }
    }

    LaunchedEffect(current?.surah?.number) {
        val number = current?.surah?.number ?: return@LaunchedEffect
        snapshotFlow { visibleAyah }
            .distinctUntilChanged()
            .debounce(700)
            .collect { vm.saveProgress(number, it) }
    }

    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) chromeVisible = false
                if (available.y > 6f) chromeVisible = true
                return Offset.Zero
            }
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (current == null) {
            CircularProgressIndicator(
                color = colors.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
        } else {
            val isPlayingThis = playback.surah == current.surah.number && playback.isPlaying
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(haze)
                    .nestedScroll(hideOnScroll),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = topInset + 84.dp, bottom = LocalBottomInset.current + 32.dp),
            ) {
                item(key = "header-${current.surah.number}") {
                    SurahHeader(
                        surah = current.surah,
                        bismillah = current.bismillah.takeIf { QuranMeta.hasBismillahHeader(current.surah.number) },
                        prefs = current.prefs,
                        isPlaying = isPlayingThis,
                        reciter = vm.preferredReciterName,
                        onPlay = { vm.playSurah(current.surah.number) },
                    )
                }
                when (layout) {
                    ReaderLayout.VerseByVerse -> itemsIndexed(current.verses, key = { _, v -> v.id }) { _, verse ->
                        VerseItem(
                            verse = verse,
                            prefs = current.prefs,
                            bookmarked = verse.ref in bookmarks,
                            onBookmark = {
                                haptics.tick()
                                vm.toggleBookmark(verse.ref)
                            },
                            onCopy = { copyVerse(context, clipboard::setText, verse, current.surah) },
                            onShare = { shareVerse(context, verse, current.surah) },
                        )
                    }
                    ReaderLayout.Continuous -> {
                        val paragraphs = current.verses.chunked(PARAGRAPH_SIZE)
                        itemsIndexed(paragraphs, key = { _, p -> "p${p.first().id}" }) { _, paragraph ->
                            ContinuousParagraph(
                                verses = paragraph,
                                prefs = current.prefs,
                                selected = selectedVerse?.id,
                                onSelect = { selectedVerse = it },
                            )
                        }
                    }
                }
                item(key = "footer-${current.surah.number}") {
                    SurahFooter(
                        surah = current.surah,
                        onOpen = { number ->
                            vm.openSurah(number)
                            scope.launch { listState.scrollToItem(0) }
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                ReaderTopBar(
                    surah = current.surah,
                    ayah = visibleAyah,
                    haze = haze,
                    isPlaying = isPlayingThis,
                    onBack = onBack,
                    onPlay = { vm.playSurah(current.surah.number) },
                    onGoTo = { showGoTo = true },
                    onPreferences = { showPreferences = true },
                )
            }
        }
    }

    if (showPreferences && current != null) {
        MihrayaSheet(onDismiss = { showPreferences = false }, title = "Reading preferences") {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                ReaderPreferencesPanel(
                    prefs = current.prefs,
                    onChange = vm::updatePrefs,
                    onManageTranslations = {
                        showPreferences = false
                        onManageTranslations()
                    },
                )
            }
        }
    }

    if (showGoTo && current != null) {
        GoToVerseDialog(
            surah = current.surah,
            onDismiss = { showGoTo = false },
            onGo = { target ->
                showGoTo = false
                chromeVisible = true
                scope.launch { listState.animateScrollToItem(itemIndexFor(target, layout)) }
            },
        )
    }

    selectedVerse?.let { verse ->
        val surahInfo = QuranMeta.surah(verse.surah)
        MihrayaSheet(onDismiss = { selectedVerse = null }, title = "${surahInfo.name} · ${verse.ref.key}") {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                VerseBody(verse = verse, prefs = current?.prefs ?: ReaderPreferences(), includeTranslations = true)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        if (verse.ref in bookmarks) "Saved" else "Bookmark",
                        onClick = { vm.toggleBookmark(verse.ref) },
                        icon = if (verse.ref in bookmarks) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton("Copy", onClick = { copyVerse(context, clipboard::setText, verse, surahInfo) }, icon = Icons.Rounded.ContentCopy)
                    SecondaryButton("Share", onClick = { shareVerse(context, verse, surahInfo) }, icon = Icons.Rounded.Share)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun itemIndexFor(ayah: Int, layout: ReaderLayout): Int = when (layout) {
    ReaderLayout.VerseByVerse -> ayah
    ReaderLayout.Continuous -> 1 + (ayah - 1) / PARAGRAPH_SIZE
}

private fun ayahForIndex(index: Int, layout: ReaderLayout): Int = when (layout) {
    ReaderLayout.VerseByVerse -> index.coerceAtLeast(1)
    ReaderLayout.Continuous -> (index - 1).coerceAtLeast(0) * PARAGRAPH_SIZE + 1
}

@Composable
private fun ReaderTopBar(
    surah: Surah,
    ayah: Int,
    haze: HazeState,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onGoTo: () -> Unit,
    onPreferences: () -> Unit,
) {
    val colors = Mihraya.colors
    Box(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .frosted(haze, RoundedCornerShape(26.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, filled = false)
                Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(surah.name, style = Mihraya.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Juzʾ ${QuranMeta.juzOf(surah.number, ayah)} · Verse $ayah of ${surah.verseCount}",
                        style = Mihraya.type.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
                CircleIconButton(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (isPlaying) "Pause recitation" else "Play recitation",
                    onPlay,
                    filled = false,
                    tint = colors.accent,
                )
                CircleIconButton(Icons.Rounded.Tag, "Go to verse", onGoTo, filled = false)
                CircleIconButton(Icons.Rounded.TextFields, "Reading preferences", onPreferences, filled = false)
            }
            ProgressLine(
                ayah.toFloat() / surah.verseCount,
                Modifier
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun SurahHeader(
    surah: Surah,
    bismillah: String?,
    prefs: ReaderPreferences,
    isPlaying: Boolean,
    reciter: String,
    onPlay: () -> Unit,
) {
    val colors = Mihraya.colors
    Column(
        Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "سورة ${surah.arabicName}",
            style = TextStyle(
                fontFamily = ArabicFonts.Amiri,
                fontSize = 34.sp,
                lineHeight = 56.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Center,
            ),
            color = colors.accent,
        )
        Text(surah.name, style = Mihraya.type.title, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "${surah.meaning} · ${surah.revelation.label} · ${surah.verseCount} verses",
            style = Mihraya.type.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        SecondaryButton(
            text = if (isPlaying) "Pause · $reciter" else "Listen · $reciter",
            onClick = onPlay,
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            tint = colors.accent,
        )
        if (bismillah != null) {
            Spacer(Modifier.height(26.dp))
            Text(
                bismillah,
                style = quranStyle(prefs).copy(fontSize = (prefs.arabicSize * 0.95f).sp, textAlign = TextAlign.Center),
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        HairlineDivider()
    }
}

@Composable
private fun quranStyle(prefs: ReaderPreferences): TextStyle = TextStyle(
    fontFamily = ArabicFonts.quran(prefs.arabicFont),
    fontSize = prefs.arabicSize.sp,
    lineHeight = (prefs.arabicSize * prefs.lineHeight).sp,
    textDirection = TextDirection.Rtl,
    textAlign = TextAlign.Right,
)

private fun ayahMarker(ayah: Int, script: QuranScript): String =
    "\u06DD" + ayah.arabicDigits(extended = script == QuranScript.IndoPak)

@Composable
private fun VerseItem(
    verse: Verse,
    prefs: ReaderPreferences,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = Mihraya.colors
    Column(
        Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(colors.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        verse.ref.key,
                        style = Mihraya.type.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = colors.accent,
                    )
                }
                Spacer(Modifier.weight(1f))
                CircleIconButton(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    if (bookmarked) "Remove bookmark" else "Bookmark verse",
                    onBookmark,
                    size = 36.dp,
                    iconSize = 18.dp,
                    filled = false,
                    tint = if (bookmarked) colors.accent else colors.textTertiary,
                )
                CircleIconButton(Icons.Rounded.ContentCopy, "Copy verse", onCopy, size = 36.dp, iconSize = 17.dp, filled = false, tint = colors.textTertiary)
                CircleIconButton(Icons.Rounded.Share, "Share verse", onShare, size = 36.dp, iconSize = 17.dp, filled = false, tint = colors.textTertiary)
            }
            Spacer(Modifier.height(12.dp))
            VerseBody(verse, prefs, includeTranslations = prefs.showTranslation)
        }
        HairlineDivider(Modifier.padding(horizontal = 22.dp))
    }
}

@Composable
internal fun VerseBody(verse: Verse, prefs: ReaderPreferences, includeTranslations: Boolean) {
    val colors = Mihraya.colors
    Text(
        verse.arabic + " " + ayahMarker(verse.ayah, prefs.script),
        style = quranStyle(prefs),
        color = colors.textPrimary,
        modifier = Modifier.fillMaxWidth(),
    )
    verse.transliteration?.let {
        Spacer(Modifier.height(10.dp))
        Text(
            it,
            style = Mihraya.type.body.copy(fontStyle = FontStyle.Italic, fontSize = (prefs.translationSize - 1).sp),
            color = colors.accent.copy(alpha = 0.85f),
        )
    }
    if (includeTranslations) {
        verse.translations.forEach { translation ->
            Spacer(Modifier.height(12.dp))
            val urdu = translation.translation.isRtl
            Text(
                translation.text,
                style = Mihraya.type.body.copy(
                    fontFamily = ArabicFonts.translation(translation.translation),
                    fontSize = (if (urdu) prefs.translationSize + 2 else prefs.translationSize).sp,
                    lineHeight = (prefs.translationSize * if (urdu) 2.3f else 1.6f).sp,
                    textDirection = if (urdu) TextDirection.Rtl else TextDirection.Ltr,
                    textAlign = if (urdu) TextAlign.Right else TextAlign.Left,
                ),
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            if (verse.translations.size > 1) {
                Text(
                    translation.translation.name,
                    style = Mihraya.type.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ContinuousParagraph(
    verses: List<Verse>,
    prefs: ReaderPreferences,
    selected: Int?,
    onSelect: (Verse) -> Unit,
) {
    val colors = Mihraya.colors
    val text = remember(verses, prefs.script, selected, colors.accentSoft, colors.accent) {
        buildAnnotatedString {
            verses.forEach { verse ->
                withLink(LinkAnnotation.Clickable(tag = verse.ref.key) { onSelect(verse) }) {
                    if (verse.id == selected) {
                        withStyle(SpanStyle(background = colors.accentSoft)) { append(verse.arabic) }
                    } else {
                        append(verse.arabic)
                    }
                }
                append(" ")
                withStyle(SpanStyle(color = colors.accent)) { append(ayahMarker(verse.ayah, prefs.script)) }
                append(" ")
            }
        }
    }
    Text(
        text,
        style = quranStyle(prefs).copy(textAlign = TextAlign.Justify),
        color = colors.textPrimary,
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
    )
}

@Composable
private fun SurahFooter(surah: Surah, onOpen: (Int) -> Unit) {
    Column(
        Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("صَدَقَ ٱللَّهُ ٱلْعَظِيمُ", style = TextStyle(fontFamily = ArabicFonts.Amiri, fontSize = 20.sp, textDirection = TextDirection.Rtl), color = Mihraya.colors.textTertiary)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (surah.number > 1) {
                SecondaryButton(QuranMeta.surah(surah.number - 1).name, onClick = { onOpen(surah.number - 1) }, modifier = Modifier.weight(1f))
            }
            if (surah.number < 114) {
                PrimaryButton("Next · ${QuranMeta.surah(surah.number + 1).name}", onClick = { onOpen(surah.number + 1) }, modifier = Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun GoToVerseDialog(surah: Surah, onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val number = text.toIntOrNull()
    val valid = number != null && number in 1..surah.verseCount
    val colors = Mihraya.colors
    MihrayaDialog(
        title = "Go to verse",
        message = "${surah.name} has ${surah.verseCount} verses.",
        onDismiss = onDismiss,
        confirmText = "Go",
        confirmEnabled = valid,
        onConfirm = { number?.let(onGo) },
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { value -> text = value.filter(Char::isDigit).take(3) },
            singleLine = true,
            label = { Text("Verse number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.glassBorder,
                focusedLabelColor = colors.accent,
                cursorColor = colors.accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun verseText(verse: Verse, surah: Surah): String = buildString {
    append(verse.arabic)
    verse.translations.forEach { append("\n\n").append(it.text) }
    append("\n\n— Quran, ${surah.name} ${verse.ref.key}")
}

private fun copyVerse(context: Context, setText: (AnnotatedString) -> Unit, verse: Verse, surah: Surah) {
    setText(AnnotatedString(verseText(verse, surah)))
    Toast.makeText(context, "Verse copied", Toast.LENGTH_SHORT).show()
}

private fun shareVerse(context: Context, verse: Verse, surah: Surah) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, verseText(verse, surah))
    context.startActivity(Intent.createChooser(send, "Share verse"))
}