package app.soine.time.format

import app.soine.time.LocalTimeZone
import app.soine.time.LocalTimeZones

/**
 * Display-time formatter owned by commonMain.
 *
 * The formatter is the single boundary between persisted absolute
 * timestamps and the localized strings the user reads. It pairs with
 * [app.soine.time.LocalTimeZone] so a render call becomes:
 *
 *   DisplayFormatters.current.formatDiscoveryDate(
 *       epochMillis,
 *       timeZone = LocalTimeZones.current,
 *   )
 *
 * V1 ships with [JapaneseDisplayFormatter] because Soine V1 launches
 * in Japan. Future locales add a new [DisplayFormatter] and swap
 * [DisplayFormatters.current] at app start; call sites do not need
 * to change.
 *
 * Implementations MUST be pure: no state, no I/O. The formatters may
 * be invoked from any thread the UI dispatcher lands on, and they
 * must not capture `LocalTimeZones.current` at construction time.
 *
 * The contract intentionally does not own the timezone argument; the
 * caller is responsible for picking [LocalTimeZones.current] (or any
 * test override). The formatter only formats what it is given.
 */
interface DisplayFormatter {
    /**
     * Format the discovery date associated with an absolute instant.
     * The date is computed in [timeZone]; the string style is the
     * locale-specific shape (e.g. "2026年10月8日" vs "Oct 8, 2026").
     */
    fun formatDiscoveryDate(epochMillis: Long, timeZone: LocalTimeZone): String

    /**
     * Format the wall-clock time of night for display in
     * [timeZone]. V1 uses a 24-hour HH:mm shape; a future locale may
     * opt into 12-hour or with-meridiem style.
     */
    fun formatNightEventTime(epochMillis: Long, timeZone: LocalTimeZone): String
}

/**
 * Mutable binding that the app wires at startup. Renderer code reads
 * [current] at call time so a locale swap is reflected immediately
 * without any cache invalidation.
 *
 * Tests swap this binding to exercise non-default locales.
 */
object DisplayFormatters {
    // KMP commonMain does not have @Volatile. The renderer is single-
    // threaded (Compose UI dispatcher) and the binding is only mutated
    // at app start. A plain var is sufficient.
    var current: DisplayFormatter = JapaneseDisplayFormatter
}
