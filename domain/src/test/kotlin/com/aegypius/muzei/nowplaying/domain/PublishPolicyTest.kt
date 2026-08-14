package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublishPolicyTest {

    @Test
    fun `publishes on any connection unless asked not to`() {
        assertTrue(PublishPolicy.allows(unmeteredOnly = false, connectionIsUnmetered = false))
        assertTrue(PublishPolicy.allows(unmeteredOnly = false, connectionIsUnmetered = true))
    }

    @Test
    fun `holds back on a metered connection when asked to`() {
        assertFalse(PublishPolicy.allows(unmeteredOnly = true, connectionIsUnmetered = false))
    }

    @Test
    fun `publishes on an unmetered connection when asked to`() {
        assertTrue(PublishPolicy.allows(unmeteredOnly = true, connectionIsUnmetered = true))
    }

    @Test
    fun `an unreadable connection counts as metered`() {
        // getNetworkCapabilities returns nothing when there is no active network, and
        // guessing "unmetered" there would spend data the user asked to protect.
        assertFalse(PublishPolicy.allows(unmeteredOnly = true, connectionIsUnmetered = null))
        assertTrue(PublishPolicy.allows(unmeteredOnly = false, connectionIsUnmetered = null))
    }
}
