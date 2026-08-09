package com.example.meandgpt2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import android.widget.LinearLayout

class RecordingActivity : AppCompatActivity() {

    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private lateinit var thermalReader: ThermalReader
    private lateinit var storage: Storage
    private lateinit var recorder: Recorder
    private lateinit var statistics: Statistics
    private lateinit var sensorPreferences: SensorPreferences
    private lateinit var btnMenu: TextView

    private lateinit var spRefresh: Spinner
    private lateinit var txtStatus: TextView
    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView
    private lateinit var txtCPUStats: LinearLayout
    private lateinit var txtGPUStats: LinearLayout
    private lateinit var txtBatteryStats: LinearLayout

    private lateinit var txtCPUMax: TextView
    private lateinit var txtCPUMin: TextView
    private lateinit var txtCPUAvg: TextView

    private lateinit var txtGPUMax: TextView
    private lateinit var txtGPUMin: TextView
    private lateinit var txtGPUAvg: TextView

    private lateinit var txtBatteryMax: TextView
    private lateinit var txtBatteryMin: TextView
    private lateinit var txtBatteryAvg: TextView
    private lateinit var txtCount: TextView
    private lateinit var logTable: TableLayout
    private lateinit var txtTotalRows: TextView

    private lateinit var btnMonitoring: Button
    private lateinit var btnSettings: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnShowLog: Button
    private lateinit var btnExport: Button

    private val handler = Handler(Looper.getMainLooper())

    private var recordingInterval = 5000L
    private var recordingUiActive = false
    private var recordingStartCount = 0

