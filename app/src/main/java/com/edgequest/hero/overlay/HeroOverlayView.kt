package com.edgequest.hero.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * 勇者キャラを描画するオーバーレイView。
 * プレースホルダーとして緑色の円形を表示。
 * ドラッグ移動・タップイベントを処理する。
 */
class HeroOverlayView @JvmOverloads constructor(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val onTap: () -> Unit = {},
    private val onDragStart: () -> Unit = {},
    private val onDragEnd: (WindowManager.LayoutParams) -> Unit = {}
) : View(context) {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // 緑色
        style = Paint.Style.FILL
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }
    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // 進化段階による色
    private val stageColors = mapOf(
        1 to Color.parseColor("#81C784"), // 薄緑（見習い）
        2 to Color.parseColor("#4CAF50"), // 緑（小さな勇者）
        3 to Color.parseColor("#FFD700")  // 金緑（画面端の英雄）
    )
    private val stageBodyColors = mapOf(
        1 to Color.parseColor("#A5D6A7"),
        2 to Color.parseColor("#66BB6A"),
        3 to Color.parseColor("#FFD54F")
    )

    var evolutionStage: Int = 1
        set(value) {
            field = value.coerceIn(1, 3)
            bodyPaint.color = stageBodyColors[field] ?: Color.parseColor("#A5D6A7")
            invalidate()
        }

    var heroSizeDp: Int = 48
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val metrics = context.resources.displayMetrics
    private val density = metrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val sizePx = (heroSizeDp * density).toInt()
        setMeasuredDimension(sizePx, sizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = measuredWidth.coerceAtMost(measuredHeight).toFloat()
        val cx = size / 2f
        val cy = size / 2f
        val radius = size * 0.4f

        // マント（進化段階3のみ）
        if (evolutionStage >= 3) {
            val mantlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8B4513")
                style = Paint.Style.FILL
            }
            val mantlePath = android.graphics.Path().apply {
                moveTo(cx - radius * 0.6f, cy)
                lineTo(cx - radius * 0.8f, cy + radius * 0.8f)
                lineTo(cx + radius * 0.8f, cy + radius * 0.8f)
                lineTo(cx + radius * 0.6f, cy)
                close()
            }
            canvas.drawPath(mantlePath, mantlePaint)
        }

        // 体（丸）
        canvas.drawCircle(cx, cy, radius, bodyPaint)

        // 目
        val eyeRadius = radius * 0.15f
        val eyeY = cy - radius * 0.15f
        canvas.drawCircle(cx - radius * 0.25f, eyeY, eyeRadius, eyePaint)
        canvas.drawCircle(cx + radius * 0.25f, eyeY, eyeRadius, eyePaint)

        // 瞳孔
        val pupilRadius = eyeRadius * 0.5f
        canvas.drawCircle(cx - radius * 0.25f, eyeY, pupilRadius, pupilPaint)
        canvas.drawCircle(cx + radius * 0.25f, eyeY, pupilRadius, pupilPaint)

        // 口（笑顔）
        val mouthRect = RectF(
            cx - radius * 0.25f,
            cy + radius * 0.1f,
            cx + radius * 0.25f,
            cy + radius * 0.35f
        )
        canvas.drawArc(mouthRect, 0f, 180f, false, mouthPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dy = y - lastTouchY

                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    isDragging = true
                    onDragStart()
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    windowManager.updateViewLayout(this, params)
                }

                lastTouchX = x
                lastTouchY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // タップ（ドラッグしてない）
                    onTap()
                } else {
                    // ドラッグ終了 → 端吸着
                    onDragEnd(params)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}