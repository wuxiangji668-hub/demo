package com.aispeech.widget.base

import android.view.Choreographer
import java.util.*

/**
 * 刷新view控制器，用户控制刷新view的频率，降低帧率
 */
class RefreshViewController(val skipFrames: Int = 0) {
    private val viewRefreshCallbacks = ArrayList<ViewRefreshCallback>()
    private val callbackDispatcher: AnimationCallbackDispatcher = AnimationCallbackDispatcher()
    private val provider = FrameCallbackProvider(callbackDispatcher)
    private var updateTimes = 0 //用于统计更新次数
    /**
     * Callbacks for invalidating the view
     */
    interface ViewRefreshCallback {
        /**
         * invalidate view
         */
        fun invalidateView()
    }

    /**
     * This class is responsible for interacting with the available frame provider by either
     * registering frame callback or posting runnable, and receiving a callback for when a
     * new frame has arrived. This dispatcher class then notifies all the on-going animations of
     * the new frame, so that they can update animation values as needed.
     */
    inner class AnimationCallbackDispatcher {
        fun dispatchAnimationFrame() {
            doAnimationFrame()
            if (viewRefreshCallbacks.isNotEmpty()) {
                provider.postFrameCallback()
            }
        }
    }

    /**
     * Register to get a callback on the view invalidating.
     */
    fun addViewRefreshCallback(callback: ViewRefreshCallback) {
        if (viewRefreshCallbacks.isEmpty()) {
            if (provider.isReleased() == true) {
                provider.postFrameCallback()
            }
        }
        if (!viewRefreshCallbacks.contains(callback)) {
            viewRefreshCallbacks.add(callback)
        }
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for the view
     * invalidating.
     */
    fun removeViewRefreshCallback(callback: ViewRefreshCallback) {
        viewRefreshCallbacks.remove(callback)
    }

    fun release() {
        viewRefreshCallbacks.clear()
        provider.removeFrameCallback()
    }

    fun doAnimationFrame() {
        for (callback in viewRefreshCallbacks) {
            if (updateTimes % (1 + skipFrames) == 0) {
                updateTimes = 0
                callback.invalidateView()
            }
        }
        updateTimes++
        if (viewRefreshCallbacks.isEmpty()) {
            release()
        }
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     */
    private class FrameCallbackProvider internal constructor(dispatcher: AnimationCallbackDispatcher) :
        AnimationFrameCallbackProvider(dispatcher) {
        private val mChoreographer = Choreographer.getInstance()
        private var isReleased = true
        private val mChoreographerCallback: Choreographer.FrameCallback =
            Choreographer.FrameCallback { mDispatcher.dispatchAnimationFrame() }

        override fun postFrameCallback() {
            mChoreographer.postFrameCallback(mChoreographerCallback)
            isReleased = false
        }

        override fun removeFrameCallback() {
            isReleased = true
            mChoreographer.removeFrameCallback(mChoreographerCallback)
        }

        override fun isReleased(): Boolean {
            return isReleased
        }
    }

    /**
     * The intention for having this interface is to increase the testability of ValueAnimator.
     * Specifically, we can have a custom implementation of the interface below and provide
     * timing pulse without using Choreographer. That way we could use any arbitrary interval for
     * our timing pulse in the tests.
     */
    abstract class AnimationFrameCallbackProvider(val mDispatcher: AnimationCallbackDispatcher) {
        abstract fun postFrameCallback()
        abstract fun removeFrameCallback()
        abstract fun isReleased(): Boolean
    }
}