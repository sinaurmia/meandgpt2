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


class RecordingActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var storage: Storage
    private lateinit var recorder: Recorder
    private lateinit var statistics: Statistics
    private lateinit var sensorPreferences: SensorPreferences

    private lateinit var spRefresh: Spinner
    private lateinit var txtStatus: TextView
    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView
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
        setupRefreshRate()
        setupButtons()
        clearRecordingDisplay()
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
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )
            finish()
        }

        btnSettings.setOnClickListener {
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

        recordingUiActive = true

        txtStatus.text = "Recording..."

        updateRecordingDisplay()

        restartUiUpdater()
    }

    private fun stopRecording() {
        if (!recorder.isRunning())
            return

        recorder.stop()

        recordingUiActive = false

        handler.removeCallbacks(
            uiRunnable
        )

        txtStatus.text = "Stopped"

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

        clearRecordingDisplay()
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
            buildTemperatureText(
                "CPU",
                sample.cpu,
                statistics.cpuMin,
                statistics.cpuMax,
                statistics.cpuAverage
            )

        txtGPU.text =
            buildTemperatureText(
                "GPU",
                sample.gpu,
                statistics.gpuMin,
                statistics.gpuMax,
                statistics.gpuAverage
            )

        txtBattery.text =
            buildTemperatureText(
                "Battery",
                sample.battery,
                statistics.batteryMin,
                statistics.batteryMax,
                statistics.batteryAverage
            )
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
        txtCPU.text = "CPU: --"
        txtGPU.text = "GPU: --"
        txtBattery.text = "Battery: --"
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
        } else {
            recordingUiActive = false
            clearRecordingDisplay()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(
            uiRunnable
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
}