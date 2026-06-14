package com.edgequest.hero.overlay

import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

/**
 * 画面端吸着ロジック。
 * 指を離した時、画面の左右どちらかの端にスムーズに吸着させる。
 */
class EdgeSnapHelper(
    private val view: View,
    private val windowManager: WindowManager,
    private val getScreenWidth: () -> Int
) {
    private var animator: ValueAnimator? = null

    /**
     * 現在のparams.xから最も近い画面端に吸着する。
     */
    fun snap(params: WindowManager.LayoutParams) {
        animator?.cancel()

        val screenWidth = getScreenWidth()
        val viewWidth = view.width
        val centerX = params.x + viewWidth / 2f
        val targetX: Int = if (centerX < screenWidth / 2f) 0 else screenWidth - viewWidth

        val startX = params.x
        if (startX == targetX) return

        animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                windowManager.updateViewLayout(view, params)
            }
            doOnEnd {
                params.x = targetX
                windowManager.updateViewLayout(view, params)
            }
            start()
        }
    }

    fun cancelAnimation() {
        animator?.cancel()
        animator = null
    }
}
