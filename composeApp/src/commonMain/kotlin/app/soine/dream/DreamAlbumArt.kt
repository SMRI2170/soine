package app.soine.dream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.soine.design.SoineColors
import app.soine.design.SoineTokens

/**
 * Visual identity primitive for the Dream Album.
 *
 * Each [DreamDefinition.artKey] is mapped deterministically to a
 * [DreamMotif] and a [DreamPalette] so a list of dreams reads as
 * a quiet collection rather than a row of identical "夢" cards.
 *
 * The motifs are intentionally abstract geometric shapes — a
 * circle for a moon, a star, a small cluster for a constellation.
 * They are not literal illustrations; the album stays text-first
 * and the visual is a memory trigger. New art keys are not
 * required: an unknown art key falls back to a neutral motif and
 * a neutral palette, so the catalog can grow without the visual
 * surface drifting.
 *
 * The palette is composed exclusively from [SoineColors]. A new
 * dream does not introduce a new color — only a new combination
 * of existing tokens — so the dusk-first identity stays intact.
 */
enum class DreamMotif {
    /** A soft full circle — moon, sun, planet, lantern. */
    ORB,
    /** A five-pointed star — pillow of stars, stargazing. */
    STAR,
    /** A horizontal line of small dots — lantern path, fireflies, constellation. */
    PATH,
    /** A single tall rectangle — window, library, room, balcony. */
    RECTANGLE,
    /** A wide arc — soft hill, breeze, morning. */
    ARC,
    /** A small filled triangle — paper boat, mountain. */
    TRIANGLE,
    /** A small cluster of two circles — cloud, bubbles, planet + moon. */
    CLUSTER,
    /** A scalloped half-circle — autumn moon, snow lantern, lantern. */
    CRESCENT,
}

/**
 * A two-color gradient with an accent stroke. Built exclusively
 * from the Soine palette so the album inherits the dusk identity.
 */
data class DreamPalette(
    val start: Color,
    val end: Color,
    val accent: Color,
) {
    companion object {
        /**
         * The neutral palette used when an artKey is null or
         * unknown. Visually a quiet dim surface so the album
         * never breaks the layout when the catalog is extended.
         */
        val Neutral: DreamPalette = DreamPalette(
            start = SoineColors.dusk,
            end = SoineColors.dim,
            accent = SoineColors.hush,
        )
    }
}

/**
 * The default palette set. Six gradients built from the existing
 * Soine tokens. A new dream is mapped to one of these by a
 * deterministic hash of the artKey so the visual variety stays
 * stable across runs.
 */
internal val DREAM_PALETTES: List<DreamPalette> = listOf(
    // cream-on-dusk — the anchor, used most often (the
    // "common" mood).
    DreamPalette(SoineColors.cream, SoineColors.dusk, SoineColors.glow),
    // sunrise-on-twilight — warm, morning-leaning.
    DreamPalette(SoineColors.sunrise, SoineColors.twilight, SoineColors.ember),
    // glow-on-midnight — high-contrast, the brighter dreams.
    DreamPalette(SoineColors.glow, SoineColors.midnight, SoineColors.cream),
    // ember-on-dusk — the deeper, more grounded dreams.
    DreamPalette(SoineColors.ember, SoineColors.dusk, SoineColors.sunrise),
    // soft-on-twilight — quieter, for the contemplative dreams.
    DreamPalette(SoineColors.soft, SoineColors.twilight, SoineColors.hush),
    // dim-on-midnight — the most subdued, for the locked / winter moods.
    DreamPalette(SoineColors.dim, SoineColors.midnight, SoineColors.hush),
)

/**
 * Map a [DreamDefinition.artKey] to a stable [DreamMotif]. The
 * mapping is purely deterministic so a dream's visual identity
 * does not change between sessions, devices, or build runs. A
 * null or empty artKey falls back to [DreamMotif.ORB], the
 * visual equivalent of "unknown".
 */
fun motifFor(artKey: String?): DreamMotif {
    if (artKey.isNullOrBlank()) return DreamMotif.ORB
    val motifs = DreamMotif.values()
    val idx = (artKey.hashCode().rem(motifs.size).let { if (it < 0) it + motifs.size else it })
    return motifs[idx]
}

/**
 * Map a [DreamDefinition.artKey] to a stable [DreamPalette].
 * Same deterministic contract as [motifFor]. A null or empty
 * artKey falls back to [DreamPalette.Neutral].
 */
fun paletteFor(artKey: String?): DreamPalette {
    if (artKey.isNullOrBlank()) return DreamPalette.Neutral
    val palettes = DREAM_PALETTES
    val idx = (artKey.hashCode().rem(palettes.size).let { if (it < 0) it + palettes.size else it })
    return palettes[idx]
}

/**
 * Resolve the visual identity of a dream. Pure function so a
 * test can pin the (motif, palette) tuple for any artKey
 * without rendering the composable.
 */
data class DreamVisualIdentity(
    val motif: DreamMotif,
    val palette: DreamPalette,
)

fun visualIdentityFor(artKey: String?): DreamVisualIdentity =
    DreamVisualIdentity(motif = motifFor(artKey), palette = paletteFor(artKey))

/**
 * The Dream Album art tile. A small gradient square with the
 * dream's motif shape painted on top. Used both in the album
 * list and the detail sheet so the same visual identity
 * follows the dream from index to detail.
 *
 * When [discovered] is `false`, the tile is rendered as a
 * silhouette: the same gradient direction, the same shape,
 * but the surface and motif are dimmed and the accent is
 * removed. The silhouette still has a world-feel (a moon is
 * still a moon, a constellation is still a constellation)
 * so an empty album reads as a quiet collection rather than
 * a row of question marks.
 *
 * The tile is exposed to the screen reader as a single merged
 * content description so TalkBack / VoiceOver read the dream
 * as one item, not as separate "image + title + date" nodes.
 */
