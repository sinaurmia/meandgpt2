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
    private lateinit var txtLog: TextView

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
        txtCount = findViewById(R.id.txtCount)
        txtLog = findViewById(R.id.txtLog)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnShowLog = findViewById(R.id.btnShowLog)
        btnExport = findViewById(R.id.btnExport)

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
    }

    private fun startRecording() {
        if (recorder.isRunning())
            return

        statistics.reset()

        recordingStartCount =
            storage.count()

        txtLog.text = ""

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
        txtLog.text = ""

        clearRecordingDisplay()
    }

    private fun updateRecordingDisplay() {
        if (!recordingUiActive)
            return

        if (!recorder.isRunning())
            return

        val sample = storage.getLast()

        val currentCount = storage.count()

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
        val logs =
            storage.getLastRecords(500)

        val builder =
            StringBuilder()

        for (sample in logs) {
            builder.append(
                "Date: ${sample.date}\n"
            )

            builder.append(
                "CPU: ${sample.cpu ?: "--"} "
            )

            builder.append(
                "GPU: ${sample.gpu ?: "--"} "
            )

            builder.append(
                "BAT: ${sample.battery ?: "--"}\n"
            )

            if (sample.sensors.isNotEmpty()) {
                builder.append(
                    "Sensors: "
                )

                builder.append(
                    sample.sensors.entries
                        .joinToString(" | ") {
                            "${it.key}:${it.value}"
                        }
                )

                builder.append("\n")
            }

            builder.append(
                "----------------------\n"
            )
        }

        txtLog.text =
            if (builder.isEmpty())
                "No records"
            else
                builder.toString()

        txtCount.text =
            "Samples: ${
                maxOf(
                    0,
                    storage.count() - recordingStartCount
                )
            }"
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
            Intent(Intent.ACTION_SEND)

        intent.type =
            "text/csv"

        intent.putExtra(
            Intent.EXTRA_STREAM,
            uri
        )

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

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