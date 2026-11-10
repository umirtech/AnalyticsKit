package com.umirtech.analyticskit

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsKitTest {

    @Test
    fun capsLongValues() {
        val long = "x".repeat(500)
        val sanitized = AnalyticsKit.sanitizeParams(mapOf("key" to long))
        assertEquals(100, sanitized.getValue("key").length)
    }

    @Test
    fun dropsBlankValuesAndKeys() {
        val sanitized = AnalyticsKit.sanitizeParams(
            mapOf(
                "ok" to "value",
                "blank" to "   ",
                "" to "x",
            ),
        )
        assertEquals(mapOf("ok" to "value"), sanitized)
    }

    @Test
    fun dropsOverlongKeys() {
        val sanitized = AnalyticsKit.sanitizeParams(mapOf("k".repeat(120) to "value"))
        assertEquals(0, sanitized.size)
    }

    @Test
    fun keepsNumericParamsWithValidKeys() {
        val sanitized = AnalyticsKit.sanitizeNumericParams(
            mapOf(
                "duration_ms" to 12_345L,
                "" to 1L,
                "k".repeat(120) to 2L,
            ),
        )
        assertEquals(mapOf("duration_ms" to 12_345L), sanitized)
    }
}
