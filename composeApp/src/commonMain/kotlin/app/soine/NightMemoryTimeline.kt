package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.night.InitialNightEventCatalog
import app.soine.night.NightEvent
import app.soine.night.NightEventAnimationIntent
import app.soine.night.NightEventType

data class NightMemoryEntry(
    val eventId: String,
    val occurredAtEpochMillis: Long,
    val timeLabel: String,
    val line: String,
    val artKey: String,
    val glyph: String,
)

fun buildNightMemoryEntries(
    events: List<NightEvent>,
    maxEntries: Int = 3,
): List<NightMemoryEntry> {
    require(maxEntries in 0..3) { "Night-memory timeline supports at most three entries." }

    return events
        .sortedWith(compareBy<NightEvent> { it.occurredAtEpochMillis }.thenBy { it.id })
        .mapNotNull { event ->
            val definition = InitialNightEventCatalog.definitionFor(event) ?: return@mapNotNull null
            NightMemoryEntry(
                eventId = event.id,
                occurredAtEpochMillis = event.occurredAtEpochMillis,
                timeLabel = formatJapaneseNightEventTime(event.occurredAtEpochMillis),
                line = definition.morningLine,
                artKey = definition.animationIntent.toArtKey(),
                glyph = event.type.toTimelineGlyph(),
            )
        }
        .take(maxEntries)
}

fun formatJapaneseNightEventTime(epochMillis: Long): String {
    require(epochMillis >= 0L) { "NightEvent timestamp must not be negative." }
    val localMillis = epochMillis + JAPAN_UTC_OFFSET_MILLIS
    val millisOfDay = localMillis % MILLIS_PER_DAY
    val hours = millisOfDay / MILLIS_PER_HOUR
    val minutes = (millisOfDay % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
    return hours.toString().padStart(2, '0') + ":" +
        minutes.toString().padStart(2, '0')
}

private fun NightEventAnimationIntent.toArtKey(): String =
    "night_event_" + name.lowercase()

private fun NightEventType.toTimelineGlyph(): String = when (this) {
    NightEventType.TURN_OVER -> "↻"
    NightEventType.EAR_TWITCH -> "︿"
    NightEventType.MOVE_CLOSER -> "→"
    NightEventType.CURL_UP -> "○"
    NightEventType.BRIEF_WAKE -> "◐"
    NightEventType.FUNNY_POSE -> "△"
    NightEventType.DREAM -> "◇"
    NightEventType.SOUND_REACTION -> "≋"
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
private const val JAPAN_UTC_OFFSET_MILLIS = 9L * MILLIS_PER_HOUR

@Composable
fun NightMemoryTimeline(entries: List<NightMemoryEntry>) {
    if (entries.isEmpty()) return

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "昨夜の記憶",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            entries.take(3).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = entry.timeLabel + "。" + entry.line
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(entry.glyph, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(entry.line, style = MaterialTheme.typography.bodyMedium)
                        Text(entry.timeLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
