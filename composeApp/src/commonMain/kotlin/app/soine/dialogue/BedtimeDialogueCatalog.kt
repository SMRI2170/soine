package app.soine.dialogue

import app.soine.relationship.FamiliarityStage

enum class BedtimeDialogueMoment {
    ARRIVAL,
    SETTLING,
    LIGHTS_OUT,
}

data class BedtimeDialogueDefinition(
    val id: String,
    val text: String,
    val moment: BedtimeDialogueMoment,
    val minimumFamiliarity: FamiliarityStage = FamiliarityStage.NEW,
) {
    init {
        require(id.isNotBlank()) { "Bedtime dialogue id must not be blank." }
        require(text.isNotBlank()) { "Bedtime dialogue text must not be blank." }
    }
}

object BedtimeDialogueCatalog {
    val all: List<BedtimeDialogueDefinition> = listOf(
        // ARRIVAL — 10
        BedtimeDialogueDefinition("bed-arrival-01", "今日も一緒に眠ろう", BedtimeDialogueMoment.ARRIVAL),
        BedtimeDialogueDefinition("bed-arrival-02", "来てくれたね", BedtimeDialogueMoment.ARRIVAL),
        BedtimeDialogueDefinition("bed-arrival-03", "ここで待ってたよ", BedtimeDialogueMoment.ARRIVAL),
        BedtimeDialogueDefinition("bed-arrival-04", "夜の時間だね", BedtimeDialogueMoment.ARRIVAL),
        BedtimeDialogueDefinition("bed-arrival-05", "そろそろ、ゆっくりしよう", BedtimeDialogueMoment.ARRIVAL),
        BedtimeDialogueDefinition("bed-arrival-06", "今日もそばにいるね", BedtimeDialogueMoment.ARRIVAL, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-arrival-07", "また一緒の夜だね", BedtimeDialogueMoment.ARRIVAL, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-arrival-08", "この時間、なんだか落ち着くね", BedtimeDialogueMoment.ARRIVAL, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-arrival-09", "今日もここがいいな", BedtimeDialogueMoment.ARRIVAL, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-arrival-10", "今日はすぐそばにいるね", BedtimeDialogueMoment.ARRIVAL, FamiliarityStage.CLOSE),

        // SETTLING — 10
        BedtimeDialogueDefinition("bed-settle-01", "少しずつ静かにしよう", BedtimeDialogueMoment.SETTLING),
        BedtimeDialogueDefinition("bed-settle-02", "もう少しだけ、のんびり", BedtimeDialogueMoment.SETTLING),
        BedtimeDialogueDefinition("bed-settle-03", "ここ、あったかいね", BedtimeDialogueMoment.SETTLING),
        BedtimeDialogueDefinition("bed-settle-04", "ゆっくり丸くなるね", BedtimeDialogueMoment.SETTLING),
        BedtimeDialogueDefinition("bed-settle-05", "そろそろ目を閉じようかな", BedtimeDialogueMoment.SETTLING),
        BedtimeDialogueDefinition("bed-settle-06", "今日も隣で落ち着くね", BedtimeDialogueMoment.SETTLING, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-settle-07", "一緒だと、少し安心するね", BedtimeDialogueMoment.SETTLING, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-settle-08", "この距離、好きかも", BedtimeDialogueMoment.SETTLING, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-settle-09", "もう少し近くに行くね", BedtimeDialogueMoment.SETTLING, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-settle-10", "今日はここで眠るね", BedtimeDialogueMoment.SETTLING, FamiliarityStage.CLOSE),

        // LIGHTS_OUT — 10, intentionally short
        BedtimeDialogueDefinition("bed-lights-01", "おやすみ", BedtimeDialogueMoment.LIGHTS_OUT),
        BedtimeDialogueDefinition("bed-lights-02", "また朝にね", BedtimeDialogueMoment.LIGHTS_OUT),
        BedtimeDialogueDefinition("bed-lights-03", "静かに眠ろう", BedtimeDialogueMoment.LIGHTS_OUT),
        BedtimeDialogueDefinition("bed-lights-04", "ここにいるよ", BedtimeDialogueMoment.LIGHTS_OUT),
        BedtimeDialogueDefinition("bed-lights-05", "目を閉じるね", BedtimeDialogueMoment.LIGHTS_OUT),
        BedtimeDialogueDefinition("bed-lights-06", "今日も隣だね", BedtimeDialogueMoment.LIGHTS_OUT, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-lights-07", "また一緒だね", BedtimeDialogueMoment.LIGHTS_OUT, FamiliarityStage.WARMING_UP),
        BedtimeDialogueDefinition("bed-lights-08", "近くで眠るね", BedtimeDialogueMoment.LIGHTS_OUT, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-lights-09", "ここが落ち着く", BedtimeDialogueMoment.LIGHTS_OUT, FamiliarityStage.FAMILIAR),
        BedtimeDialogueDefinition("bed-lights-10", "すぐそばにいるね", BedtimeDialogueMoment.LIGHTS_OUT, FamiliarityStage.CLOSE),
    )

    init {
        require(all.size >= 30) { "Bedtime catalog must contain at least 30 lines." }
        require(all.map { it.id }.distinct().size == all.size) {
            "Bedtime dialogue ids must be unique."
        }
    }

    fun eligible(
        stage: FamiliarityStage,
        moment: BedtimeDialogueMoment,
    ): List<BedtimeDialogueDefinition> =
        all.filter {
            it.moment == moment &&
                it.minimumFamiliarity.persistedValue <= stage.persistedValue
        }
}
