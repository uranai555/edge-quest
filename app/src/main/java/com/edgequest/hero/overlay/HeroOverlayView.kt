package com.edgequest.hero.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * 勇者キャラを描画するオーバーレイView。
 * ドット絵風SD勇者（ユウ）をCanvasに描画。
 * 進化段階に応じて見た目が変化する。
 */
class HeroOverlayView @JvmOverloads constructor(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val onTap: () -> Unit = {},
    private val onDragStart: () -> Unit = {},
    private val onDragEnd: (WindowManager.LayoutParams) -> Unit = {}
) : View(context) {

    // カラーパレット（設計書準拠）
    companion object {
        private val STAGE1_BODY = Color.parseColor("#35D39D")  // エメラルド（服）
        private val STAGE1_SKIN = Color.parseColor("#FFE0BD")  // 肌
        private val STAGE2_BODY = Color.parseColor("#2EBA8D")
        private val STAGE2_MANTLE = Color.parseColor("#4A90D9") // 青マント
        private val STAGE3_BODY = Color.parseColor("#2EBA8D")
        private val STAGE3_MANTLE = Color.parseColor("#E04040") // 赤マント
        private val STAGE3_GOLD = Color.parseColor("#F4C95D")   // 金
        private val SWORD = Color.parseColor("#C0C0C0")
        private val SWORD_HILT = Color.parseColor("#8B4513")
        private val EYE_WHITE = Color.WHITE
        private val PUPIL = Color.parseColor("#333333")
        private val OUTLINE = Color.parseColor("#1A1A2E")
        private val SHIELD_COLOR = Color.parseColor("#C0C0C0")
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OUTLINE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = STAGE1_SKIN
        style = Paint.Style.FILL
    }
    private val swordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SWORD; style = Paint.Style.FILL }
    private val swordHiltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SWORD_HILT; style = Paint.Style.FILL }
    private val mantlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = STAGE3_GOLD; style = Paint.Style.FILL }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHIELD_COLOR; style = Paint.Style.FILL }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EYE_WHITE; style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PUPIL; style = Paint.Style.FILL }
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E04040"); style = Paint.Style.FILL }
    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB3B3")
        style = Paint.Style.FILL
        alpha = 80
    }

    private val density = context.resources.displayMetrics.density
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var bounceOffset = 0f
    private var blinkAlpha = 0f
    private var isBlinking = false

    // 表情: 0=normal, 1=proud, 2=panic
    var expression: Int = 0

    var evolutionStage: Int = 1
        set(value) {
            field = value.coerceIn(1, 3)
            invalidate()
        }

    var heroSizeDp: Int = 48
        set(value) {
            field = value.coerceIn(24, 64)
            requestLayout()
            invalidate()
        }

    private val idleAnim: ValueAnimator
    private val mainHandler = Handler(Looper.getMainLooper())
    private val blinkRunnable = object : Runnable {
        override fun run() {
            isBlinking = true
            blinkAlpha = 1f
            invalidate()
            mainHandler.postDelayed({
                isBlinking = false
                blinkAlpha = 0f
                invalidate()
            }, 150L)
            mainHandler.postDelayed(this, (3000L + Math.random() * 3000).toLong())
        }
    }

    init {
        idleAnim = ValueAnimator.ofFloat(-1f, 1f).apply {
            duration = 2000L
            interpolator = DecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                bounceOffset = (it.animatedValue as Float) * 0.5f
                invalidate()
            }
        }
        idleAnim.start()
        mainHandler.postDelayed(blinkRunnable, 2000L)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val sizePx = (heroSizeDp * density).toInt()
        setMeasuredDimension(sizePx, (sizePx * 1.3f).toInt())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        idleAnim.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        val s = w.coerceAtMost(h) // scale base
        val cx = w / 2f
        val cy = h / 2f + bounceOffset * density

        // === 体（服） ===
        bodyPaint.color = when (evolutionStage) {
            3 -> STAGE3_BODY
            2 -> STAGE2_BODY
            else -> STAGE1_BODY
        }

        // 楕円の体
        val bodyRect = RectF(
            cx - s * 0.3f,
            cy - s * 0.1f,
            cx + s * 0.3f,
            cy + s * 0.35f
        )
        canvas.drawOval(bodyRect, bodyPaint)
        canvas.drawOval(bodyRect, outlinePaint)

        // 頭（丸）
        val headRadius = s * 0.25f
        canvas.drawCircle(cx, cy - s * 0.2f, headRadius, skinPaint)
        canvas.drawCircle(cx, cy - s * 0.2f, headRadius, outlinePaint)

        // === 目 ===
        val blinkOffset = if (isBlinking) 0f else 1f
        val eyeY = cy - s * 0.25f
        val eyeSpacing = s * 0.12f

        // 白目
        canvas.drawCircle(cx - eyeSpacing, eyeY, s * 0.06f, eyePaint)
        canvas.drawCircle(cx + eyeSpacing, eyeY, s * 0.06f, eyePaint)

        if (!isBlinking) {
            // 瞳孔（表情で変化）
            pupilPaint.color = when (expression) {
                2 -> Color.parseColor("#FF0000") // panic: 赤目
                else -> PUPIL
            }
            val pupilR = s * 0.03f
            canvas.drawCircle(cx - eyeSpacing, eyeY, pupilR, pupilPaint)
            canvas.drawCircle(cx + eyeSpacing, eyeY, pupilR, pupilPaint)

            // 口（表情で変化）
            val mouthY = cy - s * 0.12f
            outlinePaint.strokeWidth = 1.8f
            when (expression) {
                1 -> { // proud: ニヤリ
                    val mouthRect = RectF(cx - s * 0.1f, mouthY, cx + s * 0.1f, mouthY + s * 0.08f)
                    canvas.drawArc(mouthRect, 0f, -180f, false, outlinePaint)
                }
                2 -> { // panic: おおきく開ける
                    val mouthRect = RectF(cx - s * 0.08f, mouthY, cx + s * 0.08f, mouthY + s * 0.1f)
                    canvas.drawOval(mouthRect, redPaint)
                }
                else -> { // normal: にっこり
                    val mouthRect = RectF(cx - s * 0.08f, mouthY, cx + s * 0.08f, mouthY + s * 0.06f)
                    canvas.drawArc(mouthRect, 0f, 180f, false, outlinePaint)
                }
            }
            outlinePaint.strokeWidth = 2f
        }

        // ほっぺ
        canvas.drawCircle(cx - s * 0.18f, cy - s * 0.15f, s * 0.04f, blushPaint)
        canvas.drawCircle(cx + s * 0.18f, cy - s * 0.15f, s * 0.04f, blushPaint)

        // === マント（進化段階2〜3） ===
        if (evolutionStage >= 2) {
            mantlePaint.color = if (evolutionStage >= 3) STAGE3_MANTLE else STAGE2_MANTLE
            val mantlePath = Path().apply {
                moveTo(cx - s * 0.3f, cy - s * 0.05f)
                lineTo(cx - s * 0.4f, cy + s * 0.4f)
                lineTo(cx + s * 0.4f, cy + s * 0.4f)
                lineTo(cx + s * 0.3f, cy - s * 0.05f)
                close()
            }
            canvas.drawPath(mantlePath, mantlePaint)
            canvas.drawPath(mantlePath, outlinePaint)
        }

        // === 額当て（進化段階2〜3） ===
        if (evolutionStage >= 2) {
            val bandY = cy - s * 0.3f
            goldPaint.color = if (evolutionStage >= 3) STAGE3_GOLD else Color.parseColor("#C0C0C0")
            canvas.drawRect(
                cx - s * 0.15f, bandY - s * 0.01f,
                cx + s * 0.15f, bandY + s * 0.02f,
                goldPaint
            )
        }

        // === 剣 ===
        val swordX = cx + s * 0.35f
        val swordTop = cy - s * 0.25f + bounceOffset * density * 0.3f
        val swordBot = cy + s * 0.3f

        // 柄
        canvas.drawRect(
            swordX - s * 0.015f, swordTop + s * 0.2f,
            swordX + s * 0.015f, swordBot,
            swordHiltPaint
        )
        // 鍔
        canvas.drawRect(
            swordX - s * 0.06f, swordTop + s * 0.15f,
            swordX + s * 0.06f, swordTop + s * 0.22f,
            goldPaint
        )
        // 刀身
        canvas.drawRect(
            swordX - s * 0.01f, swordTop,
            swordX + s * 0.01f, swordTop + s * 0.2f,
            swordPaint
        )
        // 刃の先端
        val bladePath = Path().apply {
            moveTo(swordX - s * 0.01f, swordTop)
            lineTo(swordX, swordTop - s * 0.04f)
            lineTo(swordX + s * 0.01f, swordTop)
            close()
        }
        canvas.drawPath(bladePath, swordPaint)

        // === 盾（進化段階3のみ） ===
        if (evolutionStage >= 3) {
            val shieldPath = Path().apply {
                val sx = cx - s * 0.38f
                val sy = cy - s * 0.05f
                moveTo(sx, sy)
                lineTo(sx - s * 0.1f, sy + s * 0.15f)
                lineTo(sx, sy + s * 0.25f)
                lineTo(sx + s * 0.05f, sy + s * 0.15f)
                close()
            }
            canvas.drawPath(shieldPath, shieldPaint)
            canvas.drawPath(shieldPath, outlinePaint)
            // 盾の模様（金の十字）
            val shieldCx = cx - s * 0.36f
            val shieldCy = cy + s * 0.08f
            goldPaint.color = STAGE3_GOLD
            canvas.drawRect(shieldCx - s * 0.025f, shieldCy - s * 0.07f,
                shieldCx + s * 0.025f, shieldCy + s * 0.07f, goldPaint)
            canvas.drawRect(shieldCx - s * 0.05f, shieldCy - s * 0.015f,
                shieldCx + s * 0.05f, shieldCy + s * 0.015f, goldPaint)
        }
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
                    // タップ: 喜ぶ表情
                    expression = 1
                    invalidate()
                    onTap()
                    mainHandler.postDelayed({
                        expression = 0
                        invalidate()
                    }, 800L)
                } else {
                    onDragEnd(params)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 外部からパニック表情を設定（低バッテリー時など）
     */
    fun showPanic() {
        expression = 2
        invalidate()
        mainHandler.postDelayed({
            expression = 0
            invalidate()
        }, 2000L)
    }
}
