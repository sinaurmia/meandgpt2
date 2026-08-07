package com.example.meandgpt2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var storage: Storage
    private lateinit var recorder: Recorder
    private lateinit var sensorPreferences: SensorPreferences
    private lateinit var statistics: Statistics

    private lateinit var txtStatus: TextView
    private lateinit var txtCPU: TextView
    private lateinit var txtGPU: TextView
    private lateinit var txtBattery: TextView
    private lateinit var txtSensors: TextView
    private lateinit var txtCount: TextView
    private lateinit var txtLog: TextView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnShowLog: Button
    private lateinit var btnExport: Button

    private lateinit var spRefresh: Spinner
    private lateinit var spCPU: Spinner
    private lateinit var spGPU: Spinner

    private var loadingSettings = true
    private var uiInterval = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

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


        // preferences
        sensorPreferences =
            SensorPreferences(this)


        // views
        txtStatus = findViewById(R.id.txtStatus)
        txtCPU = findViewById(R.id.txtCPU)
        txtGPU = findViewById(R.id.txtGPU)
        txtBattery = findViewById(R.id.txtBattery)
        txtSensors = findViewById(R.id.txtSensors)
        txtCount = findViewById(R.id.txtCount)
        txtLog = findViewById(R.id.txtLog)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnShowLog = findViewById(R.id.btnShowLog)
        btnExport = findViewById(R.id.btnExport)

        spRefresh = findViewById(R.id.spRefresh)
        spCPU = findViewById(R.id.spCPU)
        spGPU = findViewById(R.id.spGPU)



        // refresh spinner

        val refreshOptions =
            listOf(
                "1 Second",
                "5 Seconds",
                "10 Seconds",
                "30 Seconds",
                "1 Minute"
            )

        val refreshValues =
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
                refreshOptions
            )


        val savedRate =
            sensorPreferences.getRefreshRate()


        refreshValues.indexOf(savedRate)
            .takeIf { it >= 0 }
            ?.let {
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

                    val interval =
                        refreshValues[position]


                    sensorPreferences.saveRefreshRate(
                        interval
                    )


                    if (::recorder.isInitialized) {

                        recorder.setInterval(
                            interval
                        )

                    }


                    Log.d(
                        "RATE_TEST",
                        "Rate: $uiInterval"
                    )
                }


                override fun onNothingSelected(
                    parent: AdapterView<*>
                ) {}
            }



        // core objects

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


        recorder.setInterval(
            sensorPreferences.getRefreshRate()
        )



        // sensor lists

        val sensorList =
            mutableListOf("Auto")


        sensorList.addAll(
            thermalReader.getSensorList()
        )


        val sensorAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sensorList
            )


        spCPU.adapter = sensorAdapter
        spGPU.adapter = sensorAdapter



        val savedCPU =
            sensorPreferences.getCpuSensor()

        val savedGPU =
            sensorPreferences.getGpuSensor()



        sensorList.indexOf(savedCPU)
            .takeIf { it >= 0 }
            ?.let {
                spCPU.setSelection(it)
            }



        sensorList.indexOf(savedGPU)
            .takeIf { it >= 0 }
            ?.let {
                spGPU.setSelection(it)
            }



        loadingSettings = false



        spCPU.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    if(!loadingSettings){

                        val value =
                            if(position == 0)
                                null
                            else
                                sensorList[position]


                        sensorPreferences.saveCpuSensor(
                            value
                        )

                    }

                }

                override fun onNothingSelected(
                    parent: AdapterView<*>
                ){}

            }



        spGPU.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    if(!loadingSettings){

                        val value =
                            if(position == 0)
                                null
                            else
                                sensorList[position]


                        sensorPreferences.saveGpuSensor(
                            value
                        )

                    }

                }


                override fun onNothingSelected(
                    parent: AdapterView<*>
                ){}

            }



        val uiHandler =
            android.os.Handler(mainLooper)


        val uiUpdater =
            object : Runnable {

                override fun run() {

                    val last =
                        storage.getLast()


                    txtCPU.text =
                        "CPU : ${last?.cpu ?: "--"}  Min: ${statistics.cpuMin ?: "--"}  Max: ${statistics.cpuMax ?: "--"}"


                    txtGPU.text =
                        "GPU : ${last?.gpu ?: "--"}  Min: ${statistics.gpuMin ?: "--"}  Max: ${statistics.gpuMax ?: "--"}"


                    txtBattery.text =
                        "Battery : ${last?.battery ?: "--"}  Min: ${statistics.batteryMin ?: "--"}  Max: ${statistics.batteryMax ?: "--"}"


                    txtCount.text =
                        "Samples : ${storage.count()}"


                    if(last != null){

                        txtSensors.text =
                            last.sensors.entries.joinToString("\n"){
                                "${it.key}: ${it.value}"
                            }

                    }


                    uiHandler.postDelayed(
                        this,
                        1000L
                    )

                }
            }


        uiHandler.post(
            uiUpdater
        )
        btnStart.setOnClickListener {

            recorder.start()

            txtStatus.text =
                "Recording..."

            Log.d(
                "UI_TEST",
                "Start pressed"
            )

        }


        btnStop.setOnClickListener {

            recorder.stop()

            txtStatus.text =
                "Stopped"

            Log.d(
                "UI_TEST",
                "Stop pressed"
            )

        }


        btnReset.setOnClickListener {

            if(recorder.isRunning()) {

                recorder.stop()

                txtStatus.text =
                    "Stopped"
            }


            storage.clear()

            statistics.reset()


            txtCPU.text =
                "CPU : --  Min: --  Max: --"

            txtGPU.text =
                "GPU : --  Min: --  Max: --"

            txtBattery.text =
                "Battery : --  Min: --  Max: --"


            txtSensors.text =
                "Sensors"


            txtCount.text =
                "Samples : 0"


            txtLog.text =
                ""


            Log.d(
                "UI_TEST",
                "Reset pressed"
            )

        }



        btnShowLog.setOnClickListener {

            val logs =
                storage.getLastRecords(500)


            val builder =
                StringBuilder()


            for(sample in logs){

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


                if(sample.sensors.isNotEmpty()){

                    builder.append(
                        "Sensors: "
                    )


                    builder.append(
                        sample.sensors.entries.joinToString(" | "){
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
                if(builder.isEmpty())
                    "No records"
                else
                    builder.toString()


            Log.d(
                "UI_TEST",
                "Show log: ${logs.size}"
            )

        }



        btnExport.setOnClickListener {


            val exporter =
                CsvExporter(this)


            val file =
                exporter.export(
                    storage.getAll()
                )


            if(file == null){

                Toast.makeText(
                    this,
                    "Export failed",
                    Toast.LENGTH_SHORT
                ).show()


                return@setOnClickListener
            }


            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )


            val intent =
                Intent(
                    Intent.ACTION_SEND
                )


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



        Log.d(
            "MAIN_TEST",
            "MainActivity connected"
        )

    }



    override fun onDestroy() {

        super.onDestroy()


        if(::recorder.isInitialized){

            recorder.stop()

        }

    }

}