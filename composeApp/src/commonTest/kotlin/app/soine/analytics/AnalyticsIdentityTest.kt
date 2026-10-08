package app.soine.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsIdentityTest {

    @Test
    fun generatedInstallIdIsThirtyTwoHexCharacters() {
        val id = AnalyticsIdentity.generate()

        assertEquals(32, id.length)
        assertTrue(id.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun generatedInstallIdsAreUnique() {
        val ids = List(64) { AnalyticsIdentity.generate() }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun ensureInstallIdReturnsCurrentWhenWellFormed() {
        val current = AnalyticsIdentity.generate()

        assertEquals(current, AnalyticsIdentity.ensureInstallId(current))
    }

    @Test
    fun ensureInstallIdRegeneratesWhenCurrentIsNull() {
        val result = AnalyticsIdentity.ensureInstallId(null)

        assertTrue(AnalyticsIdentity.isWellFormed(result))
    }

    @Test
    fun ensureInstallIdRegeneratesWhenCurrentIsMalformed() {
        val result = AnalyticsIdentity.ensureInstallId("not-a-hex-id")

        assertTrue(AnalyticsIdentity.isWellFormed(result))
        assertFalse(result == "not-a-hex-id")
    }

    @Test
    fun isWellFormedAcceptsOnlyLowercaseHexOfCorrectLength() {
        assertFalse(AnalyticsIdentity.isWellFormed(""))
        assertFalse(AnalyticsIdentity.isWellFormed("ABCD"))
        assertFalse(AnalyticsIdentity.isWellFormed("0123456789abcdef0123456789ABCDEF"))
        assertFalse(AnalyticsIdentity.isWellFormed("0123456789abcdef0123456789abcde"))
    }
}