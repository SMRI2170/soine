package app.soine.dialogue

import app.soine.night.NightEventType
import app.soine.relationship.FamiliarityStage

enum class MorningDialogueKind {
    GENERIC,
    EVENT,
    DREAM,
    ROUTINE,
    RELATIONSHIP,
}

data class MorningDialogueDefinition(
    val id: String,
    val text: String,
    val kind: MorningDialogueKind,
    val eventType: NightEventType? = null,
    val routineTriggerId: String? = null,
    val minimumFamiliarity: FamiliarityStage = FamiliarityStage.NEW,
) {
    init {
        require(id.isNotBlank()) { "Morning dialogue id must not be blank." }
        require(text.isNotBlank()) { "Morning dialogue text must not be blank." }
        require(eventType == null || kind == MorningDialogueKind.EVENT) {
            "eventType is only valid for EVENT dialogue."
        }
        require(routineTriggerId == null || kind == MorningDialogueKind.ROUTINE) {
            "routineTriggerId is only valid for ROUTINE dialogue."
        }
    }
}

object MorningDialogueCatalog {
    val all: List<MorningDialogueDefinition> = listOf(
        // GENERIC — 15
        generic("morning-generic-01", "おはよう"),
        generic("morning-generic-02", "朝になったね"),
        generic("morning-generic-03", "今日も一緒に起きられたね"),
        generic("morning-generic-04", "目が覚めたね"),
        generic("morning-generic-05", "新しい朝だね"),
        generic("morning-generic-06", "おはよう、ここにいるよ"),
        generic("morning-generic-07", "朝の空気だね"),
        generic("morning-generic-08", "ゆっくり朝にしよう"),
        generic("morning-generic-09", "今日も朝が来たね"),
        generic("morning-generic-10", "また朝に会えたね"),
        generic("morning-generic-11", "起きる時間だね"),
        generic("morning-generic-12", "おはよう、またあとでね"),
        generic("morning-generic-13", "朝の光だね"),
        generic("morning-generic-14", "今日もここからだね"),
        generic("morning-generic-15", "おはよう。そばにいたよ"),

        // EVENT — 16, two variants for every current NightEventType
        event("morning-event-turn-over-01", "夜中に、ころんと寝返りしてたよ", NightEventType.TURN_OVER),
        event("morning-event-turn-over-02", "くるっと向きを変えて眠ってたみたい", NightEventType.TURN_OVER),
        event("morning-event-ear-twitch-01", "寝ながら耳がぴくっとしてたみたい", NightEventType.EAR_TWITCH),
        event("morning-event-ear-twitch-02", "夜中に耳がちょっとだけ動いてたよ", NightEventType.EAR_TWITCH),
        event("morning-event-move-closer-01", "夜のあいだに、少し近くで眠ってたよ", NightEventType.MOVE_CLOSER),
        event("morning-event-move-closer-02", "気づいたら、少しそばに寄ってたみたい", NightEventType.MOVE_CLOSER),
        event("morning-event-curl-up-01", "いつの間にか、まるくなって眠ってたよ", NightEventType.CURL_UP),
        event("morning-event-curl-up-02", "夜中は小さく丸まってたみたい", NightEventType.CURL_UP),
        event("morning-event-brief-wake-01", "夜中に一度だけ、そっと目を開けてたみたい", NightEventType.BRIEF_WAKE),
        event("morning-event-brief-wake-02", "少しだけ目を覚まして、また眠ったみたい", NightEventType.BRIEF_WAKE),
        event("morning-event-funny-pose-01", "朝見たら、ちょっと不思議な寝相だったよ", NightEventType.FUNNY_POSE),
        event("morning-event-funny-pose-02", "なんだか変わった格好で眠ってたみたい", NightEventType.FUNNY_POSE),
        event("morning-event-dream-01", "なんだか夢を見ていたみたい", NightEventType.DREAM),
        event("morning-event-dream-02", "眠りながら、夢の中を歩いてたのかな", NightEventType.DREAM),
        event("morning-event-sound-reaction-01", "夜の音に、少しだけ反応してたみたい", NightEventType.SOUND_REACTION),
        event("morning-event-sound-reaction-02", "音がしたとき、ちょっとだけ動いてたよ", NightEventType.SOUND_REACTION),

        // DREAM — 6, shown only when a dream was actually discovered
        dream("morning-dream-01", "夢の続きを、まだ覚えてる気がする"),
        dream("morning-dream-02", "なんだか不思議な夢だったみたい"),
        dream("morning-dream-03", "夢の中でも一緒だったのかな"),
        dream("morning-dream-04", "朝まで夢を持って帰ってきたよ"),
        dream("morning-dream-05", "夢の景色、まだ少し残ってる"),
        dream("morning-dream-06", "今夜もまた夢に会えるかな"),

        // ROUTINE — 6, tied to already-confidence-gated routine triggers
        routine("morning-routine-early-01", "今日は少し早めの朝だね", "routine-wake-earlier"),
        routine("morning-routine-early-02", "いつもより早い時間に会えたね", "routine-wake-earlier"),
        routine("morning-routine-late-01", "今日はゆっくりの朝だね", "routine-wake-later"),
        routine("morning-routine-late-02", "いつもより少し遅めの朝だね", "routine-wake-later"),
        routine("morning-routine-weekend-01", "休日の朝は、少しゆっくりだね", "routine-weekend-morning"),
        routine("morning-routine-weekend-02", "今日はいつもの休日らしい朝だね", "routine-weekend-morning"),

        // RELATIONSHIP — 7
        relationship("morning-rel-warming-01", "また一緒の朝になったね", FamiliarityStage.WARMING_UP),
        relationship("morning-rel-warming-02", "少しずつ、朝の感じも覚えてきたよ", FamiliarityStage.WARMING_UP),
        relationship("morning-rel-familiar-01", "一緒に起きる朝、増えてきたね", FamiliarityStage.FAMILIAR),
        relationship("morning-rel-familiar-02", "この朝の感じ、もうおなじみだね", FamiliarityStage.FAMILIAR),
        relationship("morning-rel-close-01", "おはよう。今日もすぐそばにいるよ", FamiliarityStage.CLOSE),
        relationship("morning-rel-close-02", "朝になっても、近くにいるね", FamiliarityStage.CLOSE),
        relationship("morning-rel-close-03", "今日も一番近くで朝を迎えたね", FamiliarityStage.CLOSE),
    )

