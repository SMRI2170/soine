package app.soine.dream

import app.soine.night.RarityBand
import app.soine.relationship.FamiliarityStage

/**
 * Initial authored dream catalog.
 *
 * Dream IDs are stable collection identities. Text/art revisions keep the ID
 * and increment the definition contentVersion instead of creating duplicates.
 */
object InitialDreamCatalog {
    val definitions: List<DreamDefinition> = listOf(
        // NEW / all-year
        dream(
            id = "cloud-nap",
            title = "雲の上のひるね",
            line = "ふわふわの雲の上で、のんびりしてたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_cloud_nap",
        ),
        dream(
            id = "moon-window",
            title = "月の窓",
            line = "大きな月を、窓のすぐそばで眺めていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_moon_window",
        ),
        dream(
            id = "star-pillow",
            title = "星のまくら",
            line = "小さな星をまくらにして、ころんと休んでいたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_star_pillow",
        ),
        dream(
            id = "quiet-library",
            title = "しずかな図書室",
            line = "誰もいない図書室で、絵本を一冊ひらいていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_quiet_library",
        ),
        dream(
            id = "tiny-train",
            title = "小さな夜汽車",
            line = "窓の外を見ながら、小さな夜汽車に揺られていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_tiny_train",
        ),
        dream(
            id = "lantern-path",
            title = "あかりの小道",
            line = "やさしい灯りが並ぶ道を、ゆっくり歩いていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_lantern_path",
        ),
        dream(
            id = "paper-boat",
            title = "紙の舟",
            line = "小さな紙の舟で、静かな水の上を進んでいたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_paper_boat",
        ),
        dream(
            id = "soft-hill",
            title = "やわらかな丘",
            line = "草のやわらかな丘で、空を見上げていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_soft_hill",
        ),
        dream(
            id = "warm-bakery",
            title = "朝前のパン屋さん",
            line = "まだ静かなパン屋さんで、焼ける香りを待っていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_warm_bakery",
        ),
        dream(
            id = "glass-bubbles",
            title = "ガラスの泡",
            line = "きらきらした泡が、ゆっくり空へ浮かんでいったみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.NEW,
            artKey = "dream_glass_bubbles",
        ),

        // NEW / seasonal
        dream(
            id = "spring-petals",
            title = "花びらの道",
            line = "風に舞う花びらを追いかけていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.NEW,
            seasons = setOf(DreamSeason.SPRING),
            artKey = "dream_spring_petals",
        ),
        dream(
            id = "summer-fireflies",
            title = "ほたるの川辺",
            line = "小さな光が飛ぶ川辺を、静かに眺めていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.NEW,
            seasons = setOf(DreamSeason.SUMMER),
            artKey = "dream_summer_fireflies",
        ),

        // WARMING_UP
        dream(
            id = "shared-umbrella",
            title = "ふたりの傘",
            line = "大きな傘の下を、誰かと並んで歩いていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_shared_umbrella",
        ),
        dream(
            id = "window-seat",
            title = "窓ぎわの席",
            line = "同じ窓の景色を、となりで見ていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_window_seat",
        ),
        dream(
            id = "midnight-picnic",
            title = "夜ふかしピクニック",
            line = "月明かりの下で、小さなおやつを分けていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_midnight_picnic",
        ),
        dream(
            id = "floating-room",
            title = "空に浮かぶ部屋",
            line = "いつもの部屋が、雲の上をゆっくり漂っていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_floating_room",
        ),
        dream(
            id = "autumn-acorns",
            title = "どんぐりの広場",
            line = "ころころ転がるどんぐりを、一緒に集めていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.WARMING_UP,
            seasons = setOf(DreamSeason.AUTUMN),
            artKey = "dream_autumn_acorns",
        ),
        dream(
            id = "winter-window",
            title = "雪の窓辺",
            line = "窓の外に積もる雪を、あたたかい部屋から見ていたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.WARMING_UP,
            seasons = setOf(DreamSeason.WINTER),
            artKey = "dream_winter_window",
        ),
        dream(
            id = "rainy-rooftop",
            title = "雨音の屋根",
            line = "屋根に落ちる雨の音を、ふたりで聞いていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_rainy_rooftop",
        ),
        dream(
            id = "constellation-map",
            title = "星座の地図",
            line = "見たことのない星座を、地図に描いていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.WARMING_UP,
            artKey = "dream_constellation_map",
        ),

        // FAMILIAR
        dream(
            id = "secret-balcony",
            title = "ひみつのベランダ",
            line = "いつもの部屋の外に、知らないベランダを見つけたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.FAMILIAR,
            artKey = "dream_secret_balcony",
        ),
        dream(
            id = "same-blanket",
            title = "ひとつの毛布",
            line = "大きな毛布を分けながら、のんびり空を見ていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.FAMILIAR,
            artKey = "dream_same_blanket",
        ),
        dream(
            id = "tiny-planet",
            title = "ふたりだけの小さな星",
            line = "手のひらくらいの星を、ふたりで歩いていたみたい",
            rarity = RarityBand.RARE,
            stage = FamiliarityStage.FAMILIAR,
            artKey = "dream_tiny_planet",
        ),
        dream(
            id = "spring-night-breeze",
            title = "春の夜風",
            line = "あたたかな夜風の中を、ゆっくり散歩していたみたい",
            rarity = RarityBand.COMMON,
            stage = FamiliarityStage.FAMILIAR,
            seasons = setOf(DreamSeason.SPRING),
            artKey = "dream_spring_night_breeze",
        ),
        dream(
            id = "summer-stargazing",
            title = "夏の星見",
            line = "遠くの波音を聞きながら、星を数えていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.FAMILIAR,
            seasons = setOf(DreamSeason.SUMMER),
            artKey = "dream_summer_stargazing",
        ),
        dream(
            id = "autumn-moon",
            title = "秋のまるい月",
            line = "大きな月の下で、落ち葉の音を聞いていたみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.FAMILIAR,
            seasons = setOf(DreamSeason.AUTUMN),
            artKey = "dream_autumn_moon",
        ),
        dream(
            id = "snow-lantern",
            title = "雪あかり",
            line = "雪の中に小さな灯りを並べていたみたい",
            rarity = RarityBand.RARE,
            stage = FamiliarityStage.FAMILIAR,
            seasons = setOf(DreamSeason.WINTER),
            artKey = "dream_snow_lantern",
        ),

        // CLOSE
        dream(
            id = "home-in-clouds",
            title = "雲の上のおうち",
            line = "雲の上に、ふたりで帰る小さな部屋があったみたい",
            rarity = RarityBand.UNCOMMON,
            stage = FamiliarityStage.CLOSE,
            artKey = "dream_home_in_clouds",
        ),
        dream(
            id = "morning-before-morning",
            title = "朝になる少し前",
            line = "空が明るくなるまで、となりで静かに座っていたみたい",
            rarity = RarityBand.RARE,
            stage = FamiliarityStage.CLOSE,
            artKey = "dream_morning_before_morning",
        ),
        dream(
            id = "endless-bedroom",
            title = "ずっと続く寝室",
            line = "扉を開けてもまた同じ部屋で、ふたりで笑っていたみたい",
            rarity = RarityBand.RARE,
            stage = FamiliarityStage.CLOSE,
            artKey = "dream_endless_bedroom",
        ),
    )

    val repository: DreamDefinitionRepository =
        InMemoryDreamDefinitionRepository(definitions)

    init {
        require(definitions.size == 30) {
            "Initial dream catalog must contain exactly 30 definitions."
        }
    }

    private fun dream(
        id: String,
        title: String,
        line: String,
        rarity: RarityBand,
        stage: FamiliarityStage,
        artKey: String,
        seasons: Set<DreamSeason> = emptySet(),
    ) = DreamDefinition(
        id = id,
        title = title,
        shortLine = line,
        rarity = rarity,
        minimumFamiliarity = stage.persistedValue,
        eligibleSeasons = seasons,
        artKey = artKey,
        contentVersion = DreamDefinition.CURRENT_CONTENT_VERSION,
    )
}
