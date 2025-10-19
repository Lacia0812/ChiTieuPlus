package com.example.chitieuplus.ui.stats

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.abs

class PieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // sweep "đích" tính từ dữ liệu
    private var targetIncomeSweep = 0f
    private var targetExpenseSweep = 0f

    // sweep đang hiển thị (được animate tới target)
    private var displayIncomeSweep = 0f
    private var displayExpenseSweep = 0f

    private val paintIncome = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_green_light) // Thu: xanh tươi
        style = Paint.Style.FILL
    }
    private val paintExpense = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_red_light)   // Chi: đỏ
        style = Paint.Style.FILL
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val rect = RectF()
    private var animator: ValueAnimator? = null

    /**
     * Cập nhật dữ liệu. Luôn dùng trị tuyệt đối để vẽ.
     * Thực hiện animation từ sweep hiện tại -> sweep mục tiêu.
     */
    fun setData(income: Long, expense: Long) {
        val inc = abs(income.toDouble())
        val exp = abs(expense.toDouble())
        val total = inc + exp

        if (total <= 0.0) {
            targetIncomeSweep = 0f
            targetExpenseSweep = 0f
        } else {
            targetIncomeSweep = ((inc / total) * 360.0).toFloat()
            targetExpenseSweep = 360f - targetIncomeSweep
        }

        // Hủy animator cũ nếu còn
        animator?.cancel()

        // Animate 500ms về giá trị mới (mượt)
        val startIncome = displayIncomeSweep
        val startExpense = displayExpenseSweep

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500L
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val t = va.animatedValue as Float
                displayIncomeSweep = startIncome + (targetIncomeSweep - startIncome) * t
                displayExpenseSweep = startExpense + (targetExpenseSweep - startExpense) * t
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = width.coerceAtMost(height).toFloat()
        val pad = 16f
        val chartBottomOffset = 80f // chừa khoảng trống cho chú thích
        rect.set(pad, pad, size - pad, size - pad - chartBottomOffset)

        if (displayIncomeSweep <= 0f && displayExpenseSweep <= 0f) {
            // Không vẽ gì khi cả hai = 0
            return
        }

        // --- Vẽ biểu đồ ---
        canvas.drawArc(rect, -90f, displayIncomeSweep, true, paintIncome)     // Thu (xanh)
        canvas.drawArc(rect, -90f + displayIncomeSweep, displayExpenseSweep, true, paintExpense) // Chi (đỏ)

        // --- Chú thích ---
        val centerX = width / 2f
        val legendY = rect.bottom + 55f

        val squareSize = 28f
        val textOffsetY = 10f
        val spaceBetweenLegend = 180f

        // ô màu xanh + chữ "Thu"
        val greenLeft = centerX - spaceBetweenLegend / 2 - squareSize
        val greenRight = greenLeft + squareSize
        canvas.drawRect(greenLeft, legendY - squareSize, greenRight, legendY, paintIncome)
        canvas.drawText("Thu", greenRight + 40f, legendY - textOffsetY, paintText)

        // ô màu đỏ + chữ "Chi"
        val redLeft = centerX + spaceBetweenLegend / 2
        val redRight = redLeft + squareSize
        canvas.drawRect(redLeft, legendY - squareSize, redRight, legendY, paintExpense)
        canvas.drawText("Chi", redRight + 40f, legendY - textOffsetY, paintText)
    }
}
