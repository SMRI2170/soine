package app.soine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 Settings screen IA contract.
 *
 * #176 requires the settings screen to follow a clear
 * information architecture: every-night touch settings,
 * companion options, optional integrations, privacy and
 * data, and about. The textual / source checks here pin
 * the IA, the destructive-action hierarchy, the
 * design-system usage, and the permission state visual
 * organization.
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual source checks
 * here cover the contract.
 */
class SettingsScreenLayoutTest {

    @Test
    fun settingsScreenFollowsIaHierarchy() {
        val source = loadSettingsSource()
        // The five named groups, in order. The order matters:
        // every-night touch settings first, privacy/data in
        // the middle-bottom, about last.
        val expectedGroups = listOf(
            "睡眠について",
            "相棒について",
            "連携（任意）",
            "プライバシーとデータ",
            "このアプリについて",
        )
        var lastIndex = 0
        for (group in expectedGroups) {
            val idx = source.indexOf("\"$group\"")
            assertTrue(
                idx >= 0,
                "SettingsScreen must include the IA group \"$group\"",
            )
            assertTrue(
                idx >= lastIndex,
                "SettingsScreen IA group \"$group\" must appear after the previous group in the source order",
            )
            lastIndex = idx
        }
    }

    @Test
    fun settingsScreenUsesDesignSystem() {
        val source = loadSettingsSource()
        assertTrue(
            source.contains("SoinePanel"),
            "SettingsScreen must use SoinePanel for the IA group contents",
        )
        assertTrue(
            source.contains("SoineSectionHeader"),
            "SettingsScreen must use SoineSectionHeader for sub-section labels inside a group",
        )
    }

    @Test
    fun settingsScreenCompanionGroupIsQuietPlaceholder() {
        val mainSource = loadMainSettingsSource()
        // The Companion group is a quiet placeholder for the
        // future 3D renderer EPIC. It must say "coming
        // soon" / "next update" so the user does not expect
        // live options today.
        val companionIdx = mainSource.indexOf("相棒について")
        assertTrue(companionIdx >= 0)
        val companionSlice = mainSource.substring(
            companionIdx,
            mainSource.length,
        )
        assertTrue(
            "次のアップデート" in companionSlice,
            "SettingsScreen 相棒について group must say 'coming in the next update' so the user does not expect live options today",
        )
    }

    @Test
    fun settingsScreenDestructiveActionLivesBehindNavigation() {
        val mainSource = loadMainSettingsSource()
        // The destructive "delete all data" action must NOT
        // be a primary surface on the main settings screen.
        // It lives behind a navigation, so a mis-tap does
        // not erase the user's history.
        // The full destructive action lives on PrivacyDataScreen
        // (a separate composable in the same file) behind a
        // confirm dialog. The main settings screen body
        // must not contain the destructive text.
        assertTrue(
            !mainSource.contains("すべてのローカルデータを削除"),
            "SettingsScreen main must not render the destructive 'delete all data' button directly; it lives behind a navigation",
        )
        assertTrue(
            mainSource.contains("onPrivacyData"),
            "SettingsScreen must expose onPrivacyData so the destructive action can be reached behind a navigation",
        )
    }

    @Test
    fun settingsScreenPrivacyCopyIsShort() {
        val source = loadSettingsSource()
        // #176 also calls out privacy copy 短文化. The new
        // main-settings copy must be one short sentence so
        // the user can read it at a glance. The full
        // explanation lives behind the navigation.
        assertTrue(
            source.contains("Soineのデータは端末内に保存"),
            "SettingsScreen main プライバシーとデータ copy must be one short sentence that explains the data location",
        )
    }

    @Test
    fun settingsScreenAboutGroupContainsReplayOnboarding() {
        val source = loadSettingsSource()
        // The "replay onboarding" action moves into the
        // About group so the IA stays consistent.
        val aboutIdx = source.indexOf("このアプリについて")
        assertTrue(aboutIdx >= 0)
        val replayIdx = source.indexOf(
            "オンボーディングをもう一度見る",
        )
        val deleteAllIdx = source.indexOf("onDeleteAll")
        assertTrue(
            replayIdx > aboutIdx,
            "SettingsScreen replay-onboarding action must live inside the About group",
        )
        // The replay-onboarding must come AFTER the
        // privacy-and-data navigation so the order is
        // privacy → about.
        val privacyIdx = source.indexOf("プライバシーとデータ")
        assertTrue(
            replayIdx > privacyIdx,
            "SettingsScreen replay-onboarding action must come after the privacy-and-data group in source order",
        )
    }

    @Test
    fun settingsScreenPermissionStateHasUnavailablePolish() {
        val source = loadSettingsSource()
        // The "unavailable" microphone state must surface
        // as an inline note, not a broken switch. The
        // switch is disabled, and the user sees "この端末で
        // は利用できません" right below.
        assertTrue(
            source.contains("MicrophonePermissionState.UNAVAILABLE"),
            "SettingsScreen must special-case the UNAVAILABLE microphone state",
        )
        assertTrue(
            source.contains("この端末では利用できません"),
            "SettingsScreen must show an inline 'this device is not supported' note when microphone is unavailable",
        )
    }

    @Test
    fun settingsScreenMergesDescendantSemantics() {
        val source = loadSettingsSource()
        // The replay-onboarding action and the sound
        // analysis switch expose an explicit content
        // description so TalkBack reads the action as a
        // single concise item, not as a stream of glyph +
        // label.
        assertTrue(
            source.contains("SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION"),
            "SettingsScreen must wire the replay-onboarding action to AccessibilityPolicy.SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun settingsScreenDoesNotShowLevelOrStreak() {
        // #175 already locked the relationship-stage chrome
        // to a "no XP / no level / no streak" rule. The
        // settings screen must not regress and show
        // relationship progress as a number, an XP bar, or
        // a level chip.
        val source = loadSettingsSource()
        val nonCommentSource = source
            .lineSequence()
            .filter { line ->
                val trimmed = line.trimStart()
                !(trimmed.startsWith("*") || trimmed.startsWith("//"))
            }
            .joinToString("\n")
        assertTrue(
            !nonCommentSource.contains("Level") && !nonCommentSource.contains("レベル"),
            "SettingsScreen must not show a relationship level",
        )
        assertTrue(
            !nonCommentSource.contains("Streak") && !nonCommentSource.contains("ストリーク"),
            "SettingsScreen must not show a streak counter",
        )
    }

    private fun loadSettingsSource(): String = loadFile(
        "composeApp/src/commonMain/kotlin/app/soine/SettingsScreen.kt",
    )

    /**
     * Returns only the body of the main `SettingsScreen`
     * composable, not the `PrivacyDataScreen` composable
     * that follows it in the same file. The two screens
     * live together so the destructiveness of
     * `PrivacyDataScreen` does not bleed into the main
     * settings audit.
     */
    private fun loadMainSettingsSource(): String {
        val full = loadSettingsSource()
        val privacyDataIdx = full.indexOf("fun PrivacyDataScreen(")
        return if (privacyDataIdx > 0) full.substring(0, privacyDataIdx) else full
    }

    private fun loadFile(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("composeApp/"),
            "../${relativePath.removePrefix("composeApp/")}",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error("Source file not found in any of the candidate paths: $relativePath")
    }
}
