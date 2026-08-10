package com.example.meandgpt2

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class CpuTemperatureChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Point(
        val time: Long,
        val temperature: Float
    )

    private val points = mutableListOf<Point>()
    private var maxPointsInternal = 60

    var maxPoints: Int
        get() = maxPointsInternal
        set(value) {
            maxPointsInternal = value.coerceAtLeast(1)

            while (points.size > maxPointsInternal) {
                points.removeAt(0)
            }

            invalidate()
        }
    private val density =
        resources.displayMetrics.density

    private fun dp(value: Float): Float =
        value * density

    private val chartLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val gridPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
        }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
            textSize = dp(12f)
        }

    private val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
            textSize = dp(18f)
        }

    private val legendPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
            textSize = dp(16f)
        }

    private val legendLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3f)
            strokeCap = Paint.Cap.ROUND
        }

    private val pointPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
        }

    private val timeFormat =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        )

    private var lineColor = 0
    private var primaryTextColor = 0
    private var secondaryTextColor = 0
    private var gridColor = 0
    private var darkMode = false

    init {
        setLayerType(
            View.LAYER_TYPE_SOFTWARE,
            null
        )

        updateThemeColors()
    }

    private fun updateThemeColors() {

        darkMode =
            (resources.configuration.uiMode and
                    android.content.res.Configuration
                        .UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration
                        .UI_MODE_NIGHT_YES

        lineColor =
            ContextCompat.getColor(
                context,
                if (darkMode) {
                    R.color.primary_blue_dark
                } else {
                    R.color.primary_blue
                }
            )

        primaryTextColor =
            ContextCompat.getColor(
                context,
                if (darkMode) {
                    R.color.dark_text
                } else {
                    R.color.light_text
                }
            )

        secondaryTextColor =
            ContextCompat.getColor(
                context,
                if (darkMode) {
                    R.color.dark_secondary_text
                } else {
                    R.color.light_secondary_text
                }
            )

        gridColor =
            ContextCompat.getColor(
                context,
                if (darkMode) {
                    R.color.dark_border
                } else {
                    R.color.light_border
                }
            )

        chartLinePaint.color =
            lineColor

        legendLinePaint.color =
            lineColor

        titlePaint.color =
            primaryTextColor

        legendPaint.color =
            secondaryTextColor

        textPaint.color =
            secondaryTextColor

        gridPaint.color =
            gridColor

        borderPaint.color =
            if (darkMode) {
                ContextCompat.getColor(
                    context,
                    R.color.dark_border
                )
            } else {
                ContextCompat.getColor(
                    context,
                    R.color.light_border
                )
            }
    }

    fun addTemperature(
        temperature: Float?,
        timestamp: Long =
            System.currentTimeMillis()
    ) {

        if (temperature == null) {
            return
        }

        points.add(
            Point(
                timestamp,
                temperature
            )
        )

        /*
 * Keep only the configured number of samples.
 */
        while (points.size > maxPointsInternal) {
            points.removeAt(0)
        }

        invalidate()
    }

    fun clearData() {
        points.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        updateThemeColors()

        val width =
            width.toFloat()

        val height =
            height.toFloat()

        if (width <= 0f ||
            height <= 0f
        ) {
            return
        }

        /*
         * Internal chart area.
         *
         * Extra right padding keeps the newest
         * point/time label away from the card edge.
         */
        val left =
            dp(42f)

        val right =
            width - dp(22f)

        val top =
            dp(60f)

        val bottom =
            height - dp(40f)

        if (right <= left ||
            bottom <= top
        ) {
            return
        }

        /*
         * Subtle border for dark mode.
         */
        if (darkMode) {
            canvas.drawRoundRect(
                dp(0.5f),
                dp(0.5f),
                width - dp(0.5f),
                height - dp(0.5f),
                dp(6f),
                dp(6f),
                borderPaint
            )
        }

        drawTitle(
            canvas,
            width
        )

        drawLegend(
            canvas,
            width
        )

        val chartTop =
            top

        val chartBottom =
            bottom

        val chartHeight =
            chartBottom - chartTop

        /*
         * CPU temperature range:
         * 20°C -> 100°C
         */
        val maxTemperature =
            100f

        val minTemperature =
            20f

        val gridValues =
            floatArrayOf(
                20f,
                40f,
                60f,
                80f,
                100f
            )

        /*
         * Horizontal grid lines.
         */
        gridValues.forEach { value ->

            val y =
                chartBottom -
                        ((value -
                                minTemperature) /
                                (maxTemperature -
                                        minTemperature)) *
                        chartHeight

            canvas.drawLine(
                left,
                y,
                right,
                y,
                gridPaint
            )

            textPaint.textAlign =
                Paint.Align.RIGHT

            canvas.drawText(
                value.toInt().toString(),
                left - dp(8f),
                y + dp(4f),
                textPaint
            )
        }

        if (points.isEmpty()) {

            drawTimePlaceholder(
                canvas,
                left,
                right,
                chartBottom
            )

            return
        }

        drawChart(
            canvas,
            left,
            right,
            chartTop,
            chartBottom,
            maxTemperature,
            minTemperature
        )

        drawTimeLabels(
            canvas,
            left,
            right,
            chartBottom
        )
    }

    private fun drawTitle(
        canvas: Canvas,
        width: Float
    ) {

        titlePaint.textAlign =
            Paint.Align.LEFT

        canvas.drawText(
            "CPU Temperature (°C)",
            dp(22f),
            dp(30f),
            titlePaint
        )
    }

    private fun drawLegend(
        canvas: Canvas,
        width: Float
    ) {

        val legendText =
            "CPU"

        legendPaint.textAlign =
            Paint.Align.LEFT

        val textWidth =
            legendPaint.measureText(
                legendText
            )

        val textX =
            width -
                    dp(22f) -
                    textWidth

        val lineEnd =
            textX -
                    dp(10f)

        val lineStart =
            lineEnd -
                    dp(18f)

        canvas.drawLine(
            lineStart,
            dp(32f),
            lineEnd,
            dp(32f),
            legendLinePaint
        )

        canvas.drawText(
            legendText,
            textX,
            dp(38f),
            legendPaint
        )
    }

    private fun drawChart(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        maxTemperature: Float,
        minTemperature: Float
    ) {

        /*
 * Show up to the configured number of samples.
 */
        val visiblePoints =
            points.takeLast(maxPointsInternal)

        if (visiblePoints.isEmpty()) {
            return
        }

        val pointCount =
            visiblePoints.size

        val path =
            Path()

        val fillPath =
            Path()

        visiblePoints.forEachIndexed {
                index,
                point ->

            val x =
                if (pointCount == 1) {

                    (left + right) / 2f

                } else {

                    left +
                            (index.toFloat() /
                                    (pointCount -
                                            1).toFloat()) *
                            (right - left)
                }

            val safeTemperature =
                min(
                    max(
                        point.temperature,
                        minTemperature
                    ),
                    maxTemperature
                )

            val y =
                bottom -
                        ((safeTemperature -
                                minTemperature) /
                                (maxTemperature -
                                        minTemperature)) *
                        (bottom - top)

            if (index == 0) {

                path.moveTo(
                    x,
                    y
                )

                fillPath.moveTo(
                    x,
                    bottom
                )

                fillPath.lineTo(
                    x,
                    y
                )

            } else {

                val previous =
                    visiblePoints[
                        index - 1
                    ]

                val previousX =
                    if (pointCount == 1) {

                        x

                    } else {

                        left +
                                ((index - 1).toFloat() /
                                        (pointCount -
                                                1).toFloat()) *
                                (right - left)
                    }

                val previousTemperature =
                    min(
                        max(
                            previous.temperature,
                            minTemperature
                        ),
                        maxTemperature
                    )

                val previousY =
                    bottom -
                            ((previousTemperature -
                                    minTemperature) /
                                    (maxTemperature -
                                            minTemperature)) *
                            (bottom - top)

                val controlX =
                    (previousX + x) / 2f

                path.cubicTo(
                    controlX,
                    previousY,
                    controlX,
                    y,
                    x,
                    y
                )

                fillPath.cubicTo(
                    controlX,
                    previousY,
                    controlX,
                    y,
                    x,
                    y
                )
            }
        }

        val lastPoint =
            visiblePoints.last()

        val lastX =
            if (pointCount == 1) {

                (left + right) / 2f

            } else {

                right
            }

        val lastTemperature =
            min(
                max(
                    lastPoint.temperature,
                    minTemperature
                ),
                maxTemperature
            )

        val lastY =
            bottom -
                    ((lastTemperature -
                            minTemperature) /
                            (maxTemperature -
                                    minTemperature)) *
                    (bottom - top)

        fillPath.lineTo(
            lastX,
            bottom
        )

        fillPath.close()

        fillPaint.shader =
            LinearGradient(
                0f,
                top,
                0f,
                bottom,
                lineColor and 0x55FFFFFF,
                lineColor and 0x00FFFFFF,
                Shader.TileMode.CLAMP
            )

        canvas.drawPath(
            fillPath,
            fillPaint
        )

        canvas.drawPath(
            path,
            chartLinePaint
        )

        /*
         * Latest point.
         */
        pointPaint.color =
            lineColor

        canvas.drawCircle(
            lastX,
            lastY,
            dp(4f),
            pointPaint
        )
    }

    private fun drawTimeLabels(
        canvas: Canvas,
        left: Float,
        right: Float,
        bottom: Float
    ) {

        if (points.isEmpty()) {
            return
        }

        val visiblePoints =
            points.takeLast(maxPointsInternal)

        if (visiblePoints.isEmpty()) {
            return
        }

        textPaint.textAlign =
            Paint.Align.CENTER

        val positions =
            min(
                6,
                visiblePoints.size
            )

        for (i in 0 until positions) {

            val index =
                if (positions == 1) {

                    0

                } else {

                    (
                            (visiblePoints.size - 1) *
                                    i.toFloat() /
                                    (positions - 1)
                                        .toFloat()
                            ).toInt()
                }

            val point =
                visiblePoints[index]

            val x =
                if (positions == 1) {

                    (left + right) / 2f

                } else {

                    left +
                            (index.toFloat() /
                                    (visiblePoints.size -
                                            1).toFloat()) *
                            (right - left)
                }

            canvas.drawText(
                timeFormat.format(
                    Date(point.time)
                ),
                x,
                bottom + dp(25f),
                textPaint
            )
        }
    }

    private fun drawTimePlaceholder(
        canvas: Canvas,
        left: Float,
        right: Float,
        bottom: Float
    ) {

        textPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            "--:--",
            (left + right) / 2f,
            bottom + dp(25f),
            textPaint
        )
    }
}