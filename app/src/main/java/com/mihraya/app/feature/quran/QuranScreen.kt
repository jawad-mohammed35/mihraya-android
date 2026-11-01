package com.mihraya.app.feature.quran

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.CollapsingHeaderLayout
import com.mihraya.app.core.designsystem.components.EmptyState
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.PagerTabs
import com.mihraya.app.core.designsystem.components.ScreenHeader
import com.mihraya.app.core.designsystem.components.SearchField
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.StarBadge
import com.mihraya.app.core.designsystem.components.fadeAbove
import com.mihraya.app.core.designsystem.components.rememberCollapsingHeaderState
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.library.Bookmark
import com.mihraya.app.data.library.ReadingPosition
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.SearchHit
import com.mihraya.app.data.quran.Surah
import com.mihraya.app.data.quran.arabicDigits
import com.mihraya.app.feature.home.ProgressLine
import com.mihraya.app.ui.LocalBottomInset
import com.mihraya.app.ui.mihrayaViewModel
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(scrollToTop: Int = 0, onOpenReader: (surah: Int, ayah: Int) -> Unit) {
    val vm = mihrayaViewModel { QuranViewModel(it) }
    val query by vm.query.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val verseSearch by vm.verseSearch.collectAsStateWithLifecycle()
    val searching = query.isNotBlank()
    val item = Modifier.widthIn(max = 680.dp).fillMaxWidth()
    val header = rememberCollapsingHeaderState()
    val pager = rememberPagerState { 3 }
    val surahList = rememberLazyListState()
    val juzList = rememberLazyListState()
    val bookmarkList = rememberLazyListState()
    val searchList = rememberLazyListState()
    val lists = listOf(surahList, juzList, bookmarkList)
    val visibleList = { if (searching) searchList else lists[pager.currentPage] }
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            launch { header.expand() }
            visibleList().animateScrollToItem(0)
        }
    }
    val listPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = LocalBottomInset.current + 16.dp)

    CollapsingHeaderLayout(
        state = header,
        canScrollForward = { visibleList().canScrollForward },
        header = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ScreenHeader("Quran", modifier = item)
                SearchField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    placeholder = "Sūrah, verse 2:255, or a word",
                    modifier = item.padding(top = 10.dp, bottom = 8.dp),
                )
                if (!searching) {
                    library.lastRead?.let { position ->
                        ContinueReadingCard(
                            position,
                            onClick = { onOpenReader(position.surah, position.ayah) },
                            modifier = item.padding(top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        },
        pinned = {
            if (!searching) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    PagerTabs(listOf("Sūrahs", "Juzʾ", "Bookmarks"), pager, item)
                }
            }
        },
        body = {
            if (searching) {
                LazyColumn(
                    state = searchList,
                    modifier = Modifier.fillMaxSize().fadeAbove(top = 8.dp, fade = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = listPadding,
                ) {
                    searchResults(query, vm, verseSearch, onOpenReader, item)
                }
            } else {
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxSize().fadeAbove(top = 8.dp, fade = 8.dp),
                ) { page ->
                    LazyColumn(
                        state = lists[page],
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = listPadding,
                    ) {
                        when (page) {
                            0 -> items(QuranMeta.surahs, key = { "s${it.number}" }) { surah ->
                                SurahRow(
                                    surah = surah,
                                    highlighted = library.lastRead?.surah == surah.number,
                                    onClick = { onOpenReader(surah.number, 1) },
                                    modifier = item,
                                )
                            }
                            1 -> items((1..QuranMeta.JUZ_COUNT).toList(), key = { "j$it" }) { juz ->
                                JuzRow(juz, onClick = { QuranMeta.juzStart(juz).let { onOpenReader(it.surah, it.ayah) } }, modifier = item)
                            }
                            else -> if (library.bookmarks.isEmpty()) {
                                item(key = "noBookmarks") {
                                    EmptyState(
                                        icon = Icons.Rounded.BookmarkBorder,
                                        title = "No bookmarks yet",
                                        message = "Tap the bookmark icon beside any verse to save it here.",
                                        modifier = item,
                                    )
                                }
                            } else {
                                items(library.bookmarks, key = { "b${it.surah}:${it.ayah}" }) { bookmark ->
                                    BookmarkRow(
                                        bookmark = bookmark,
                                        onClick = { onOpenReader(bookmark.surah, bookmark.ayah) },
                                        onRemove = { vm.removeBookmark(bookmark.surah, bookmark.ayah) },
                                        modifier = item,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun LazyListScope.searchResults(
    query: String,
    vm: QuranViewModel,
    verseSearch: VerseSearch,
    onOpenReader: (Int, Int) -> Unit,
    item: Modifier,
) {
    val ref = QuranMeta.parseRef(query)
    val surahs = vm.surahMatches(query)
    if (ref != null && query.contains(Regex("[:\\s.]"))) {
        item(key = "goto") {
            GlassCard(item.padding(bottom = 12.dp), onClick = { onOpenReader(ref.surah, ref.ayah) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("GO TO VERSE", style = Mihraya.type.overline, color = Mihraya.colors.accent)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${QuranMeta.surah(ref.surah).name} · ${ref.key}",
                            style = Mihraya.type.title,
                            color = Mihraya.colors.textPrimary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Mihraya.colors.accent)
                }
            }
        }
    }
    if (surahs.isNotEmpty()) {
        item(key = "surahLabel") { SectionLabel("Sūrahs", item.padding(vertical = 8.dp)) }
        items(surahs, key = { "ms${it.number}" }) { surah ->
            SurahRow(surah, highlighted = false, onClick = { onOpenReader(surah.number, 1) }, modifier = item)
        }
    }
    when (verseSearch) {
        VerseSearch.Idle -> Unit
        VerseSearch.Searching -> item(key = "searching") {
            Box(item.padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mihraya.colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        is VerseSearch.Results -> {
            item(key = "versesLabel") {
                SectionLabel(
                    if (verseSearch.hits.size >= 60) "Verses · first 60" else "Verses · ${verseSearch.hits.size}",
                    item.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            if (verseSearch.hits.isEmpty() && surahs.isEmpty()) {
                item(key = "noResults") {
                    EmptyState(Icons.Rounded.Search, "No matches", "Try another word, a sūrah name, or a reference like 2:255.", item)
                }
            }
            items(verseSearch.hits, key = { "h${it.ref.key}" }) { hit ->
                SearchHitRow(hit, onClick = { onOpenReader(hit.ref.surah, hit.ref.ayah) }, modifier = item)
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(position: ReadingPosition, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val surah = QuranMeta.surah(position.surah)
    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CONTINUE READING", style = Mihraya.type.overline, color = colors.accent)
                Spacer(Modifier.height(6.dp))
                Text(surah.name, style = Mihraya.type.title, color = colors.textPrimary)
                Text(
                    "Verse ${position.ayah} of ${surah.verseCount} · Juzʾ ${QuranMeta.juzOf(position.surah, position.ayah)}",
                    style = Mihraya.type.bodySmall,
                    color = colors.textTertiary,
                )
            }
            Text(
                surah.arabicName,
                style = Mihraya.type.title.copy(fontFamily = ArabicFonts.Amiri, fontSize = 28.sp, lineHeight = 40.sp, textDirection = TextDirection.Rtl),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(16.dp))
        ProgressLine(position.ayah.toFloat() / surah.verseCount)
    }
}

@Composable
fun SurahRow(surah: Surah, highlighted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 70.dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarBadge(surah.number, highlighted = highlighted)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(surah.name, style = Mihraya.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${surah.meaning} · ${surah.verseCount} verses",
                    style = Mihraya.type.caption,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    surah.arabicName,
                    style = Mihraya.type.title.copy(
                        fontFamily = ArabicFonts.Amiri,
                        fontWeight = FontWeight.Normal,
                        fontSize = 21.sp,
                        lineHeight = 30.sp,
                        textDirection = TextDirection.Rtl,
                    ),
                    color = if (highlighted) colors.accent else colors.textSecondary,
                )
                Text(surah.revelation.label, style = Mihraya.type.caption, color = colors.textTertiary)
            }
        }
        HairlineDivider(startIndent = 58.dp)
    }
}

@Composable
private fun JuzRow(juz: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val start = QuranMeta.juzStart(juz)
    val surah = QuranMeta.surah(start.surah)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 68.dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarBadge(juz)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Juzʾ $juz", style = Mihraya.type.titleSmall, color = colors.textPrimary)
                Text(
                    "Begins ${surah.name} ${start.key} · ${QuranMeta.juzVerseCount(juz)} verses",
                    style = Mihraya.type.caption,
                    color = colors.textTertiary,
                )
            }
            Text(
                "الجزء ${juz.arabicDigits()}",
                style = Mihraya.type.title.copy(fontFamily = ArabicFonts.Amiri, fontWeight = FontWeight.Normal, fontSize = 19.sp, textDirection = TextDirection.Rtl),
                color = colors.textSecondary,
            )
        }
        HairlineDivider(startIndent = 58.dp)
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val surah = QuranMeta.surah(bookmark.surah)
    val saved = remember(bookmark.createdAt) {
        DateUtils.getRelativeTimeSpanString(bookmark.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bookmark, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${surah.name} · ${bookmark.surah}:${bookmark.ayah}", style = Mihraya.type.titleSmall, color = colors.textPrimary)
                Text("Saved $saved", style = Mihraya.type.caption, color = colors.textTertiary)
            }
            CircleIconButton(Icons.Rounded.Close, "Remove bookmark", onRemove, size = 36.dp, iconSize = 16.dp, filled = false, tint = colors.textTertiary)
        }
        HairlineDivider(startIndent = 58.dp)
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val arabic = hit.text.any { it in '\u0600'..'\u06FF' }
    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
        ) {
            Text(
                "${QuranMeta.surah(hit.ref.surah).name} · ${hit.ref.key}",
                style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                hit.text,
                style = if (arabic) {
                    Mihraya.type.body.copy(fontFamily = ArabicFonts.AmiriQuran, fontSize = 20.sp, lineHeight = 38.sp, textDirection = TextDirection.Rtl)
                } else {
                    Mihraya.type.bodySmall
                },
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HairlineDivider()
    }
}
