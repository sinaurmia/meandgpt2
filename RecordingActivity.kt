package com.example.meandgpt2

import android.content.Intent
import android.os.Bundle
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
    private lateinit var txtCount: TextView
    private lateinit var txtLog: TextView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnShowLog: Button
    private lateinit var btnExport: Button

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_recording
        )

        title = "Recording"

        sensorPreferences =
            SensorPreferences(this)

        thermalReader =
            ThermalReader(this)

        storage =
            Storage(this)

        statistics =
            Statistics()

        recorder =
            Recorder(
                thermalReader,
                storage,
                statistics
            )

        spRefresh =
            findViewById(R.id.spRefresh)

        txtStatus =
            findViewById(R.id.txtStatus)

        txtCount =
            findViewById(R.id.txtCount)

        txtLog =
            findViewById(R.id.txtLog)

        btnStart =
            findViewById(R.id.btnStart)

        btnStop =
            findViewById(R.id.btnStop)

        btnReset =
            findViewById(R.id.btnReset)

        btnShowLog =
            findViewById(R.id.btnShowLog)

        btnExport =
            findViewById(R.id.btnExport)

        setupRefreshRate()
        setupButtons()

        updateStatus()
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

        val saved =
            sensorPreferences.getRefreshRate()

        values.indexOf(saved)
            .takeIf { it >= 0 }
            ?.let {
                spRefresh.setSelection(it)
            }

        spRefresh.onItemSelectedListener =
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent:
                    android.widget.AdapterView<*>,
                    view:
                    android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    recorder.setInterval(
                        values[position]
                    )

                    sensorPreferences.saveRefreshRate(
                        values[position]
                    )
                }

                override fun onNothingSelected(
                    parent:
                    android.widget.AdapterView<*>
                ) {}
            }
    }

    private fun setupButtons() {

        btnStart.setOnClickListener {

            recorder.start()

            txtStatus.text =
                "Recording..."
        }

        btnStop.setOnClickListener {

            recorder.stop()

            txtStatus.text =
                "Stopped"
        }

        btnReset.setOnClickListener {

            if (recorder.isRunning()) {
                recorder.stop()
            }

            storage.clear()
            statistics.reset()

            txtStatus.text =
                "Stopped"

            txtCount.text =
                "Samples: 0"

            txtLog.text =
                ""
        }

        btnShowLog.setOnClickListener {

            showLog()
        }

        btnExport.setOnClickListener {

            exportCsv()
        }
    }

    private fun updateStatus() {

        txtCount.text =
            "Samples: ${storage.count()}"

        txtStatus.text =
            if (recorder.isRunning())
                "Recording..."
            else
                "Stopped"
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
            "Samples: ${storage.count()}"
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

        updateStatus()

        txtCount.text =
            "Samples: ${storage.count()}"
    }

    override fun onDestroy() {

        if (recorder.isRunning()) {
            recorder.stop()
        }

        super.onDestroy()
    }

}
