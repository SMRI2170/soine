package app.soine.companion

/**
 * The V1 production-asset pipeline contract.
 *
 * #173 requires that the V1 ship the static 2D fallback as
 * the primary path; the production 3D asset lives in the
 * art pipeline and ships behind a future polish slice. The
 * [CompanionAssetPipelineStatus] enum is the single source
 * of truth for "which path is the V1 default?" so a future
 * artist session can flip the status without touching the
 * call sites.
 *
 * The static 2D fallback is not a degraded path. It is the
 * V1 voice: the same soft, kaomoji-style companion glyphs
 * ([CompanionStaticFallback]) are the visual identity the
 * user reads every night. The production 3D asset is a
 * higher-fidelity version of the same identity, not a
 * replacement.
 *
 * The contract is intentionally narrow. The 3D renderer
 * reads the same [CompanionRenderRequest] that the static
 * fallback reads, so the renderer swap is a one-line
 * change in [CompanionRenderCoordinator]. The renderer
 * does not need to know which pipeline is active; the
 * pipeline status is owned by this file.
 */
enum class CompanionAssetPipelineStatus {
    /**
     * The V1 default. The static 2D fallback
     * ([CompanionStaticFallback]) is the primary path.
     * A future artist session can flip the status to
     * [PRODUCTION_READY] when the production 3D asset
     * ships and the renderer can load it on a real
     * device PoC.
     */
    STATIC_FALLBACK,

    /**
     * The production 3D asset is loaded, the renderer
     * is wired, and a device PoC has verified the
     * performance budget. The static fallback remains
     * the graceful-degradation path; the renderer flips
     * to it through [CompanionRendererFallbackPolicy]
     * when the 3D initialization fails.
     */
    PRODUCTION_READY,
}

/**
 * The V1 pipeline owner. The single instance owns the
 * V1 default. A future polish slice flips the status by
 * changing the [CompanionAssetPipelineStatus] constant;
 * the call sites do not change.
 */
object CompanionAssetPipeline {
    /**
     * The V1 default. The static 2D fallback is the
     * primary path. The production 3D asset is owned by
     * a future polish slice (#173) and is not in V1.
     */
    val current: CompanionAssetPipelineStatus = CompanionAssetPipelineStatus.STATIC_FALLBACK
}

/**
 * The production-asset acceptance criteria.
 *
 * #173 lists the asset-quality tasks. A future polish
 * slice (or a future artist session) reads these criteria
 * to flip [CompanionAssetPipeline.current] from
 * [CompanionAssetPipelineStatus.STATIC_FALLBACK] to
 * [CompanionAssetPipelineStatus.PRODUCTION_READY]. The
 * criteria are pinned in source so a future contributor
 * cannot drift the bar.
 */
object CompanionAssetAcceptance {
    /**
     * The production mesh topology must stay under the
     * mobile triangle count target. The exact budget is
     * determined by a real device PoC; the V1 ships a
     * conservative ceiling.
     */
    const val MAX_TRIANGLES: Int = 8_000

    /**
     * The compact skeleton must keep the bone count
     * under the mobile animation budget. A skeleton with
     * more than this many bones does not animate within
     * the per-frame budget on a mid-range Android.
     */
    const val MAX_BONES: Int = 48

    /**
     * The maximum GLB asset size in bytes, after
     * compression. The V1 ships the PoC at ~30 KB; the
     * production asset can grow but must stay under
     * this ceiling.
     */
    const val MAX_GLB_BYTES: Long = 1_500_000L

    /**
     * The list of semantic-intent clip ids the
     * production asset must include. The V1 fallback
     * covers every intent; the production asset must
     * too, or the renderer falls back to the static path
     * through [CompanionRendererFallbackPolicy].
     */
    val REQUIRED_CLIP_IDS: Set<String> = CompanionAnimationMapping.requiredClipIds
}
