package com.evan.brightnesscurve.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun detectsNewerRemoteSemanticVersion() {
        assertTrue(VersionComparator.isRemoteNewer("1.0.0", "v1.0.1"))
        assertTrue(VersionComparator.isRemoteNewer("1.0.9", "1.1.0"))
        assertTrue(VersionComparator.isRemoteNewer("0.9.9", "v1.0.0"))
    }

    @Test
    fun ignoresEqualOrOlderRemoteVersion() {
        assertFalse(VersionComparator.isRemoteNewer("1.0.0", "v1.0.0"))
        assertFalse(VersionComparator.isRemoteNewer("1.0.1", "v1.0.0"))
        assertFalse(VersionComparator.isRemoteNewer("1.1.0", "1.0.9"))
    }
}
