package com.example.meandgpt2

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var btnBack: Button
    private lateinit var thermalReader: ThermalReader
    private lateinit var sensorPreferences: SensorPreferences

    private lateinit var spCPU: Spinner
    private lateinit var spGPU: Spinner
    private lateinit var sensorContainer: LinearLayout
    private lateinit var chkBatteryPercentage: CheckBox
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        title = "Settings"

        sensorPreferences = SensorPreferences(this)
        thermalReader = ThermalReader(this)

        spCPU = findViewById(R.id.spCPU)
        spGPU = findViewById(R.id.spGPU)
        sensorContainer = findViewById(R.id.sensorContainer)
        chkBatteryPercentage = findViewById(R.id.chkBatteryPercentage)
        btnSave = findViewById(R.id.btnSave)
        btnBack =
            findViewById(R.id.btnBack)

        setupSensorSelectors()
        setupSensorCheckboxes()
        setupBattery()

        btnSave.setOnClickListener {
            saveSettings()
        }
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSensorSelectors() {
        val sensors = thermalReader.getSensorList()
        val list = mutableListOf("Auto")
        list.addAll(sensors)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            list
        )

        spCPU.adapter = adapter
        spGPU.adapter = adapter

        val savedCPU = sensorPreferences.getCpuSensor()
        val savedGPU = sensorPreferences.getGpuSensor()

        if (savedCPU != null) {
            list.indexOf(savedCPU)
                .takeIf { it >= 0 }
                ?.let { spCPU.setSelection(it) }
        }

        if (savedGPU != null) {
            list.indexOf(savedGPU)
                .takeIf { it >= 0 }
                ?.let { spGPU.setSelection(it) }
        }
    }

    private fun setupSensorCheckboxes() {
        sensorContainer.removeAllViews()

        val sensors = thermalReader.getSensorList()
        val selected = sensorPreferences.getSelectedSensors()

        for (sensor in sensors) {
            val checkBox = CheckBox(this)

            checkBox.text = sensor
            checkBox.tag = sensor
            checkBox.isChecked = selected.contains(sensor)

            sensorContainer.addView(
                checkBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun setupBattery() {
        chkBatteryPercentage.isChecked =
            sensorPreferences.isBatteryPercentageEnabled()
    }

    private fun saveSettings() {
        val cpuSensor =
            if (spCPU.selectedItemPosition == 0) {
                null
            } else {
                spCPU.selectedItem?.toString()
            }

        sensorPreferences.saveCpuSensor(cpuSensor)

        val gpuSensor =
            if (spGPU.selectedItemPosition == 0) {
                null
            } else {
                spGPU.selectedItem?.toString()
            }

        sensorPreferences.saveGpuSensor(gpuSensor)

        val selectedSensors = mutableSetOf<String>()

        for (i in 0 until sensorContainer.childCount) {
            val view = sensorContainer.getChildAt(i)

            if (view is CheckBox && view.isChecked) {
                val sensorName = view.tag?.toString()

                if (!sensorName.isNullOrEmpty()) {
                    selectedSensors.add(sensorName)
                }
            }
        }

        sensorPreferences.saveSelectedSensors(selectedSensors)

        sensorPreferences.saveBatteryPercentageEnabled(
            chkBatteryPercentage.isChecked
        )

        finish()
    }


}
