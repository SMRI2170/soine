package app.soine.analytics

import android.content.Context

class AndroidAnalyticsPreferencesStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : AnalyticsPreferencesStore {

    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): AnalyticsPreferences = AnalyticsPreferences(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        installId = preferences.getString(KEY_INSTALL_ID, null),
    )

    override fun write(preferences: AnalyticsPreferences) {
        this.preferences.edit()
            .putBoolean(KEY_ENABLED, preferences.enabled)
            .apply {
                val installId = preferences.installId
                if (installId == null) {
                    remove(KEY_INSTALL_ID)
                } else {
                    putString(KEY_INSTALL_ID, installId)
                }
            }
            .apply()
    }

    companion object {
        private const val DEFAULT_PREFERENCES_NAME = "soine_analytics"
        private const val KEY_ENABLED = "analytics_enabled"
        private const val KEY_INSTALL_ID = "analytics_install_id"
    }
}