package app.soine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.soine.dream.DreamDefinition
import app.soine.dream.DreamDiscovery
import app.soine.night.RarityBand

data class DreamAlbumEntry(
    val id: String,
    val discovered: Boolean,
    val title: String,
    val shortLine: String,
    val rarityLabel: String?,
    val discoveredAtEpochMillis: Long?,
    val discoveredDateLabel: String?,
)

fun buildDreamAlbumEntries(
    definitions: List<DreamDefinition>,
    discoveries: List<DreamDiscovery>,
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
                )
            }
        }
        .sortedWith(
            compareByDescending<DreamAlbumEntry> { it.discovered }
                .thenByDescending { it.discoveredAtEpochMillis ?: Long.MIN_VALUE }
                .thenBy { it.id },
        )
}

private fun RarityBand.toGentleLabel(): String = when (this) {
    RarityBand.COMMON -> "よく見る夢"
    RarityBand.UNCOMMON -> "ときどき見る夢"
    RarityBand.RARE -> "めずらしい夢"
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

@Composable
fun DreamAlbumScreen(
    entries: List<DreamAlbumEntry>,
    onBack: () -> Unit,
) {
    var selectedDreamId by remember(entries) { mutableStateOf<String?>(null) }
    val discoveredCount = entries.count { it.discovered }
    val selectedEntry = entries.firstOrNull { it.id == selectedDreamId && it.discovered }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("戻る") }
            Column {
                Text(
                    "夢のアルバム",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    discoveredCount.toString() + " / " + entries.size + " 見つけた",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (discoveredCount == 0) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("まだ夢は見つかっていません", fontWeight = FontWeight.Medium)
                    Text(
                        "眠った朝に、ときどき新しい夢を見つけます。急いで集めなくても大丈夫です。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                DreamAlbumCard(
                    entry = entry,
                    onClick = { selectedDreamId = entry.id },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    selectedEntry?.let { entry ->
        DreamDetailDialog(
            entry = entry,
            onDismiss = { selectedDreamId = null },
        )
    }
}

@Composable
private fun DreamAlbumCard(
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .clickable(enabled = entry.discovered, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = if (entry.discovered) 2.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (entry.discovered) "夢" else "？",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(entry.title, fontWeight = FontWeight.SemiBold)
                Text(entry.shortLine, style = MaterialTheme.typography.bodyMedium)
                if (entry.discovered) {
                    entry.rarityLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }
                    entry.discoveredDateLabel?.let {
                        Text("見つけた日 " + it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamDetailDialog(
    entry: DreamAlbumEntry,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entry.shortLine)
                entry.rarityLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge)
                }
                entry.discoveredDateLabel?.let {
                    Text("見つけた日 " + it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}
