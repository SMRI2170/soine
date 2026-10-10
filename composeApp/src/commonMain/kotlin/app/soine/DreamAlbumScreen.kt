package app.soine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.design.SoineColors
import app.soine.design.SoinePanel
import app.soine.design.SoineQuietButton
import app.soine.design.SoineSectionHeader
import app.soine.design.SoineTokens
import app.soine.dream.DreamAlbumArtTile

data class DreamAlbumEntry(
    val id: String,
    val discovered: Boolean,
    val title: String,
    val shortLine: String,
    val rarityLabel: String?,
    val discoveredAtEpochMillis: Long?,
    val discoveredDateLabel: String?,
    val artKey: String?,
)

fun buildDreamAlbumEntries(
    definitions: List<app.soine.dream.DreamDefinition>,
    discoveries: List<app.soine.dream.DreamDiscovery>,
    timeZone: app.soine.time.LocalTimeZone = app.soine.time.LocalTimeZones.current,
    formatter: app.soine.time.format.DisplayFormatter = app.soine.time.format.DisplayFormatters.current,
): List<DreamAlbumEntry> {
    val earliestDiscoveryByDreamId = discoveries
        .groupBy { it.dreamId }
        .mapValues { (_, values) -> values.minBy { it.discoveredAtEpochMillis } }

    return definitions
        .map { definition ->
            val discovery = earliestDiscoveryByDreamId[definition.id]
            if (discovery == null) {
                DreamAlbumEntry(
                    id = definition.id,
                    discovered = false,
                    title = "？？？",
                    shortLine = "まだ見つけていない夢",
                    rarityLabel = null,
                    discoveredAtEpochMillis = null,
                    discoveredDateLabel = null,
                    artKey = definition.artKey,
                )
            } else {
                DreamAlbumEntry(
                    id = definition.id,
                    discovered = true,
                    title = definition.title,
                    shortLine = definition.shortLine,
                    rarityLabel = definition.rarity.toGentleLabel(),
                    discoveredAtEpochMillis = discovery.discoveredAtEpochMillis,
                    discoveredDateLabel = formatter.formatDiscoveryDate(
                        discovery.discoveredAtEpochMillis,
                        timeZone,
                    ),
                    artKey = definition.artKey,
                )
            }
        }
        .sortedWith(
            compareByDescending<DreamAlbumEntry> { it.discovered }
                .thenByDescending { it.discoveredAtEpochMillis ?: Long.MIN_VALUE }
                .thenBy { it.id },
        )
}

private fun app.soine.night.RarityBand.toGentleLabel(): String = when (this) {
    app.soine.night.RarityBand.COMMON -> "よく見る夢"
    app.soine.night.RarityBand.UNCOMMON -> "ときどき見る夢"
    app.soine.night.RarityBand.RARE -> "めずらしい夢"
}

/**
 * Format a discovery's wall-clock date in the current locale style.
 *
 * V1 calls into [app.soine.time.format.JapaneseDisplayFormatter];
 * future locales swap [app.soine.time.format.DisplayFormatters.current]
 * to add a new style without touching call sites.
 */
fun formatJapaneseDiscoveryDate(
    epochMillis: Long,
    timeZone: app.soine.time.LocalTimeZone = app.soine.time.LocalTimeZones.current,
): String =
    app.soine.time.format.DisplayFormatters.current.formatDiscoveryDate(epochMillis, timeZone)

