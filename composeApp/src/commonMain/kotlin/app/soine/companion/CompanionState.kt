package app.soine.companion

enum class CompanionMood { AWAKE, SETTLING, SLEEPING, WAKING }

data class CompanionState(
    val mood: CompanionMood = CompanionMood.AWAKE,
    val totalSleepMillis: Long = 0,
) {
    fun forSleepStart() = copy(mood = CompanionMood.SETTLING)
    fun asleep() = copy(mood = CompanionMood.SLEEPING)
    fun wake(sessionDurationMillis: Long) = copy(
        mood = CompanionMood.WAKING,
        totalSleepMillis = totalSleepMillis + sessionDurationMillis.coerceAtLeast(0),
    )
}
