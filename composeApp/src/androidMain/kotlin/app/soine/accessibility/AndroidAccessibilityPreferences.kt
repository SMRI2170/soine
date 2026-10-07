package app.soine.accessibility

import android.content.Context
import android.provider.Settings

class AndroidAccessibilityPreferences(
    context: Context,
) : AccessibilityPreferences {
    private val resolver = context.applicationContext.contentResolver

    override fun reduceMotionEnabled(): Boolean =
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
}
