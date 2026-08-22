package com.hampushallkvist.biometunes.client

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VanillaMusicGateTest {
    @Test
    fun `vanilla gate follows adapter ownership`() {
        VanillaMusicGate.suppressed = false
        VanillaMusicGate.suppressed = true
        assertTrue(VanillaMusicGate.suppressed)
        VanillaMusicGate.suppressed = false
        assertFalse(VanillaMusicGate.suppressed)
    }
}