    private val uiRunnable = object : Runnable {
        override fun run() {
            if (!recordingUiActive)
                return

            updateRecordingDisplay()

            handler.postDelayed(
                this,
                recordingInterval
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recording)

        title = "Recording"

        sensorPreferences = SensorPreferences(this)
        thermalReader = ThermalReader(this)
        storage = Storage(this)
        statistics = Statistics()

        recorder = Recorder(
            thermalReader,
            storage,
            statistics
        )

        spRefresh = findViewById(R.id.spRefresh)

        txtStatus = findViewById(R.id.txtStatus)
        txtCPU = findViewById(R.id.txtCPU)
        txtGPU = findViewById(R.id.txtGPU)
        txtBattery = findViewById(R.id.txtBattery)
        txtCPUStats = findViewById(R.id.txtCPUStats)
        txtGPUStats = findViewById(R.id.txtGPUStats)
        txtBatteryStats = findViewById(R.id.txtBatteryStats)

        txtCPUMax = findViewById(R.id.txtCPUMax)
        txtCPUMin = findViewById(R.id.txtCPUMin)
        txtCPUAvg = findViewById(R.id.txtCPUAvg)

        txtGPUMax = findViewById(R.id.txtGPUMax)
        txtGPUMin = findViewById(R.id.txtGPUMin)
        txtGPUAvg = findViewById(R.id.txtGPUAvg)

        txtBatteryMax = findViewById(R.id.txtBatteryMax)
        txtBatteryMin = findViewById(R.id.txtBatteryMin)
        txtBatteryAvg = findViewById(R.id.txtBatteryAvg)

        btnMenu = findViewById(R.id.btnMenu)
        txtCount =
            findViewById(R.id.txtCount)

        txtTotalRows =
            findViewById(R.id.txtTotalRows)

        logTable =
            findViewById(R.id.logTable)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnShowLog = findViewById(R.id.btnShowLog)
        btnExport = findViewById(R.id.btnExport)

        btnMonitoring =
            findViewById(R.id.btnMonitoring)

        btnSettings =
            findViewById(R.id.btnSettings)
        btnStop.isEnabled = false
        btnStart.isEnabled = true

        btnStop.alpha = 0.45f
        btnStart.alpha = 1f
        setupRefreshRate()
        setupButtons()
        clearRecordingDisplay()
    }
    private val recordingTimerRunnable =
        object : Runnable {

            override fun run() {

                if (!recordingUiActive)
                    return

                recordingElapsedTime =
                    android.os.SystemClock.elapsedRealtime() -
                            recordingStartTime

                updateRecordingTime()

                handler.postDelayed(
                    this,
                    1000L
                )
            }
        }

    private fun setupRefreshRate() {
        val options = listOf(
            "1 Second",
            "5 Seconds",
            "10 Seconds",
            "30 Seconds",
            "1 Minute"
        )

        val values = listOf(
            1000L,
            5000L,
            10000L,
            30000L,
            60000L
        )

        spRefresh.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            options
        )

        recordingInterval =
            sensorPreferences.getRefreshRate()

        values.indexOf(recordingInterval)
            .takeIf { it >= 0 }
            ?.let {
                spRefresh.setSelection(it)
            }

        spRefresh.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    recordingInterval = values[position]

                    sensorPreferences.saveRefreshRate(
                        recordingInterval
                    )

                    recorder.setInterval(
                        recordingInterval
                    )

                    if (recordingUiActive) {
                        restartUiUpdater()
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>
                ) {}
            }
    }

    private fun setupButtons() {
        btnStart.setOnClickListener {
            startRecording()
        }

        btnStop.setOnClickListener {
            stopRecording()
        }

        btnReset.setOnClickListener {
            resetRecording()
        }

        btnShowLog.setOnClickListener {
            showLog()
        }

        btnExport.setOnClickListener {
            exportCsv()
        }
        btnMonitoring.setOnClickListener {
            finish()
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

    private fun startRecording() {
        if (recorder.isRunning())
            return

        statistics.reset()

        recordingStartCount =
            storage.count()

        logTable.removeAllViews()

        clearRecordingDisplay()

        recorder.start()
        recordingStartTime =
            android.os.SystemClock.elapsedRealtime()

        recordingElapsedTime = 0L

        updateRecordingTime()

        handler.post(
            recordingTimerRunnable
        )

        recordingUiActive = true

        txtStatus.text = "Recording..."

        btnStart.isEnabled = false
        btnStop.isEnabled = true

        btnStart.alpha = 0.45f
        btnStop.alpha = 1f

        updateRecordingDisplay()

        restartUiUpdater()
    }

    private fun stopRecording() {
        if (!recorder.isRunning())
            return

        recorder.stop()
        recordingElapsedTime =
            android.os.SystemClock.elapsedRealtime() -
                    recordingStartTime

        handler.removeCallbacks(
            recordingTimerRunnable
        )

        updateRecordingTime()
        recordingUiActive = false

        handler.removeCallbacks(
            uiRunnable
        )

        txtStatus.text = "Stopped"

        btnStart.isEnabled = true
        btnStop.isEnabled = false

        btnStart.alpha = 1f
        btnStop.alpha = 0.45f

        clearRecordingDisplay()
    }

    private fun resetRecording() {
        if (recorder.isRunning()) {
            recorder.stop()
        }

        recordingUiActive = false

        handler.removeCallbacks(
            uiRunnable
        )

        storage.clear()
        statistics.reset()

        recordingStartCount = 0

        txtStatus.text = "Stopped"

        logTable.removeAllViews()

        btnStart.isEnabled = true
        btnStop.isEnabled = false

        btnStart.alpha = 1f
        btnStop.alpha = 0.45f
        recordingElapsedTime = 0L

        handler.removeCallbacks(
            recordingTimerRunnable
        )

        updateRecordingTime()
        clearRecordingDisplay()
    }
    private fun updateRecordingTime() {

        val totalSeconds =
            recordingElapsedTime / 1000L

        val hours =
            totalSeconds / 3600L

        val minutes =
            (totalSeconds % 3600L) / 60L

        val seconds =
            totalSeconds % 60L

        val time =
            String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )

        val text =
            "Recording\n$time"

        val result =
            SpannableString(text)

        val blue =
            ContextCompat.getColor(
                this,
                R.color.primary_blue
            )

        result.setSpan(
            ForegroundColorSpan(blue),
            0,
            "Recording".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            android.text.style.StyleSpan(
                android.graphics.Typeface.BOLD
            ),
            0,
            "Recording".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        txtStatus.text = result
    }
    private fun updateRecordingDisplay() {
        if (!recordingUiActive)
            return

        if (!recorder.isRunning())
            return

        val sample = storage.getLast()

        val currentCount = storage.count()
        txtTotalRows.text =
            "Total Rows: $currentCount"

        val sessionCount =
            currentCount - recordingStartCount

        txtCount.text =
            "Samples: ${maxOf(0, sessionCount)}"

        if (sessionCount <= 0 || sample == null) {
            clearTemperatureDisplays()
            return
        }

        txtCPU.text =
            buildCurrentText(
                "CPU",
                sample.cpu
            )

        txtGPU.text =
            buildCurrentText(
                "GPU",
                sample.gpu
            )

        txtBattery.text =
            buildCurrentText(
                "Battery",
                sample.battery
            )

        txtCPUMax.text =
            "${statistics.cpuMax?.let { formatTemperature(it) } ?: "--"} °C"

        txtCPUMin.text =
            "${statistics.cpuMin?.let { formatTemperature(it) } ?: "--"} °C"

        txtCPUAvg.text =
            "${statistics.cpuAverage?.let { formatTemperature(it) } ?: "--"} °C"

        txtGPUMax.text =
            "${statistics.gpuMax?.let { formatTemperature(it) } ?: "--"} °C"

        txtGPUMin.text =
            "${statistics.gpuMin?.let { formatTemperature(it) } ?: "--"} °C"

        txtGPUAvg.text =
            "${statistics.gpuAverage?.let { formatTemperature(it) } ?: "--"} °C"

        txtBatteryMax.text =
            "${statistics.batteryMax?.let { formatTemperature(it) } ?: "--"} °C"

        txtBatteryMin.text =
            "${statistics.batteryMin?.let { formatTemperature(it) } ?: "--"} °C"

        txtBatteryAvg.text =
            "${statistics.batteryAverage?.let { formatTemperature(it) } ?: "--"} °C"

        txtCPUStats.text =
            buildStatsText(
                statistics.cpuMax,
                statistics.cpuMin,
                statistics.cpuAverage
            )

        txtGPUStats.text =
            buildStatsText(
                statistics.gpuMax,
                statistics.gpuMin,
                statistics.gpuAverage
            )

        txtBatteryStats.text =
            buildStatsText(
                statistics.batteryMax,
                statistics.batteryMin,
                statistics.batteryAverage
            )

        txtBattery.text =
            sample.battery?.let {
                "${formatTemperature(it)} °C\nBattery"
            } ?: "--\nBattery"

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
            android.text.style.AbsoluteSizeSpan(
                28,
                true
            ),
            0,
            numberEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            android.text.style.StyleSpan(
                android.graphics.Typeface.BOLD
            ),
            0,
            numberEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            android.text.style.AbsoluteSizeSpan(
                11,
                true
            ),
            numberEnd,
            unitEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            android.text.style.AbsoluteSizeSpan(
                12,
                true
            ),
            unitEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            android.text.style.StyleSpan(
                android.graphics.Typeface.BOLD
            ),
            unitEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }
    private fun buildTemperatureText(
        name: String,
        current: Float?,
        min: Float?,
        max: Float?,
        average: Float?
    ): String {
        return buildString {
            append(name)
            append(": ")
            append(
                current?.let {
                    formatTemperature(it)
                } ?: "--"
            )
            append(" °C\n")

            append("Min: ")
            append(
                min?.let {
                    formatTemperature(it)
                } ?: "--"
            )
            append(" °C\n")

            append("Max: ")
            append(
                max?.let {
                    formatTemperature(it)
                } ?: "--"
            )
            append(" °C\n")

            append("Average: ")
            append(
                average?.let {
                    formatTemperature(it)
                } ?: "--"
            )
            append(" °C")
        }
    }

    private fun clearRecordingDisplay() {
        clearTemperatureDisplays()

        txtCount.text =
            "Samples: --"
        txtTotalRows.text =
            "Total Rows: ${storage.count()}"
    }

    private fun clearTemperatureDisplays() {

        txtCPU.text = buildCurrentText(
            "CPU",
            null
        )

        txtGPU.text = buildCurrentText(
            "GPU",
            null
        )

        txtBattery.text = buildCurrentText(
            "Battery",
            null
        )

        txtCPUMax.text = "-- °C"
        txtCPUMin.text = "-- °C"
        txtCPUAvg.text = "-- °C"

        txtGPUMax.text = "-- °C"
        txtGPUMin.text = "-- °C"
        txtGPUAvg.text = "-- °C"

        txtBatteryMax.text = "-- °C"
        txtBatteryMin.text = "-- °C"
        txtBatteryAvg.text = "-- °C"
    }

    private fun restartUiUpdater() {
        handler.removeCallbacks(
            uiRunnable
        )

        if (!recordingUiActive)
            return

        updateRecordingDisplay()

        handler.postDelayed(
            uiRunnable,
            recordingInterval
        )
    }

    private fun showLog() {
        logTable.removeAllViews()

        val logs =
            storage.getLastRecords(500)

        val sensors =
            sensorPreferences
                .getEnabledSensors()
                .sorted()

        addTableHeader(sensors)

        if (logs.isEmpty()) {
            addEmptyRow()
        } else {
            logs.reversed().forEach { sample ->
                addTableRow(
                    sample,
                    sensors
                )
            }
        }

        txtCount.text =
            "Samples: ${
                maxOf(
                    0,
                    storage.count() -
                            recordingStartCount
                )
            }"

        txtTotalRows.text =
            "Total Rows: ${storage.count()}"
    }

    private fun addTableHeader(
        sensors: List<String>
    ) {
        val row =
            TableRow(this)

        addCell(
            row,
            "Date",
            true
        )

        addCell(
            row,
            "CPU",
            true
        )

        addCell(
            row,
            "GPU",
            true
        )

        addCell(
            row,
            "Battery",
            true
        )

        for (sensor in sensors) {
            addCell(
                row,
                sensor,
                true
            )
        }

        logTable.addView(row)
    }
    private fun addTableRow(
        sample: Sample,
        sensors: List<String>
    ) {
        val row =
            TableRow(this)

        addCell(
            row,
            sample.date
        )

        addCell(
            row,
            sample.cpu?.let {
                formatTemperature(it)
            } ?: ""
        )

        addCell(
            row,
            sample.gpu?.let {
                formatTemperature(it)
            } ?: ""
        )

        addCell(
            row,
            sample.battery?.let {
                formatTemperature(it)
            } ?: ""
        )

        for (sensor in sensors) {
            val value =
                sample.sensors[sensor]

            addCell(
                row,
                value?.let {
                    formatTemperature(it)
                } ?: ""
            )
        }

        logTable.addView(row)
    }
    private fun addCell(
        row: TableRow,
        text: String,
        header: Boolean = false
    ) {
        val cell =
            TextView(this)

        cell.text = text
        cell.gravity = Gravity.CENTER
        cell.setPadding(
            16,
            10,
            16,
            10
        )

        cell.textSize =
            if (header) 13f else 12f

        if (header) {
            cell.setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }

        cell.layoutParams =
            TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        row.addView(cell)
    }
    private fun addEmptyRow() {
        val row =
            TableRow(this)

        addCell(
            row,
            "No records"
        )

        logTable.addView(row)
    }
    private fun exportCsv() {
        val exporter =
            CsvExporter(this)
        val file =
            exporter.export(
                storage.getAll()
            )

        if (file == null) {
            Toast.makeText(
                this,
                "Export failed",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val uri =
            FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    "Temperature log: ${file.name}"
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Export CSV"
            )
        )


    }


    override fun onResume() {
        super.onResume()

        if (recorder.isRunning()) {
            recordingUiActive = true
            restartUiUpdater()
            handler.post(
                recordingTimerRunnable
            )
        } else {
            recordingUiActive = false
            clearRecordingDisplay()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(
            uiRunnable
        )
        handler.removeCallbacks(
            recordingTimerRunnable
        )

        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(
            uiRunnable
        )

        if (recorder.isRunning()) {
            recorder.stop()
        }

        super.onDestroy()
    }

    private fun formatTemperature(
        value: Float
    ): String {
        return "%.1f".format(value)
    }
    private fun buildStatsText(
        max: Float?,
        min: Float?,
        average: Float?
    ): SpannableString {

        val maxText =
            "MAX  ${max?.let { formatTemperature(it) } ?: "--"} °C"

        val minText =
            "MIN  ${min?.let { formatTemperature(it) } ?: "--"} °C"

        val avgText =
            "AVG  ${average?.let { formatTemperature(it) } ?: "--"} °C"

        val text =
            "$maxText\n$minText\n$avgText"

        val result =
            SpannableString(text)

        val maxColor =
            ContextCompat.getColor(
                this,
                R.color.temperature_max
            )

        val minColor =
            ContextCompat.getColor(
                this,
                R.color.temperature_min
            )

        val avgColor =
            ContextCompat.getColor(
                this,
                R.color.temperature_average
            )

        result.setSpan(
            ForegroundColorSpan(maxColor),
            0,
            maxText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            ForegroundColorSpan(minColor),
            maxText.length + 1,
            maxText.length + 1 + minText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            ForegroundColorSpan(avgColor),
            maxText.length + minText.length + 2,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }
}