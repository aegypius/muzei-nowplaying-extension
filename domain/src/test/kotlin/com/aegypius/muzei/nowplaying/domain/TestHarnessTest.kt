package com.aegypius.muzei.nowplaying.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the JVM test harness resolves and runs in this module, including the
 * coroutine test dispatcher the publish path will need.
 *
 * Scaffolding only. Real domain tests arrive with ticket 37a746; delete this
 * once they exist.
 */
class TestHarnessTest {

    @Test
    fun `coroutine test harness runs`() = runTest {
        assertEquals(2, listOf(1, 1).sum())
    }
}
