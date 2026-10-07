package app.soine.analytics

/**
 * User-facing analytics preferences.
 *
 * The default `enabled = false` mirrors the "local-first, no implicit
 * tracking" stance in [docs/privacy.md]. Soine ships with analytics
 * opted out until the user explicitly enables it (or a vendor
 * integration makes that decision and reflects it in the privacy
 * store listing).
 *
 * `installId` is an anonymous per-install random identifier, generated
 * by [AnalyticsIdentity.ensureInstallId] and never derived from
 * hardware IDs.
 */
data class AnalyticsPreferences(
    val enabled: Boolean = false,
    val installId: String? = null,
)

fun interface AnalyticsPreferencesStore {
    fun read(): AnalyticsPreferences
    fun write(preferences: AnalyticsPreferences)
}