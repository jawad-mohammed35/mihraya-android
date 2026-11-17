package com.mihraya.app.feature.more

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mihraya.app.core.designsystem.components.*
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.designsystem.theme.overlaySurface
import com.mihraya.app.feature.settings.SettingsScaffold
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.ui.LocalBottomInset
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.random.Random

@Composable
internal fun ArabicName(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val font = LocalNamesArabicFont.current
    val size = if (large) 40 else 27
    Text(text, modifier.fillMaxWidth(), color = Mihraya.colors.textPrimary,
        style = Mihraya.type.body.copy(fontFamily = font.family(), fontSize = size.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, letterSpacing = 0.sp,
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = true),
            lineHeight = (size * font.lineSpacing).sp, textDirection = TextDirection.Rtl, textAlign = TextAlign.Center))
}
@Composable
fun NamesScreen(onBack: () -> Unit, onLearn: () -> Unit) = NamesTypography { NamesContent(onBack, onLearn) }

@Composable
private fun NamesContent(onBack: () -> Unit, onLearn: () -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val names = remember { NamesData.load(context) }
    val store = remember(context) { MoreStore(context) }
    val font = LocalNamesArabicFont.current
    var pickFont by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.sideChrome)) {
        TopBar("99 Names of Allah", onBack)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomInset.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item { PrimaryButton("Learn", onLearn, Modifier.widthIn(max = 640.dp).fillMaxWidth()) }
            item { SecondaryButton("Arabic font · ${font.label}", { pickFont = true }, Modifier.widthIn(max = 640.dp).fillMaxWidth()) }
            item { SearchField(query, { query = it }, "Search Arabic, transliteration or meaning", Modifier.widthIn(max = 640.dp).fillMaxWidth()) }
            val filtered = names.filter { NamesData.fold("${it.arabic} ${it.transliteration} ${it.meaning}").contains(NamesData.fold(query)) }
            if (filtered.isEmpty()) item { ToolText("No Names match your search.") }
            items(filtered, key = { it.number }) { name ->
                GlassCard(Modifier.widthIn(max = 640.dp).fillMaxWidth(), onClick = { focus.clearFocus(); selected = name.number }, contentPadding = PaddingValues(16.dp)) {
                    Text("${name.number} · ${name.transliteration}", style = Mihraya.type.titleSmall, color = Mihraya.colors.accent)
                    ArabicName(name.arabic)
                    ToolText(name.meaning)
                }
            }
        }
    }
    if (pickFont) OptionSheet("Arabic font", NamesArabicFont.entries.map { SheetOption(it, it.label, it.description) }, font,
        { store.setNamesFont(it); pickFont = false }, { pickFont = false })
    selected?.let { number ->
        val name = names.first { it.number == number }
        Dialog(onDismissRequest = { selected = null }) {
            GlassCard(Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 620.dp), fill = Mihraya.colors.overlaySurface()) {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ToolText("Name ${name.number} of 99", true)
                    ArabicName(name.arabic, large = true)
                    Text(name.transliteration, style = Mihraya.type.title, color = Mihraya.colors.textPrimary, textAlign = TextAlign.Center)
                    Text(name.meaning, style = Mihraya.type.body, color = Mihraya.colors.textSecondary, textAlign = TextAlign.Center)
                    SecondaryButton("Copy", { copyMore(context, name.shareText()) }, Modifier.fillMaxWidth())
                    SecondaryButton("Share", { shareMore(context, name.shareText()) }, Modifier.fillMaxWidth())
                    SecondaryButton("Close", { selected = null }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Serializable
private data class LearnSession(
    val id: String, val ids: List<Int>, val matching: Boolean, val direction: Direction,
    val randomized: Boolean, val questions: List<QuizQuestion>, val index: Int = 0,
    val correct: Int = 0, val answer: Int? = null, val round: Int = 0,
    val matched: List<Int> = emptyList(), val mistakes: Int = 0,
)
@Composable
fun NamesLearnScreen(onBack: () -> Unit) = NamesTypography { NamesLearnContent(onBack) }

@Composable
private fun NamesLearnContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val names = remember { NamesData.load(context) }
    val store = remember { MoreStore(context) }
    var progress by remember { mutableStateOf(store.progress()) }
    var count by rememberSaveable { mutableIntStateOf(10) }
    var direction by rememberSaveable { mutableStateOf(Direction.Mixed) }
    var randomized by rememberSaveable { mutableStateOf(true) }
    var encoded by rememberSaveable { mutableStateOf<String?>(null) }
    val session = remember(encoded) { encoded?.let { Json.decodeFromString<LearnSession>(it) } }
    fun update(value: LearnSession) { encoded = Json.encodeToString(LearnSession.serializer(), value) }
    var leave by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptics()
    fun feedback() { haptic.confirm() }
    val done = session != null && if (session.matching) session.matched.size == session.ids.size else session.index >= session.questions.size
    fun back() { if (session == null) onBack() else if (done) encoded = null else leave = true }
    BackHandler(session != null) { back() }
    LaunchedEffect(done, session?.id) {
        if (done && !session!!.matching) progress = store.record(session.id, session.ids, false, session.correct)
    }
    SettingsScaffold("Learn the Names", { back() }) {
        if (session == null) {
            ToolCard("Your practice") {
                ToolText("Quizzes completed: ${progress.quizzes} · Best score: ${progress.bestPercent}%\nNames correctly matched: ${progress.matches}")
                if (progress.recent.isNotEmpty()) ToolText("Recently practiced: " + progress.recent.map { id -> names.first { it.number == id }.transliteration }.joinToString(", "))
            }
            ToolCard("Customize your session") {
                ToolText("Number of Names / questions")
                // Individual rows remain readable at large font sizes.
                listOf(5, 10, 20, 50, 99).forEach { n -> SecondaryButton(if (n == 99) "${if (count == n) "✓ " else ""}All 99" else "${if (count == n) "✓ " else ""}$n Names", { count = n }, Modifier.fillMaxWidth()) }
                ToolText("Question direction")
                Direction.entries.forEach { d -> SecondaryButton("${if (direction == d) "✓ " else ""}${d.label}", { direction = d }, Modifier.fillMaxWidth()) }
                SettingsSwitchRow("Randomized order", randomized, { randomized = it })
                ToolText("With randomization off, practice starts from Name 1 in source order. Matching uses rounds of up to 5 pairs. Answer positions are shuffled in quizzes.")
                fun start(matching: Boolean) {
                    val ids = NamesGames.session(names, count, randomized)
                    update(LearnSession(UUID.randomUUID().toString(), ids, matching, direction, randomized,
                        if (matching) emptyList() else NamesGames.questions(names, ids, direction)))
                }
                PrimaryButton("Flashcard Matching", { start(true) }, Modifier.fillMaxWidth())
                PrimaryButton("Multiple Choice Quiz", { start(false) }, Modifier.fillMaxWidth())
            }
        } else if (done) {
            ToolCard("Session complete") {
                if (session.matching) {
                    ToolTitle("${session.matched.size} / ${session.ids.size} matched")
                    ToolText("Incorrect attempts: ${session.mistakes}")
                } else {
                    ToolTitle("${session.correct} / ${session.ids.size} · ${session.correct * 100 / session.ids.size}%")
                    ToolText("Correct: ${session.correct} · Incorrect: ${session.ids.size - session.correct}")
                }
                ToolText("Your learning progress is saved locally.")
                PrimaryButton("New session", { encoded = null }, Modifier.fillMaxWidth())
                SecondaryButton("Practice again", {
                    update(session.copy(id = UUID.randomUUID().toString(), questions = if (session.matching) emptyList() else NamesGames.questions(names, session.ids, session.direction),
                        index = 0, correct = 0, answer = null, round = 0, matched = emptyList(), mistakes = 0))
                }, Modifier.fillMaxWidth())
            }
        } else if (!session.matching) {
            val question = session.questions[session.index]
            val name = names.first { it.number == question.nameId }
            ToolCard("Question ${session.index + 1} of ${session.ids.size}") {
                val answeredCount = session.index + if (session.answer != null) 1 else 0
                LinearProgressIndicator(progress = { answeredCount.toFloat() / session.ids.size }, modifier = Modifier.fillMaxWidth(), color = Mihraya.colors.accent, trackColor = Mihraya.colors.accentSoft)
                ToolText("Correct: ${session.correct} · Incorrect: ${answeredCount - session.correct}")
                ToolText(if (question.arabicPrompt) "Choose the English meaning" else "Choose the Arabic Name")
                if (question.arabicPrompt) ArabicName(name.arabic, large = true) else ToolTitle(name.meaning)
                question.choices.forEach { id ->
                    val option = names.first { it.number == id }
                    val answered = session.answer != null
                    val correct = question.correct(id)
                    val label = if (question.arabicPrompt) option.meaning else option.arabic
                    val prefix = if (answered && correct) "✓ Correct · " else if (session.answer == id) "✕ Your answer · " else ""
                    LearningCard(prefix + label, arabic = !question.arabicPrompt, selected = answered && correct,
                        wrong = answered && session.answer == id && !correct, enabled = !answered) {
                        feedback(); update(session.copy(answer = id, correct = session.correct + if (correct) 1 else 0))
                    }
                }
                if (session.answer != null) {
                    ToolText(if (question.correct(session.answer)) "Correct" else "The correct answer is ${if (question.arabicPrompt) name.meaning else name.transliteration}", true)
                    PrimaryButton(if (session.index == session.ids.lastIndex) "See results" else "Next question", { update(session.copy(index = session.index + 1, answer = null)) }, Modifier.fillMaxWidth())
                }
            }
        } else {
            val roundIds = session.ids.chunked(5)[session.round]
            val arabicLeft = when (session.direction) { Direction.ArabicToEnglish -> true; Direction.EnglishToArabic -> false; Direction.Mixed -> session.round % 2 == 0 }
            var left by rememberSaveable(session.id, session.round) { mutableStateOf<Int?>(null) }
            var right by rememberSaveable(session.id, session.round) { mutableStateOf<Int?>(null) }
            var matchMessage by rememberSaveable(session.id, session.round) { mutableStateOf("") }
            var wrong by rememberSaveable(session.id, session.round) { mutableStateOf(false) }
            val roundDone = roundIds.all { it in session.matched }
            LaunchedEffect(left, right) {
                if (left != null && right != null) {
                    feedback()
                    if (left == right) {
                        val id = left!!
                        matchMessage = "Correct · ${names.first { it.number == id }.transliteration}"
                        update(session.copy(matched = session.matched + id))
                        progress = store.record("${session.id}:match:$id", listOf(id), true, 1)
                        left = null; right = null
                    } else {
                        if (!wrong) {
                            wrong = true; matchMessage = "Not a match. Try again."
                            update(session.copy(mistakes = session.mistakes + 1))
                        }
                        delay(750); left = null; right = null; wrong = false
                    }
                }
            }
            ToolCard("Round ${session.round + 1} of ${session.ids.chunked(5).size}") {
                ToolText("${session.matched.size} / ${session.ids.size} matched · ${session.mistakes} incorrect attempts")
                LinearProgressIndicator(progress = { session.matched.size.toFloat() / session.ids.size }, modifier = Modifier.fillMaxWidth(), color = Mihraya.colors.accent, trackColor = Mihraya.colors.accentSoft)
                ToolText("Tap one card in each column to match a Name and its meaning.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(true, false).forEach { isLeft ->
                        val arabic = if (isLeft) arabicLeft else !arabicLeft
                        val ordered = remember(session.id, session.round, isLeft) {
                            if (session.randomized) roundIds.shuffled(Random(session.id.hashCode() + session.round * 2 + if (isLeft) 0 else 1))
                            else if (isLeft) roundIds else roundIds.reversed()
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ToolText(if (arabic) "Arabic" else "Meaning", true)
                            ordered.forEach { id ->
                                val name = names.first { it.number == id }
                                val matched = id in session.matched
                                val selected = if (isLeft) left == id else right == id
                                LearningCard((if (matched) "✓ " else "") + if (arabic) name.arabic else name.meaning,
                                    arabic, selected || matched, wrong && selected, !matched && !wrong) {
                                    if (isLeft) left = id else right = id
                                }
                            }
                        }
                    }
                }
                if (matchMessage.isNotBlank()) ToolText(matchMessage, !wrong)
                if (roundDone) PrimaryButton("Next round", { left = null; right = null; update(session.copy(round = session.round + 1)) }, Modifier.fillMaxWidth())
            }
        }
    }
    if (leave) MihrayaDialog(title = "Leave this session?", message = "Completed matches remain saved. An unfinished quiz does not count as a completed quiz.", onDismiss = { leave = false }, confirmText = "Leave", onConfirm = { encoded = null; leave = false })
}
@Composable
private fun LearningCard(text: String, arabic: Boolean, selected: Boolean, wrong: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val fill by animateColorAsState(if (wrong) Mihraya.colors.danger.copy(alpha = 0.16f) else if (selected) Mihraya.colors.accentSoft else Mihraya.colors.glass, label = "learningFeedback")
    GlassCard(Modifier.fillMaxWidth().heightIn(min = 64.dp), fill = fill, onClick = if (enabled) onClick else null, contentPadding = PaddingValues(12.dp)) {
        if (arabic) ArabicName(text) else Text(text, style = Mihraya.type.body, color = Mihraya.colors.textPrimary)
    }
}
