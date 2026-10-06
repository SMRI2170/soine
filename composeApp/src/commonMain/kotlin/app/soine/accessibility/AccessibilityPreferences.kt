package app.soine.accessibility

fun interface AccessibilityPreferences {
    fun reduceMotionEnabled(): Boolean
}

object DefaultAccessibilityPreferences : AccessibilityPreferences {
    override fun reduceMotionEnabled(): Boolean = false
}

object AccessibilityPolicy {
    const val MIN_TOUCH_TARGET_DP: Int = 48
}
