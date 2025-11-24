package com.example.chitieuplus.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class ThreeMonthsBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val labels = mutableListOf<String>()
    private val incomes = mutableListOf<Long>()
    private val expenses = mutableListOf<Long>()

    // THU: xanh lá
    private val paintIncome = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    // CHI: đỏ
    private val paintExpense = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF5350")
        style = Paint.Style.FILL
    }

    private val paintAxis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    // Text nhãn trục hoành (tháng)
    private val paintBottomText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // Text nhãn trục tung (số tiền)
    private val paintYAxisText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 24f
        textAlign = Paint.Align.LEFT   // vẽ từ trái sang, tránh bị cắt
    }

    private val numberFormat: NumberFormat =
        NumberFormat.getInstance(Locale("vi", "VN"))

    fun setData(newLabels: List<String>, newIncomes: List<Long>, newExpenses: List<Long>) {
        labels.clear()
        incomes.clear()
        expenses.clear()

        val size = minOf(newLabels.size, newIncomes.size, newExpenses.size)
        if (size == 0) {
            invalidate()
            return
        }

        for (i in 0 until size) {
            labels.add(newLabels[i])
            incomes.add(newIncomes[i])
            expenses.add(newExpenses[i])
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (labels.isEmpty()) {
            canvas.drawText("Chưa có dữ liệu", width / 2f, height / 2f, paintBottomText)
            return
        }

        // Chừa chỗ bên trái cho nhãn trục tung
        val leftPadding = 100f
        val rightPadding = 40f
        val topPadding = 24f
        val bottomPadding = 60f

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        if (chartWidth <= 0 || chartHeight <= 0) return

        // Tìm max thực tế trong 3 tháng
        var maxVal = 0L
        for (i in labels.indices) {
            maxVal = max(maxVal, incomes[i])
            maxVal = max(maxVal, expenses[i])
        }
        if (maxVal <= 0L) {
            canvas.drawText("Chưa có dữ liệu", width / 2f, height / 2f, paintBottomText)
            return
        }

        // Làm tròn max lên mốc đẹp (1,2,5,10 * 10^n)
        val axisMax = niceAxisMax(maxVal)

        val baseY = height - bottomPadding

        // Trục hoành (x)
        canvas.drawLine(
            leftPadding,
            baseY,
            width - rightPadding,
            baseY,
            paintAxis
        )

        // ===== Trục tung + grid + nhãn số tiền =====
        val yAxisSteps = 4 // 0, 1/4, 2/4, 3/4, max
        for (i in 0..yAxisSteps) {
            val ratio = i.toFloat() / yAxisSteps.toFloat()
            val value = (axisMax * ratio).toLong()
            val y = baseY - chartHeight * ratio

            // vạch ngang
            canvas.drawLine(
                leftPadding,
                y,
                width - rightPadding,
                y,
                paintAxis
            )

            // nhãn số tiền ở bên trái, từ dưới lên
            val label = if (value == 0L) "0" else numberFormat.format(value)
            canvas.drawText(
                label,
                8f,            // vẽ từ 8px cách mép trái
                y + 8f,        // hơi thấp hơn line 1 chút
                paintYAxisText
            )
        }

        // ===== Vẽ cột =====
        val groupCount = labels.size
        val groupWidth = chartWidth / groupCount
        val barWidth = groupWidth * 0.28f
        val barSpace = barWidth * 0.3f

        for (i in 0 until groupCount) {
            val centerX = leftPadding + groupWidth * i + groupWidth / 2f

            val incomeVal = incomes[i].toFloat()
            val expenseVal = expenses[i].toFloat()

            val incomeHeight = (incomeVal / axisMax.toFloat()) * chartHeight
            val expenseHeight = (expenseVal / axisMax.toFloat()) * chartHeight

            // THU (xanh lá) bên trái
            val incomeLeft = centerX - barSpace / 2f - barWidth
            val incomeRight = incomeLeft + barWidth
            val incomeTop = baseY - incomeHeight
            canvas.drawRoundRect(
                incomeLeft,
                incomeTop,
                incomeRight,
                baseY,
                12f,
                12f,
                paintIncome
            )

            // CHI (đỏ) bên phải
            val expenseLeft = centerX + barSpace / 2f
            val expenseRight = expenseLeft + barWidth
            val expenseTop = baseY - expenseHeight
            canvas.drawRoundRect(
                expenseLeft,
                expenseTop,
                expenseRight,
                baseY,
                12f,
                12f,
                paintExpense
            )

            // Label tháng dưới trục
            canvas.drawText(labels[i], centerX, height - 20f, paintBottomText)
        }
    }

    /**
     * Tìm mốc trục tung "đẹp":
     *  - lấy 10^n gần nhất
     *  - chuẩn hoá về 1..10
     *  - dùng 1,2,5 hoặc 10 * 10^n
     */
    private fun niceAxisMax(value: Long): Long {
        if (value <= 0L) return 0L
        val d = value.toDouble()
        val magnitude = 10.0.pow(floor(log10(d))).toLong()
        val normalized = d / magnitude.toDouble()

        val niceNormalized = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
        return (niceNormalized * magnitude).toLong()
    }
}
