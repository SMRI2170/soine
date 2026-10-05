package app.soine.storage

/**
 * Minimal platform storage boundary for the sleep-session snapshot.
 *
 * A single snapshot is used so active/completed session transitions can be
 * persisted as one platform preference write. Platform adapters decide how
 * that string is stored.
 */
interface SleepSessionStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}
