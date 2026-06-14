package com.edgequest.hero.overlay

import android.view.animation.AlphaAnimation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator

/**
 * 吹き出しView。白背景+角丸+下向き三角。
 * 3.5秒表示後、フェードアウト。
 */
class SpeechBubbleView(
    context: Context,
    private val windowManager: WindowManager,
    private val heroParams: WindowManager.LayoutParams,
    private val heroSizePx: Int
) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDDDDD")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 42f // ~14sp
        isFakeBoldText = false
    }
    private val tailPath = Path()
    private val displayMetrics = context.resources.displayMetrics
    private var bubbleWidth = 0
    private var bubbleHeight = 0
    private val paddingH = 24f
    private val paddingV = 20f
    private val cornerRadius = 16f
    private val tailHeight = 14f
    private val tailWidth = 20f
    private var displayText = ""
    private var params: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    /**
     * 台詞を表示する。3.5秒後に自動消去。
     */
    fun showBubble(text: String) {
        displayText = text

        // テキスト幅を計測して吹き出しサイズを決定
        textPaint.textSize = 42f * displayMetrics.density / 3f
        val textWidth = textPaint.measureText(text)
        val maxWidth = (displayMetrics.widthPixels * 0.5f).toInt()
        val wrappedWidth = textWidth.coerceAtMost(maxWidth.toFloat()).toInt()
        bubbleWidth = wrappedWidth + (paddingH * 2).toInt()

        // テキストが1行に収まるかチェック
        val lines = if (textWidth > maxWidth) {
            wrapText(text, maxWidth)
        } else {
            listOf(text)
        }

        val lineHeight = textPaint.textSize * 1.4f
        bubbleHeight = (lines.size * lineHeight + paddingV * 2 + tailHeight).toInt()

        // LayoutParamsを設定
        params = WindowManager.LayoutParams(
            bubbleWidth,
            bubbleHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            // 勇者の真上に配置
            x = heroParams.x.coerceAtLeast(0)
            y = (heroParams.y - bubbleHeight).coerceAtLeast(0)
        }

        windowManager.addView(this@SpeechBubbleView, params)

        // 3.5秒後にフェードアウトして削除
        hideRunnable?.let { mainHandler.removeCallbacks(it) }
        hideRunnable = Runnable { fadeOutAndRemove() }
        mainHandler.postDelayed(hideRunnable!!, 3500L)
    }

    private fun wrapText(text: String, maxWidthPx: Int): List<String> {
        textPaint.textSize = 42f * displayMetrics.density / 3f
        val words = text.toList()
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (char in words) {
            val testLine = currentLine.toString() + char
            if (textPaint.measureText(testLine) > maxWidthPx && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(char.toString())
            } else {
                currentLine.append(char)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun fadeOutAndRemove() {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 300
            fadeOut.interpolator = DecelerateInterpolator()
            fadeOut.fillAfter = true
            startAnimation(fadeOut)
            mainHandler.postDelayed({
                try { windowManager.removeView(this@SpeechBubbleView) } catch (_: Exception) {}
            }, 350L)
        }

    /**
     * 吹き出しを即座に非表示にする（新しい台詞表示前など）。
     */
    fun dismiss() {
        hideRunnable?.let { mainHandler.removeCallbacks(it) }
        try { windowManager.removeView(this@SpeechBubbleView) } catch (_: Exception) {}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = (height - tailHeight).toFloat()

        // 角丸長方形（吹き出し本体）
        val rect = RectF(0f, 0f, w, h)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 下向き三角（しっぽ）
        tailPath.reset()
        val cx = w / 2f
        tailPath.moveTo(cx - tailWidth / 2f, h)
        tailPath.lineTo(cx, h + tailHeight)
        tailPath.lineTo(cx + tailWidth / 2f, h)
        tailPath.close()
        canvas.drawPath(tailPath, bgPaint)
        canvas.drawPath(tailPath, borderPaint)

        // テキスト
        textPaint.textSize = 42f * displayMetrics.density / 3f
        val lines = wrapText(displayText, width - (paddingH * 2).toInt())
        val lineHeight = textPaint.textSize * 1.4f
        var y = paddingV + textPaint.textSize
        for (line in lines) {
            canvas.drawText(line, paddingH, y, textPaint)
            y += lineHeight
        }
    }
}