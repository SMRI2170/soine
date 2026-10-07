package app.soine.accessibility

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

class IosAccessibilityPreferences : AccessibilityPreferences {
    override fun reduceMotionEnabled(): Boolean =
        UIAccessibilityIsReduceMotionEnabled()
}
