package app.soine.analytics

import platform.Foundation.NSUserDefaults

class IosAnalyticsPreferencesStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AnalyticsPreferencesStore {
    override fun read(): AnalyticsPreferences = AnalyticsPreferences(
        enabled = defaults.boolForKey(KEY_ENABLED),
        installId = defaults.stringForKey(KEY_INSTALL_ID),
    )

    override fun write(preferences: AnalyticsPreferences) {
        defaults.setBool(preferences.enabled, forKey = KEY_ENABLED)
        val installId = preferences.installId
        if (installId == null) {
            defaults.removeObjectForKey(KEY_INSTALL_ID)
        } else {
            defaults.setObject(installId, forKey = KEY_INSTALL_ID)
        }
    }

    companion object {
        private const val KEY_ENABLED = "app.soine.analytics.enabled"
        private const val KEY_INSTALL_ID = "app.soine.analytics.install_id"
    }
}