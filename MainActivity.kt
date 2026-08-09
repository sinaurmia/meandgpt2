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

class MainActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var sensorPreferences: SensorPreferences
    private lateinit var statistics: Statistics

    private lateinit var txtStatus: TextView

    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView

    private lateinit var txtCPUStats: TextView
    private lateinit var txtGPUStats: TextView
    private lateinit var txtBatteryStats: TextView

    private lateinit var txtSensors: TableLayout

    private lateinit var spRefresh: Spinner


    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable
    private lateinit var btnRecording: Button
    private lateinit var btnMenu: TextView
    private var refreshInterval = 1000L
    private var updating = false

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

        txtStatus =
            findViewById(R.id.txtStatus)

        txtCPU =
            findViewById(R.id.txtCPU)

        txtGPU =
            findViewById(R.id.txtGPU)

        txtBattery =
            findViewById(R.id.txtBattery)

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


        btnRecording =
            findViewById(R.id.btnRecording)

        btnMenu =
            findViewById(R.id.btnMenu)

        styleCards()
        styleRefreshSpinner()
        styleBottomButtons()

        setupRefreshRate()
        setupButtons()

        txtStatus.text =
            "Monitoring stopped"

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

            card.background = drawable

            card.elevation =
                3f *
                        resources.displayMetrics.density
        }
    }
    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun styleRefreshSpinner() {

        val borderColor =
            ContextCompat.getColor(
                this,
                if (isDarkTheme()) {
                    R.color.dark_border
                } else {
                    R.color.light_border
                }
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

        spRefresh.background =
            roundedBorder(
                borderColor,
                backgroundColor,
                1f,
                6f
            )
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

            setTemperatureCard(
                txtCPU,
                txtCPUStats,
                "CPU",
                sample.cpu,
                statistics.cpuMin,
                statistics.cpuMax,
                statistics.cpuAverage
            )

            setTemperatureCard(
                txtGPU,
                txtGPUStats,
                "GPU",
                sample.gpu,
                statistics.gpuMin,
                statistics.gpuMax,
                statistics.gpuAverage
            )

            setTemperatureCard(
                txtBattery,
                txtBatteryStats,
                "Battery",
                sample.battery,
                statistics.batteryMin,
                statistics.batteryMax,
                statistics.batteryAverage
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
        statsView: TextView,
        name: String,
        current: Float?,
        min: Float?,
        max: Float?,
        average: Float?
    ) {

        valueView.text =
            buildCurrentText(
                name,
                current
            )

        statsView.text =
            buildStatsText(
                min,
                max,
                average
            )
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

            val row =
                createSensorRow(
                    "No additional sensors",
                    "--"
                )

            txtSensors.addView(row)

            return
        }

        sensors.entries
            .sortedBy {
                it.key
            }
            .forEach { entry ->

                val row =
                    createSensorRow(
                        entry.key,
                        formatTemperature(
                            entry.value
                        )
                    )

                txtSensors.addView(row)
            }
    }

    private fun createSensorRow(
        sensorName: String,
        temperature: String
    ): TableRow {

        val row =
            TableRow(this)

        row.layoutParams =
            TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    (6 *
                            resources.displayMetrics.density)
                        .toInt()
            }

        val backgroundColor =
            ContextCompat.getColor(
                this,
                if (isDarkTheme()) {
                    R.color.dark_surface
                } else {
                    R.color.light_surface
                }
            )

        val background =
            GradientDrawable()

        background.shape =
            GradientDrawable.RECTANGLE

        background.setColor(
            backgroundColor
        )

        background.cornerRadius =
            10f *
                    resources.displayMetrics.density

        row.background =
            background

        row.elevation =
            2.5f *
                    resources.displayMetrics.density


        val nameView =
            TextView(this)

        nameView.text =
            sensorName

        nameView.setTextColor(
            textPrimary
        )

        nameView.textSize =
            14f

        nameView.setTypeface(
            null,
            Typeface.BOLD
        )

        nameView.gravity =
            Gravity.CENTER_VERTICAL

        nameView.setPadding(
            12,
            8,
            8,
            8
        )


        val valueView =
            TextView(this)

        valueView.text =
            if (temperature == "--") {
                temperature
            } else {
                "$temperature °C"
            }

        valueView.setTextColor(
            blue
        )

        valueView.textSize =
            14f

        valueView.setTypeface(
            null,
            Typeface.BOLD
        )

        valueView.gravity =
            Gravity.CENTER_VERTICAL or
                    Gravity.END

        valueView.setPadding(
            8,
            8,
            12,
            8
        )


        nameView.layoutParams =
            TableRow.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

        valueView.layoutParams =
            TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        row.addView(nameView)
        row.addView(valueView)

        return row
    }

    private fun clearSensorTable() {

        txtSensors.removeAllViews()

        txtSensors.addView(
            createSensorRow(
                "No additional sensors",
                "--"
            )
        )
    }

    private fun startUpdater() {

        if (updating)
            return

        updating = true

        txtStatus.text =
            "Monitoring..."

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

        txtStatus.text =
            "Monitoring stopped"
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