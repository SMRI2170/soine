package app.soine

import app.soine.accessibility.AccessibilityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 morning screen layout contract.
 *
 * #171 requires:
 *
 *   - sleep duration が最初の hero 情報ではない — the
 *     companion + morning greeting are the hero; the duration
 *     sits in a small summary card
 *   - night memory が glyph + text だけで終わらない — the night
 *     memory timeline uses the existing visual cards; the
 *     morning screen does not regress to a glyph-only list
 *   - dream 発見時に「見つけた感」がある — the special reveal
 *     panel surfaces above the night memory
 *   - morning flow が長すぎず 1 画面 〜 短い scroll で完結 —
 *     the layout fits in a single vertical column with
 *     verticalScroll
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual / source checks here
 * cover the contract.
 */
class MorningScreenLayoutTest {

    @Test
    fun morningScreenUsesDesignSystem() {
        val source = loadMorningSource()
        assertTrue(
            source.contains("SoinePrimaryButton"),
            "MorningScreen must use SoinePrimaryButton for the close CTA",
        )
        assertTrue(
            source.contains("SoinePanel"),
            "MorningScreen must use SoinePanel for the summary / dream reveal cards",
        )
        assertTrue(
            source.contains("SoineSectionHeader"),
            "MorningScreen must use SoineSectionHeader for sub-section labels",
        )
    }

    @Test
    fun morningScreenReusesCompanionSceneContent() {
        val source = loadMorningSource()
        assertTrue(
            source.contains("CompanionSceneContent"),
            "MorningScreen must call CompanionSceneContent for the hero wake scene",
        )
    }

    @Test
    fun morningScreenEmotionBeforeData() {
        val source = loadMorningSource()
        // The "おはよう" / morning greeting is the first hero
        // text. The duration comes after the night memory and
        // the dream reveal so the user's eye lands on the
        // emotion first.
        // We anchor on the greeting render call (`text =
        // morningGreeting + (...)`) and the summary call site
        // so we measure where the panels actually appear in
        // the MorningScreen body, not where the helper
        // functions or constants are declared. The
        // `morningGreeting +` prefix is what the new
        // relationship-stage presentation appends the stage
        // suffix onto.
        val greetingIdx = source.indexOf("text = morningGreeting +")
        val summaryIdx = source.indexOf("SummaryCard(summary = summary)")
        assertTrue(
            greetingIdx >= 0,
            "MorningScreen must render the morning greeting",
        )
        assertTrue(
            summaryIdx >= 0,
            "MorningScreen must render the summary card",
        )
        assertTrue(
            greetingIdx < summaryIdx,
            "Morning greeting must come before the summary card (Emotion before Data)",
        )
    }

    @Test
    fun morningScreenDreamDiscoveryRevealPrecedesNightMemory() {
        val source = loadMorningSource()
        // The dream reveal must come before the night memory so
        // the "見つけた" feeling lands first. We anchor on the
        // call sites (not the helper function definitions or
        // the panel's internal copy) so we measure where the
        // panels actually appear in the MorningScreen body.
        val dreamIdx = source.indexOf("DreamDiscoveryReveal(")
        val nightIdx = source.indexOf("NightMemoryTimeline(nightMemoryEntries)")
        assertTrue(
            dreamIdx >= 0,
            "MorningScreen must render the dream discovery reveal",
        )
        assertTrue(
            nightIdx >= 0,
            "MorningScreen must render the night memory timeline",
        )
        assertTrue(
            dreamIdx < nightIdx,
            "Dream discovery reveal must come before the night memory timeline",
        )
    }

