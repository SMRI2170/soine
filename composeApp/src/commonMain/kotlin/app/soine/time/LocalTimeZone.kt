package app.soine.time

/**
 * Display-timezone abstraction used by all UI time / date rendering.
 *
 * Soine stores absolute timestamps as epoch millis. Timezone information
 * belongs only at the display edge, where the renderer converts an
 * epoch instant into the user's local wall clock. This interface is the
 * single place that holds that conversion.
 *
 * The V1 default implementation is [JapanLocalTimeZone] because Soine
 * V1 ships in Japan only. Multi-region releases swap the binding at
 * app start (or hot-update it on timezone change) without touching
 * the renderers.
 *
 * Implementations MUST NOT mutate state. They are queried at render
 * time so a swap in [LocalTimeZones.current] is reflected immediately.
 */
interface LocalTimeZone {
    /**
     * UTC offset in milliseconds at [epochMillis]. The return value is
     * the offset that applies at that instant (so DST-aware zones can
     * return different offsets across a year). Implementations that do
     * not observe DST return a constant.
     *
     * @param epochMillis absolute instant
     * @return offset added to UTC to obtain the local wall clock, in
     *   milliseconds; negative for zones west of UTC.
     */
    fun utcOffsetMillisAt(epochMillis: Long): Int
}

/**
 * Fixed UTC+09:00 zone used by the V1 launch.
 *
 * Soine V1 ships in Japan only; the offset is constant and does not
 * observe DST (Japan has not observed DST since 1951).
 */
object JapanLocalTimeZone : LocalTimeZone {
    private const val OFFSET_MILLIS = 9 * 60 * 60 * 1_000

    override fun utcOffsetMillisAt(epochMillis: Long): Int = OFFSET_MILLIS
}

/**
 * UTC zone used by tests to assert the abstraction actually switches
 * behaviour when the offset changes.
 */
object UtcTimeZone : LocalTimeZone {
    override fun utcOffsetMillisAt(epochMillis: Long): Int = 0
}

/**
 * Mutable binding that the app wires at startup. Renderer code reads
 * [current] at call time so a swap is reflected immediately without
 * any cache invalidation.
 *
 * Tests swap this binding to exercise travel and DST scenarios.
 */
object LocalTimeZones {
    @Volatile
    var current: LocalTimeZone = JapanLocalTimeZone
}