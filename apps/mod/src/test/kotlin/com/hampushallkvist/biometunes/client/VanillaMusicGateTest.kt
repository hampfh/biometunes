package com.hampushallkvist.biometunes.client

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VanillaMusicGateTest {
    @Test
    fun `vanilla gate requires active gameplay even with stale ownership`() {
        // Catches a mixin gate that cancels credits music before the end-of-tick lifecycle cleanup.
        VanillaMusicGate.suppressed = true
        try {
            assertTrue(
                VanillaMusicGate.shouldSuppress(
                    hasLevel = true,
                    hasPlayer = true,
                    isEndCredits = false,
                ),
            )
            assertFalse(
                VanillaMusicGate.shouldSuppress(
                    hasLevel = true,
                    hasPlayer = true,
                    isEndCredits = true,
                ),
            )
            assertFalse(
                VanillaMusicGate.shouldSuppress(
                    hasLevel = false,
                    hasPlayer = false,
                    isEndCredits = false,
                ),
            )
        } finally {
            VanillaMusicGate.suppressed = false
        }
    }
}