/**
 * The Dream Album — a quiet visual collection.
 *
 * The album uses a 2-column [LazyVerticalGrid] so the visual
 * identity of each dream (the [DreamAlbumArtTile]) reads as
 * a constellation / scrapbook rather than a flat list. The
 * hero "夢" / "？" glyph that the previous V0 used is gone;
 * every cell is the dream's own gradient + motif shape. The
 * locked state is rendered as a silhouette of the same shape
 * (not a "？") so the empty album still has a world-feel.
 *
 * The detail surface is a [ModalBottomSheet] (an immersive
 * sheet / page), not a dialog. The sheet surfaces a larger
 * version of the art tile, the dream's title, short line,
 * rarity, and discovery date so a tap on a dream feels like
 * opening a page in a scrapbook.
 *
 * No progress bar, no streak counter, no login bonus. The
 * header is a single line "X / N 見つけた" so the user can
 * see how many dreams are left without feeling pressured to
 * collect them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamAlbumScreen(
    entries: List<DreamAlbumEntry>,
    onBack: () -> Unit,
) {
    var selectedDreamId by remember(entries) { mutableStateOf<String?>(null) }
    val discoveredCount = entries.count { it.discovered }
    val selectedEntry = entries.firstOrNull { it.id == selectedDreamId && it.discovered }
    val sheetState = rememberBottomSheetState(initialValue = androidx.compose.material3.SheetValue.Hidden)

    Column(
        Modifier.fillMaxSize().padding(horizontal = SoineTokens.SpacingLg),
    ) {
        Spacer(Modifier.height(SoineTokens.SpacingMd))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
        ) {
            SoineQuietButton(
                text = "もどる",
                onClick = onBack,
                contentDescription = "もどる",
            )
            Spacer(Modifier.size(SoineTokens.SpacingXs))
            Column {
                Text(
                    "夢のアルバム",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SoineColors.cream,
                )
                Text(
                    text = discoveredCount.toString() + " / " + entries.size + " 見つけた",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoineColors.hush,
                )
            }
        }

        Spacer(Modifier.height(SoineTokens.SpacingMd))

        if (discoveredCount == 0) {
            SoinePanel {
                Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
                    Text(
                        "まだ夢は見つかっていません",
                        style = MaterialTheme.typography.titleMedium,
                        color = SoineColors.cream,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "眠った朝に、ときどき新しい夢を見つけます。急いで集めなくても大丈夫です。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoineColors.hush,
                    )
                }
            }
            Spacer(Modifier.height(SoineTokens.SpacingMd))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = SoineTokens.SpacingXs),
            verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingMd),
            horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingMd),
        ) {
            items(entries, key = { it.id }) { entry ->
                DreamAlbumCell(
                    entry = entry,
                    onClick = { if (entry.discovered) selectedDreamId = entry.id },
                )
            }
        }
    }

    if (selectedEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDreamId = null },
            sheetState = sheetState,
            containerColor = SoineColors.dusk,
        ) {
            DreamDetailSheet(
                entry = selectedEntry,
                onClose = { selectedDreamId = null },
            )
        }
    }
}

@Composable
private fun DreamAlbumCell(
    entry: DreamAlbumEntry,
    onClick: () -> Unit,
) {
    val accessibilityLabel = if (entry.discovered) {
        buildString {
            append(entry.title)
            entry.rarityLabel?.let { append("。").append(it) }
            entry.discoveredDateLabel?.let { append("。見つけた日 ").append(it) }
        }
    } else {
        "未発見の夢"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.discovered, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            },
        verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
    ) {
        DreamAlbumArtTile(
            artKey = entry.artKey,
            discovered = entry.discovered,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            accessibilityLabel = accessibilityLabel,
        )
        if (entry.discovered) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = SoineColors.cream,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                "？？？",
                style = MaterialTheme.typography.titleSmall,
                color = SoineColors.hush,
            )
        }
    }
}

@Composable
private fun DreamDetailSheet(
    entry: DreamAlbumEntry,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SoineTokens.SpacingLg, vertical = SoineTokens.SpacingMd),
        verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingMd),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
        ) {
            DreamAlbumArtTile(
                artKey = entry.artKey,
                discovered = entry.discovered,
                modifier = Modifier.fillMaxSize(),
                accessibilityLabel = entry.title + "の表紙",
            )
        }
        Text(
            entry.title,
            style = MaterialTheme.typography.headlineSmall,
            color = SoineColors.cream,
            fontWeight = FontWeight.SemiBold,
        )
        SoineSectionHeader("記憶")
        Text(
            entry.shortLine,
            style = MaterialTheme.typography.bodyLarge,
            color = SoineColors.cream,
        )
        entry.rarityLabel?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = SoineColors.hush,
            )
        }
        entry.discoveredDateLabel?.let {
            Text(
                "見つけた日 " + it,
                style = MaterialTheme.typography.bodyMedium,
                color = SoineColors.hush,
            )
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .heightIn(min = SoineTokens.QuietCtaHeight)
                .align(Alignment.End),
        ) {
            Text("閉じる", color = SoineColors.cream)
        }
    }
}
