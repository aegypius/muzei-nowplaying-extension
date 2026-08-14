package com.aegypius.muzei.nowplaying.domain

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Counts how often work was dispatched, so a test can tell that the publish went
 * through the injected dispatcher rather than running inline on the caller.
 */
internal class CountingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatched = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatched++
        delegate.dispatch(context, block)
    }
}