@Composable
fun DreamAlbumArtTile(
    artKey: String?,
    discovered: Boolean,
    modifier: Modifier = Modifier,
    accessibilityLabel: String,
) {
    val identity = visualIdentityFor(artKey)
    val effectivePalette = if (discovered) {
        identity.palette
    } else {
        // Silhouette palette: dimmer of the same gradient pair
        // and a soft accent so the tile still feels like the
        // same dream, just asleep.
        DreamPalette(
            start = identity.palette.end.copy(alpha = 0.55f),
            end = SoineColors.midnight.copy(alpha = 0.85f),
            accent = SoineColors.hush.copy(alpha = 0.6f),
        )
    }
    val shape = RoundedCornerShape(SoineTokens.RadiusLg)

    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .then(if (!discovered) Modifier.alpha(0.85f) else Modifier),
        shape = shape,
        color = effectivePalette.end,
        contentColor = effectivePalette.end,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize: Size = size
                val w = canvasSize.width
                val h = canvasSize.height
                val brush = Brush.linearGradient(
                    colors = listOf(effectivePalette.start, effectivePalette.end),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
                // Background gradient
                drawRect(brush = brush, size = canvasSize)
                // Motif stroke / fill on top
                drawMotif(
                    motif = identity.motif,
                    accent = effectivePalette.accent,
                    width = w,
                    height = h,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMotif(
    motif: DreamMotif,
    accent: Color,
    width: Float,
    height: Float,
) {
    val cx = width / 2f
    val cy = height / 2f
    when (motif) {
        DreamMotif.ORB -> {
            // Centered filled circle, accent ring around it.
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = minOf(width, height) * 0.28f,
                center = Offset(cx, cy),
            )
        }
        DreamMotif.STAR -> {
            // A five-pointed star path.
            val path = fivePointStarPath(
                cx = cx,
                cy = cy,
                outer = minOf(width, height) * 0.34f,
                inner = minOf(width, height) * 0.16f,
            )
            drawPath(path = path, color = accent.copy(alpha = 0.9f))
        }
        DreamMotif.PATH -> {
            // A horizontal line of small dots — a constellation row.
            val count = 5
            val totalWidth = width * 0.7f
            val startX = (width - totalWidth) / 2f
            val y = cy
            for (i in 0 until count) {
                val x = startX + (i.toFloat() / (count - 1).toFloat()) * totalWidth
                drawCircle(
                    color = accent.copy(alpha = 0.85f),
                    radius = minOf(width, height) * 0.06f,
                    center = Offset(x, y),
                )
            }
        }
        DreamMotif.RECTANGLE -> {
            // A tall rounded rectangle — a window frame.
            val rw = width * 0.42f
            val rh = height * 0.62f
            val rx = (width - rw) / 2f
            val ry = (height - rh) / 2f
            drawRoundRect(
                color = accent.copy(alpha = 0.55f),
                topLeft = Offset(rx, ry),
                size = Size(rw, rh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rw * 0.18f, rw * 0.18f),
            )
            // Inner highlight — a small lit window pane
            drawRoundRect(
                color = accent.copy(alpha = 0.9f),
                topLeft = Offset(rx + rw * 0.18f, ry + rh * 0.18f),
                size = Size(rw * 0.64f, rh * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rw * 0.12f, rw * 0.12f),
            )
        }
        DreamMotif.ARC -> {
            // A wide soft arc — a hill silhouette.
            val path = Path().apply {
                val startX = width * 0.1f
                val endX = width * 0.9f
                val baseY = height * 0.72f
                moveTo(startX, baseY)
                quadraticTo(cx, height * 0.28f, endX, baseY)
                lineTo(endX, height)
                lineTo(startX, height)
                close()
            }
            drawPath(path = path, color = accent.copy(alpha = 0.7f))
        }
        DreamMotif.TRIANGLE -> {
            // A small filled triangle — a paper boat or mountain.
            val path = Path().apply {
                val baseY = height * 0.78f
                val wHalf = width * 0.32f
                moveTo(cx, height * 0.22f)
                lineTo(cx + wHalf, baseY)
                lineTo(cx - wHalf, baseY)
                close()
            }
            drawPath(path = path, color = accent.copy(alpha = 0.9f))
        }
        DreamMotif.CLUSTER -> {
            // Two overlapping circles — a cloud, bubbles, or a
            // tiny planet + moon.
            val r1 = minOf(width, height) * 0.26f
            val r2 = minOf(width, height) * 0.18f
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = r1,
                center = Offset(cx - width * 0.08f, cy + height * 0.04f),
            )
            drawCircle(
                color = accent.copy(alpha = 0.7f),
                radius = r2,
                center = Offset(cx + width * 0.12f, cy - height * 0.08f),
            )
        }
        DreamMotif.CRESCENT -> {
            // A circle with a smaller offset circle of the
            // background color to suggest a crescent moon. We
            // approximate with two filled circles.
            val r = minOf(width, height) * 0.34f
            drawCircle(
                color = accent.copy(alpha = 0.9f),
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = SoineColors.midnight.copy(alpha = 0.55f),
                radius = r * 0.85f,
                center = Offset(cx + r * 0.4f, cy - r * 0.1f),
            )
        }
    }
}

private fun fivePointStarPath(
    cx: Float,
    cy: Float,
    outer: Float,
    inner: Float,
): Path {
    val path = Path()
    val points = 5
    val step = kotlin.math.PI / points.toDouble()
    var angle = -kotlin.math.PI / 2.0
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val x = cx + r * kotlin.math.cos(angle).toFloat()
        val y = cy + r * kotlin.math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += step
    }
    path.close()
    return path
}
