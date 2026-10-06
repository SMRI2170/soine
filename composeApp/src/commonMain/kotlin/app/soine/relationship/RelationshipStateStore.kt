package app.soine.relationship

interface RelationshipStateStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}
