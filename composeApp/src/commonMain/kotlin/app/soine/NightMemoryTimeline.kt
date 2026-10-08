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
import app.soine.time.LocalTimeZone
import app.soine.time.LocalTimeZones
import app.soine.time.format.DisplayFormatters

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
    timeZone: LocalTimeZone = LocalTimeZones.current,
    formatter: app.soine.time.format.DisplayFormatter = DisplayFormatters.current,
): List<NightMemoryEntry> {
    require(maxEntries in 0..3) { "Night-memory timeline supports at most three entries." }

    return events
        .sortedWith(compareBy<NightEvent> { it.occurredAtEpochMillis }.thenBy { it.id })
        .mapNotNull { event ->
            val definition = InitialNightEventCatalog.definitionFor(event) ?: return@mapNotNull null
            NightMemoryEntry(
                eventId = event.id,
                occurredAtEpochMillis = event.occurredAtEpochMillis,
                timeLabel = formatter.formatNightEventTime(event.occurredAtEpochMillis, timeZone),
                line = definition.morningLine,
                artKey = definition.animationIntent.toArtKey(),
                glyph = event.type.toTimelineGlyph(),
            )
        }
        .take(maxEntries)
}

/**
 * Format a night event's wall-clock time using the current
 * [DisplayFormatters.current] and [LocalTimeZones.current] bindings.
 *
 * Kept as a top-level helper for tests and Compose previews that want
 * a one-line call without passing a formatter. The actual formatting
 * lives in [app.soine.time.format.JapaneseDisplayFormatter].
 */
fun formatNightEventTime(
    epochMillis: Long,
    timeZone: LocalTimeZone = LocalTimeZones.current,
): String =
    DisplayFormatters.current.formatNightEventTime(epochMillis, timeZone)

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