    init {
        require(all.size >= 50) { "Morning dialogue catalog must contain at least 50 lines." }
        require(all.map { it.id }.distinct().size == all.size) {
            "Morning dialogue ids must be unique."
        }
    }

    fun genericDefinitions(): List<MorningDialogueDefinition> =
        all.filter { it.kind == MorningDialogueKind.GENERIC }

    fun forEvent(type: NightEventType): List<MorningDialogueDefinition> =
        all.filter { it.kind == MorningDialogueKind.EVENT && it.eventType == type }

    fun dreamDefinitions(): List<MorningDialogueDefinition> =
        all.filter { it.kind == MorningDialogueKind.DREAM }

    fun forRoutineTrigger(triggerId: String): List<MorningDialogueDefinition> =
        all.filter {
            it.kind == MorningDialogueKind.ROUTINE &&
                it.routineTriggerId == triggerId
        }

    fun forRelationship(stage: FamiliarityStage): List<MorningDialogueDefinition> =
        all.filter {
            it.kind == MorningDialogueKind.RELATIONSHIP &&
                it.minimumFamiliarity.persistedValue <= stage.persistedValue
        }

    private fun generic(id: String, text: String) = MorningDialogueDefinition(
        id = id,
        text = text,
        kind = MorningDialogueKind.GENERIC,
    )

    private fun event(
        id: String,
        text: String,
        type: NightEventType,
    ) = MorningDialogueDefinition(
        id = id,
        text = text,
        kind = MorningDialogueKind.EVENT,
        eventType = type,
    )

    private fun dream(id: String, text: String) = MorningDialogueDefinition(
        id = id,
        text = text,
        kind = MorningDialogueKind.DREAM,
    )

    private fun routine(
        id: String,
        text: String,
        triggerId: String,
    ) = MorningDialogueDefinition(
        id = id,
        text = text,
        kind = MorningDialogueKind.ROUTINE,
        routineTriggerId = triggerId,
    )

    private fun relationship(
        id: String,
        text: String,
        stage: FamiliarityStage,
    ) = MorningDialogueDefinition(
        id = id,
        text = text,
        kind = MorningDialogueKind.RELATIONSHIP,
        minimumFamiliarity = stage,
    )
}
