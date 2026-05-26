package com.evan.brightnesscurve.update

object VersionComparator {
    fun isRemoteNewer(currentVersion: String, remoteVersion: String): Boolean {
        return compare(currentVersion, remoteVersion) < 0
    }

    fun compare(left: String, right: String): Int {
        val leftParts = left.toVersionParts()
        val rightParts = right.toVersionParts()
        val size = maxOf(leftParts.size, rightParts.size)

        for (index in 0 until size) {
            val leftValue = leftParts.getOrElse(index) { 0 }
            val rightValue = rightParts.getOrElse(index) { 0 }
            if (leftValue != rightValue) return leftValue.compareTo(rightValue)
        }

        return 0
    }

    private fun String.toVersionParts(): List<Int> {
        return trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { part ->
                part.takeWhile { it.isDigit() }
                    .takeIf { it.isNotBlank() }
                    ?.toIntOrNull()
            }
            .ifEmpty { listOf(0) }
    }
}