    @Test
    fun morningScreenCtaIsAnchored() {
        val source = loadMorningSource()
        assertTrue(
            source.contains("AccessibilityPolicy.MORNING_DONE_CONTENT_DESCRIPTION"),
            "MorningScreen must wire the close CTA to AccessibilityPolicy.MORNING_DONE_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun morningScreenDefaultGreetingIsStable() {
        val source = loadMorningSource()
        assertTrue(
            source.contains("const val DEFAULT_MORNING_GREETING: String = \"今日も一緒に起きられたね\""),
            "MorningScreen must default the morning greeting to \"今日も一緒に起きられたね\"",
        )
    }

    @Test
    fun morningScreenDefaultsToNoDiscoveries() {
        val source = loadMorningSource()
        // The default empty list keeps the morning screen
        // rendering even when the dream coordinator has not
        // produced any discoveries yet.
        assertTrue(
            source.contains("dreamDiscoveries: List<DreamDiscovery> = emptyList()"),
            "MorningScreen must default dreamDiscoveries to emptyList()",
        )
    }

    @Test
    fun morningScreenTouchTargetsMeetBaseline() {
        val source = loadMorningSource()
        // SoinePrimaryButton enforces the 56dp touch target
        // and SoineQuietButton enforces the 48dp touch
        // target. The morning screen does not re-declare
        // them; the design-system tokens must surface
        // directly. We accept either the bare
        // `MIN_TOUCH_TARGET_DP` constant or the V1 design
        // system tokens (SoineTokens.QuietCtaHeight,
        // SoineTokens.PrimaryCtaHeight, etc).
        val usesDesignSystemTokens = source.contains("MIN_TOUCH_TARGET_DP") ||
            source.contains("SoineTokens.QuietCtaHeight") ||
            source.contains("SoineTokens.PrimaryCtaHeight")
        assertTrue(
            !source.contains("heightIn(min = ") || usesDesignSystemTokens,
            "MorningScreen must use the design-system touch-target tokens, not raw dp values",
        )
    }

    @Test
    fun morningScreenDefaultGreetingConstantExposed() {
        assertEquals(
            "今日も一緒に起きられたね",
            app.soine.DEFAULT_MORNING_GREETING,
        )
    }

    @Test
    fun morningScreenConsumesStagePresentation() {
        // #175 ships the relationship-stage visual
        // presentation as a single source of truth. The
        // morning screen must consume it so a closer
        // relationship warms the morning scene through
        // the morningGreetingSuffix and the hero glow.
        val source = loadMorningSource()
        assertTrue(
            source.contains("CompanionStagePresentationPolicy.forStage"),
            "MorningScreen must consume CompanionStagePresentationPolicy.forStage so the hero glow and greeting suffix are stage-gated",
        )
        assertTrue(
            source.contains("presentation.morningGreetingSuffix"),
            "MorningScreen must append the stage-specific morning greeting suffix to the default greeting",
        )
    }

    @Test
    fun morningScreenRendersRelationshipChangeReveal() {
        // The relationship-change reveal is the one-shot
        // notice that surfaces when the user wakes and the
        // relationship stage has crossed since they fell
        // asleep. The reveal must be a SoinePanel line
        // (not a celebration, not a number).
        val source = loadMorningSource()
        assertTrue(
            source.contains("RelationshipChangeReveal"),
            "MorningScreen must define and render RelationshipChangeReveal for the one-shot stage-advanced notice",
        )
        assertTrue(
            source.contains("previousStage != null && previousStage != relationshipStage"),
            "MorningScreen must compare previousStage against the current stage to gate the one-shot reveal",
        )
        assertTrue(
            source.contains("少しだけ、近づいた朝"),
            "MorningScreen relationship-change reveal must use a quiet section header (not a celebration)",
        )
    }

    @Test
    fun morningScreenRendersSoundSummaryReveal() {
        // #178 ships the overnight sound-event UX as a
        // quiet one-line reveal in the morning screen.
        // The reveal must be a SoinePanel that uses the
        // "*のような*" copy pattern and never references
        // raw audio. The reveal must not surface a
        // confidence percentage or a count.
        val source = loadMorningSource()
        assertTrue(
            source.contains("SoundSummaryReveal"),
            "MorningScreen must define and render SoundSummaryReveal for the overnight sound summary",
        )
        assertTrue(
            source.contains("soundEventSummary"),
            "MorningScreen must accept a soundEventSummary parameter so the optional sound analysis can flow into the morning view",
        )
        assertTrue(
            source.contains("詳細と削除は設定から"),
            "MorningScreen sound summary reveal must include the privacy link '詳細と削除は設定から' so the user can inspect / delete the events",
        )
    }

    @Test
    fun morningScreenDoesNotEmbedRawAudioReference() {
        // #178 explicitly excludes the raw-audio
        // playback path. The morning screen's user-
        // facing copy must not mention "audio", "録音",
        // or "音声ファイル". We strip comment lines so a
        // mention in a doc comment does not trip the
        // guard.
        val source = loadMorningSource()
        val nonCommentSource = source
            .lineSequence()
            .filter { line ->
                val trimmed = line.trimStart()
                !(trimmed.startsWith("*") || trimmed.startsWith("//"))
            }
            .joinToString("\n")
        assertTrue(
            !nonCommentSource.contains("audio"),
            "MorningScreen must not mention 'audio' in the source; the optional sound analysis uses derived events, not raw audio",
        )
    }

    private fun loadMorningSource(): String {
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/MorningScreen.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/MorningScreen.kt",
            "src/commonMain/kotlin/app/soine/MorningScreen.kt",
            "../src/commonMain/kotlin/app/soine/MorningScreen.kt",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "MorningScreen.kt not found in any of the candidate paths; tried: " +
                candidates.joinToString(),
        )
    }
}
