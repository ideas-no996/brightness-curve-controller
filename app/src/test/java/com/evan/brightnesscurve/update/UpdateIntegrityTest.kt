package com.evan.brightnesscurve.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateIntegrityTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `normalizes GitHub release asset sha256 digest`() {
        val digest = "SHA256:ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"

        assertEquals(
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
            normalizeSha256Digest(digest)
        )
    }

    @Test
    fun `rejects invalid sha256 digest`() {
        assertNull(normalizeSha256Digest("sha256:not-a-digest"))
        assertNull(normalizeSha256Digest(null))
    }

    @Test
    fun `computes file sha256`() {
        val file = temp.newFile("payload.txt")
        file.writeText("hello")

        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            file.sha256Hex()
        )
    }
}
