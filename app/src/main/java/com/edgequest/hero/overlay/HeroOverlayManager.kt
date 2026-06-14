package com.edgequest.hero.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView.ScaleType
import com.edgequest.hero.R

/**
 * WindowManager をラップし、勇者オーバーレイViewの表示・非表示・状態管理を行う。
 */
class HeroOverlayManager(
    private val context: Context,
    private val onHeroTap: () -> Unit = {}
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = context.resources.displayMetrics

    private var heroView: HeroOverlayView? = null
    private var minimizeButton: ImageButton? = null
    private var closeButton: ImageButton? = null
    private var container: FrameLayout? = null
    private var edgeSnapHelper: EdgeSnapHelper? = null

    var params: WindowManager.LayoutParams? = null
        private set

    private var isVisible = false
    private var isMinimized = false
    private var heroSizeDp: Int = 48

    /**
     * オーバーレイを表示する。
     */
    fun show(sizeDp: Int, savedX: Int? = null, savedY: Int? = null) {
        if (isVisible) return
        heroSizeDp = sizeDp

        val sizePx = (sizeDp * displayMetrics.density).toInt()

        // コンテナ（最小化ボタンと閉じるボタンを含める）
        container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                sizePx + (32 * displayMetrics.density).toInt(), // ボタン分の余白
                sizePx + (32 * displayMetrics.density).toInt()
            )
        }

        // 閉じるボタン
        closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(android.graphics.Color.parseColor("#AA000000"))
            scaleType = ScaleType.FIT_CENTER
            val buttonSize = (18 * displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                gravity = Gravity.TOP or Gravity.END
            }
            setOnClickListener { hide() }
        }
        container?.addView(closeButton)

        // 最小化ボタン
        minimizeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_zoom)
            setBackgroundColor(android.graphics.Color.parseColor("#AA000000"))
            scaleType = ScaleType.FIT_CENTER
            val buttonSize = (18 * displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            setOnClickListener {
                if (isMinimized) {
                    restoreFromMinimize()
                } else {
                    minimize()
                }
            }
        }
        container?.addView(minimizeButton)

        // 勇者View
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX ?: (displayMetrics.widthPixels - sizePx - (32 * displayMetrics.density).toInt())
            y = savedY ?: (displayMetrics.heightPixels / 3)
        }

        heroView = HeroOverlayView(
            context = context,
            windowManager = windowManager,
            params = params!!,
            onTap = onHeroTap,
            onDragStart = { /* 必要なら */ },
            onDragEnd = { p -> edgeSnapHelper?.snap(p) }
        ).apply {
            heroSizeDp = sizeDp
            val sizeP = (sizeDp * displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizeP, sizeP).apply {
                gravity = Gravity.CENTER
            }
        }

        container?.addView(heroView)

        edgeSnapHelper = EdgeSnapHelper(
            view = heroView!!,
            windowManager = windowManager,
            getScreenWidth = { displayMetrics.widthPixels }
        )

        windowManager.addView(container, params!!)
        isVisible = true
        isMinimized = false
    }

    /**
     * オーバーレイを非表示にする。
     */
    fun hide() {
        if (!isVisible) return
        edgeSnapHelper?.cancelAnimation()
        try {
            container?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
            // 既にremoveされている場合は無視
        }
        container = null
        heroView = null
        minimizeButton = null
        closeButton = null
        edgeSnapHelper = null
        isVisible = false
        isMinimized = false
    }

    /**
     * 最小化（24dpに縮小）
     */
    private fun minimize() {
        if (!isVisible || isMinimized) return
        isMinimized = true
        val smallSize = (24 * displayMetrics.density).toInt()
        heroView?.heroSizeDp = 24
        heroView?.layoutParams?.width = smallSize
        heroView?.layoutParams?.height = smallSize
        heroView?.requestLayout()
        minimizeButton?.setImageResource(android.R.drawable.ic_menu_add)
    }

    /**
     * 最小化から復帰
     */
    private fun restoreFromMinimize() {
        if (!isVisible || !isMinimized) return
        isMinimized = false
        val normalSize = (heroSizeDp * displayMetrics.density).toInt()
        heroView?.heroSizeDp = heroSizeDp
        heroView?.layoutParams?.width = normalSize
        heroView?.layoutParams?.height = normalSize
        heroView?.requestLayout()
        minimizeButton?.setImageResource(android.R.drawable.ic_menu_zoom)
    }

    /**
     * キャラサイズ変更（設定画面から）
     */
    fun updateSize(sizeDp: Int) {
        heroSizeDp = sizeDp
        val sizePx = (sizeDp * displayMetrics.density).toInt()
        heroView?.heroSizeDp = sizeDp
        heroView?.layoutParams?.width = sizePx
        heroView?.layoutParams?.height = sizePx
        heroView?.requestLayout()
    }

    /**
     * 表示状態の更新
     */
    fun isDisplayed(): Boolean = isVisible
    fun isCurrentlyMinimized(): Boolean = isMinimized
    fun getCurrentPosition(): Pair<Int, Int>? {
        params?.let { return Pair(it.x, it.y) }
        return null
    }
}
