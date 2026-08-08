package com.example.meandgpt2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var sensorPreferences: SensorPreferences
    private lateinit var statistics: Statistics

    private lateinit var txtStatus: TextView
    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView
    private lateinit var txtSensors: TextView
    private lateinit var spRefresh: Spinner
    private lateinit var btnSettings: Button
    private lateinit var btnRecording: Button

    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable

    private var refreshInterval = 1000L
    private var updating = false

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

        txtSensors =
            findViewById(R.id.txtSensors)

        spRefresh =
            findViewById(R.id.spRefresh)

        btnSettings =
            findViewById(R.id.btnSettings)

        btnRecording =
            findViewById(R.id.btnRecording)

        setupRefreshRate()
        setupButtons()

        txtStatus.text =
            "Monitoring stopped"
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
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
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
        btnSettings.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        btnRecording.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RecordingActivity::class.java
                )
            )
        }
    }

    private fun readTemperatures() {
        try {
            val sample =
                thermalReader.readAll()

            // فقط سه سنسور اصلی وارد Statistics می‌شوند.
            statistics.update(sample)

            txtCPU.text =
                buildMainTemperatureText(
                    "CPU",
                    sample.cpu,
                    statistics.cpuMin,
                    statistics.cpuMax,
                    statistics.cpuAverage
                )

            txtGPU.text =
                buildMainTemperatureText(
                    "GPU",
                    sample.gpu,
                    statistics.gpuMin,
                    statistics.gpuMax,
                    statistics.gpuAverage
                )

            txtBattery.text =
                buildMainTemperatureText(
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

            txtSensors.text =
                if (extraSensors.isEmpty()) {
                    "No additional sensors selected"
                } else {
                    extraSensors.entries
                        .joinToString("\n") {
                            "${it.key}: ${formatTemperature(it.value)} °C"
                        }
                }

        } catch (e: Exception) {
            Log.e(
                "MONITOR",
                "Read failed",
                e
            )
        }
    }

    private fun buildMainTemperatureText(
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

    private fun formatTemperature(
        value: Float
    ): String {
        return "%.1f".format(value)
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
}

