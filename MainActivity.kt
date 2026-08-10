package com.example.meandgpt2

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var sensorPreferences: SensorPreferences
    private lateinit var statistics: Statistics
    private lateinit var cpuTemperatureChart: CpuTemperatureChartView
    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView

    private lateinit var txtCPUStats: LinearLayout
    private lateinit var txtGPUStats: LinearLayout
    private lateinit var txtBatteryStats: LinearLayout

    private lateinit var txtSensors: TableLayout

    private lateinit var spRefresh: Spinner


    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable
    private lateinit var btnRecording: Button
    private lateinit var btnMenu: TextView
    private var refreshInterval = 1000L
    private var updating = false
    private lateinit var txtCPUMax: TextView
    private lateinit var txtCPUMin: TextView
    private lateinit var txtCPUAvg: TextView

    private lateinit var txtGPUMax: TextView
    private lateinit var txtGPUMin: TextView
    private lateinit var txtGPUAvg: TextView

    private lateinit var txtBatteryMax: TextView
    private lateinit var txtBatteryMin: TextView
    private lateinit var txtBatteryAvg: TextView

    private val blue: Int
        get() = ContextCompat.getColor(
            this,
            R.color.primary_blue
        )

    private val red: Int
        get() = ContextCompat.getColor(
            this,
            R.color.temperature_max
        )

    private val textPrimary: Int
        get() {
            val typedValue = android.util.TypedValue()

            theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                typedValue,
                true
            )

            return if (typedValue.resourceId != 0) {
                ContextCompat.getColorStateList(
                    this,
                    typedValue.resourceId
                )?.defaultColor ?: typedValue.data
            } else {
                typedValue.data
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())

        updateRunnable = object : Runnable {
            override fun run() {
                if (!updating)
                    return

                readTemperatures()

                handler.postDelayed(
                    this,
                    refreshInterval
                )
            }
        }

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        sensorPreferences =
            SensorPreferences(this)

        thermalReader =
            ThermalReader(this)

        statistics =
            Statistics()


        txtCPU =
            findViewById(R.id.txtCPU)

        txtGPU =
            findViewById(R.id.txtGPU)

        txtBattery =
            findViewById(R.id.txtBattery)
        txtCPUMax = findViewById(R.id.txtCPUMax)
        txtCPUMin = findViewById(R.id.txtCPUMin)
        txtCPUAvg = findViewById(R.id.txtCPUAvg)

        txtGPUMax = findViewById(R.id.txtGPUMax)
        txtGPUMin = findViewById(R.id.txtGPUMin)
        txtGPUAvg = findViewById(R.id.txtGPUAvg)

        txtBatteryMax = findViewById(R.id.txtBatteryMax)
        txtBatteryMin = findViewById(R.id.txtBatteryMin)
        txtBatteryAvg = findViewById(R.id.txtBatteryAvg)

        txtCPUStats =
            findViewById(R.id.txtCPUStats)

        txtGPUStats =
            findViewById(R.id.txtGPUStats)

        txtBatteryStats =
            findViewById(R.id.txtBatteryStats)

        txtSensors =
            findViewById(R.id.txtSensors)

        spRefresh =
            findViewById(R.id.spRefresh)

        cpuTemperatureChart =
            findViewById(R.id.cpuTemperatureChart)


        btnRecording =
            findViewById(R.id.btnRecording)

        btnMenu =
            findViewById(R.id.btnMenu)

        styleCards()
        styleBottomButtons()

        setupRefreshRate()
        setupButtons()


        clearSensorTable()
    }

    private fun styleCards() {

        val cards = listOf(
            findViewById<View>(R.id.cardCPU),
            findViewById<View>(R.id.cardGPU),
            findViewById<View>(R.id.cardBattery)
        )

        val backgroundColor =
            ContextCompat.getColor(
                this,
                if (isDarkTheme()) {
                    R.color.dark_surface
                } else {
                    R.color.light_surface
                }
            )

        val darkBorderColor =
            ContextCompat.getColor(
                this,
                R.color.dark_border
            )

        cards.forEach { card ->

            val drawable = GradientDrawable()

            drawable.shape =
                GradientDrawable.RECTANGLE

            drawable.setColor(
                backgroundColor
            )

            drawable.cornerRadius =
                12f *
                        resources.displayMetrics.density

            if (isDarkTheme()) {
                drawable.setStroke(
                    (
                            1f *
                                    resources.displayMetrics.density
                            )
                        .toInt()
                        .coerceAtLeast(1),
                    darkBorderColor
                )
            }

            card.background = drawable

            card.elevation =
                4f *
                        resources.displayMetrics.density
        }
    }
    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }


    private fun styleBottomButtons() {

        val secondaryTextColor =
            ContextCompat.getColor(
                this,
                if (isDarkTheme()) {
                    R.color.dark_secondary_text
                } else {
                    R.color.light_secondary_text
                }
            )

        btnRecording.setTextColor(
            secondaryTextColor
        )

        btnRecording.gravity =
            Gravity.CENTER

        btnRecording.setPadding(
            0,
            4,
            0,
            2
        )

        if (isDarkTheme()) {

            val bottomNavigation =
                findViewById<View>(
                    R.id.bottomNavigation
                )

            val backgroundColor =
                ContextCompat.getColor(
                    this,
                    R.color.dark_background
                )

            val borderColor =
                ContextCompat.getColor(
                    this,
                    R.color.dark_border
                )

            bottomNavigation.background =
                roundedBorder(
                    borderColor,
                    backgroundColor,
                    1f,
                    14f
                )

            bottomNavigation.elevation =
                6f *
                        resources.displayMetrics.density
        }
    }

    private fun roundedBorder(
        strokeColor: Int,
        fillColor: Int,
        strokeWidthDp: Float,
        radiusDp: Float
    ): GradientDrawable {

        val drawable =
            GradientDrawable()

        drawable.shape =
            GradientDrawable.RECTANGLE

        drawable.setColor(fillColor)

        drawable.cornerRadius =
            radiusDp * resources.displayMetrics.density

        drawable.setStroke(
            (strokeWidthDp *
                    resources.displayMetrics.density)
                .toInt()
                .coerceAtLeast(1),
            strokeColor
        )

        return drawable
    }

    private fun setupRefreshRate() {

        val options =
            listOf(
                "1 Second",
                "5 Seconds",
                "10 Seconds",
                "30 Seconds",
                "1 Minute"
            )

        val values =
            listOf(
                1000L,
                5000L,
                10000L,
                30000L,
                60000L
            )

        spRefresh.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
            )

        refreshInterval =
            sensorPreferences
                .getMonitorRefreshRate()

        values.indexOf(
            refreshInterval
        ).takeIf {
            it >= 0
        }?.let {
            spRefresh.setSelection(it)
        }

        spRefresh.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    refreshInterval =
                        values[position]

                    sensorPreferences
                        .saveMonitorRefreshRate(
                            refreshInterval
                        )

                    if (updating)
                        restartUpdater()

                    Log.d(
                        "MONITOR_RATE",
                        "Rate: $refreshInterval"
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>
                ) {}
            }
    }

    private fun setupButtons() {

        btnRecording.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RecordingActivity::class.java
                )
            )
        }

        btnMenu.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }
    }

    private fun readTemperatures() {

        try {

            val sample =
                thermalReader.readAll()

            statistics.update(sample)
            cpuTemperatureChart.addTemperature(sample.cpu)

            setTemperatureCard(
                txtCPU,
                "CPU",
                sample.cpu,
                statistics.cpuMin,
                statistics.cpuMax,
                statistics.cpuAverage,
                txtCPUMax,
                txtCPUMin,
                txtCPUAvg
            )

            setTemperatureCard(
                txtGPU,
                "GPU",
                sample.gpu,
                statistics.gpuMin,
                statistics.gpuMax,
                statistics.gpuAverage,
                txtGPUMax,
                txtGPUMin,
                txtGPUAvg
            )

            setTemperatureCard(
                txtBattery,
                "Battery",
                sample.battery,
                statistics.batteryMin,
                statistics.batteryMax,
                statistics.batteryAverage,
                txtBatteryMax,
                txtBatteryMin,
                txtBatteryAvg
            )

            val enabled =
                sensorPreferences
                    .getSelectedSensors()

            val extraSensors =
                sample.sensors.filterKeys {
                    enabled.contains(it)
                }

            updateSensorTable(
                extraSensors
            )

        } catch (e: Exception) {

            Log.e(
                "MONITOR",
                "Read failed",
                e
            )
        }
    }

    private fun setTemperatureCard(
        valueView: TextView,
        name: String,
        current: Float?,
        min: Float?,
        max: Float?,
        average: Float?,
        maxView: TextView,
        minView: TextView,
        avgView: TextView
    ) {
        valueView.text =
            buildCurrentText(
                name,
                current
            )

        maxView.text =
            "${max?.let { formatTemperature(it) } ?: "--"} °C"

        minView.text =
            "${min?.let { formatTemperature(it) } ?: "--"} °C"

        avgView.text =
            "${average?.let { formatTemperature(it) } ?: "--"} °C"
    }

    private fun buildCurrentText(
        name: String,
        current: Float?
    ): SpannableString {

        val temperature =
            current?.let {
                formatTemperature(it)
            } ?: "--"

        val unit = " °C"

        val text =
            "$temperature$unit\n$name"

        val result =
            SpannableString(text)

        val numberEnd =
            temperature.length

        val unitEnd =
            numberEnd + unit.length


        result.setSpan(
            AbsoluteSizeSpan(
                34,
                true
            ),
            0,
            numberEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            0,
            numberEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        result.setSpan(
            AbsoluteSizeSpan(
                13,
                true
            ),
            numberEnd,
            unitEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        result.setSpan(
            ForegroundColorSpan(
                textPrimary
            ),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        result.setSpan(
            AbsoluteSizeSpan(
                15,
                true
            ),
            unitEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            unitEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }

    private fun buildStatsText(
        min: Float?,
        max: Float?,
        average: Float?
    ): SpannableStringBuilder {

        val maxValue =
            max?.let {
                formatTemperature(it)
            } ?: "--"

        val minValue =
            min?.let {
                formatTemperature(it)
            } ?: "--"

        val avgValue =
            average?.let {
                formatTemperature(it)
            } ?: "--"

        val result =
            SpannableStringBuilder()

        appendStatLine(
            result,
            "MAX",
            maxValue,
            red
        )

        appendStatLine(
            result,
            "MIN",
            minValue,
            blue
        )

        appendStatLine(
            result,
            "AVG",
            avgValue,
            textPrimary
        )

        return result
    }

    private fun appendStatLine(
        builder: SpannableStringBuilder,
        label: String,
        value: String,
        labelColor: Int
    ) {

        val start =
            builder.length

        builder.append(
            "$label   $value"
        )

        val labelEnd =
            start + label.length

        builder.setSpan(
            ForegroundColorSpan(
                labelColor
            ),
            start,
            labelEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            start,
            labelEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            labelEnd + 3,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append("\n")
    }

    private fun updateSensorTable(
        sensors: Map<String, Float>
    ) {
        txtSensors.removeAllViews()

        if (sensors.isEmpty()) {
            val emptyText = TextView(this)

            emptyText.text = "No additional sensors"
            emptyText.textSize = 13f
            emptyText.setTextColor(textPrimary)
            emptyText.gravity = Gravity.CENTER

            val density =
                resources.displayMetrics.density

            emptyText.layoutParams =
                TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (40 * density).toInt()
                ).apply {
                    topMargin = (4 * density).toInt()
                    bottomMargin = (4 * density).toInt()
                }

            txtSensors.addView(emptyText)

            return
        }

        sensors.entries
            .sortedBy { it.key }
            .forEach { entry ->

                txtSensors.addView(
                    createSensorRow(
                        entry.key,
                        formatTemperature(entry.value)
                    )
                )
            }
    }


    private fun createSensorRow(
        sensorName: String,
        temperature: String
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

        fun dp(value: Int): Int {
            return (value * density).toInt()
        }

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.layoutParams =
            TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
                leftMargin = dp(0)
                rightMargin = dp(0)
            }

        row.setPadding(
            dp(10),
            0,
            dp(10),
            0
        )

        val sensorBackground =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_temperature_card
            )?.mutate()

        if (isDarkTheme() && sensorBackground is GradientDrawable) {
            sensorBackground.setStroke(
                (
                        1f * resources.displayMetrics.density
                        ).toInt().coerceAtLeast(1),
                ContextCompat.getColor(
                    this,
                    R.color.dark_border
                )
            )
        }

        row.background = sensorBackground

        row.elevation =
            dp(3).toFloat()


        // Thermometer icon
        val iconView =
            android.widget.ImageView(this)

        iconView.setImageResource(
            R.drawable.ic_thermometer
        )

        iconView.setColorFilter(
            blue
        )

        iconView.layoutParams =
            LinearLayout.LayoutParams(
                dp(22),
                dp(22)
            ).apply {
                marginEnd = dp(8)
            }


        // Sensor name
        val nameView =
            TextView(this)

        nameView.text =
            sensorName

        nameView.setTextColor(
            textPrimary
        )

        nameView.textSize =
            13f

        nameView.setTypeface(
            null,
            Typeface.BOLD
        )

        nameView.gravity =
            Gravity.CENTER_VERTICAL

        nameView.maxLines =
            1

        nameView.ellipsize =
            android.text.TextUtils.TruncateAt.END

        nameView.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                marginEnd = dp(8)
            }


        // Temperature
        val valueView =
            TextView(this)

        valueView.text =
            if (temperature == "--") {
                "--"
            } else {
                "$temperature °C"
            }

        valueView.setTextColor(
            blue
        )

        valueView.textSize =
            13f

        valueView.setTypeface(
            null,
            Typeface.BOLD
        )

        valueView.gravity =
            Gravity.CENTER_VERTICAL or
                    Gravity.END

        valueView.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )


        row.addView(
            iconView
        )

        row.addView(
            nameView
        )

        row.addView(
            valueView
        )

        return row
    }


    private fun clearSensorTable() {
        txtSensors.removeAllViews()

        val emptyText =
            TextView(this)

        emptyText.text =
            "No additional sensors"

        emptyText.textSize =
            13f

        emptyText.setTextColor(
            textPrimary
        )

        emptyText.gravity =
            Gravity.CENTER

        val density =
            resources.displayMetrics.density

        emptyText.layoutParams =
            TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (40 * density).toInt()
            ).apply {
                topMargin = (4 * density).toInt()
                bottomMargin = (4 * density).toInt()
            }

        txtSensors.addView(
            emptyText
        )
    }
    private fun startUpdater() {

        if (updating)
            return

        updating = true


        readTemperatures()

        handler.removeCallbacks(
            updateRunnable
        )

        handler.postDelayed(
            updateRunnable,
            refreshInterval
        )
    }

    private fun stopUpdater() {

        updating = false

        handler.removeCallbacks(
            updateRunnable
        )

    }

    private fun restartUpdater() {

        handler.removeCallbacks(
            updateRunnable
        )

        if (!updating)
            return

        readTemperatures()

        handler.postDelayed(
            updateRunnable,
            refreshInterval
        )
    }

    override fun onResume() {

        super.onResume()

        thermalReader.refreshSensorCache()

        refreshInterval =
            sensorPreferences
                .getMonitorRefreshRate()

        startUpdater()
    }

    override fun onPause() {

        stopUpdater()

        super.onPause()
    }

    override fun onDestroy() {

        stopUpdater()

        super.onDestroy()
    }

    private fun formatTemperature(
        value: Float
    ): String {

        return "%.1f".format(value)
    }
}