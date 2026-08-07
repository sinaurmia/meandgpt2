package com.example.meandgpt2

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var thermalReader: ThermalReader
    private lateinit var sensorPreferences: SensorPreferences

    private lateinit var spCPU: Spinner
    private lateinit var spGPU: Spinner
    private lateinit var chkBatteryLevel: CheckBox
    private lateinit var sensorContainer: LinearLayout
    private lateinit var btnSave: Button

    private val sensorCheckBoxes =
        mutableMapOf<String, CheckBox>()


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)


        sensorPreferences =
            SensorPreferences(this)

        thermalReader =
            ThermalReader(this)


        spCPU =
            findViewById(R.id.spCPU)

        spGPU =
            findViewById(R.id.spGPU)

        chkBatteryLevel =
            findViewById(R.id.chkBatteryLevel)

        sensorContainer =
            findViewById(R.id.sensorContainer)

        btnSave =
            findViewById(R.id.btnSave)


        setupSensors()

        setupBattery()

        setupSaveButton()
    }


    private fun setupSensors() {

        val sensors =
            thermalReader.getSensorList()


        val sensorList =
            mutableListOf("Auto")

        sensorList.addAll(sensors)


        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sensorList
            )


        spCPU.adapter = adapter
        spGPU.adapter = adapter


        val savedCPU =
            sensorPreferences.getCpuSensor()

        val savedGPU =
            sensorPreferences.getGpuSensor()


        sensorList
            .indexOf(savedCPU)
            .takeIf { it >= 0 }
            ?.let {
                spCPU.setSelection(it)
            }


        sensorList
            .indexOf(savedGPU)
            .takeIf { it >= 0 }
            ?.let {
                spGPU.setSelection(it)
            }


        val selectedSensors =
            sensorPreferences.getSelectedSensors()


        for (sensor in sensors) {

            val checkBox =
                CheckBox(this)


            checkBox.text =
                sensor


            checkBox.isChecked =
                selectedSensors.contains(sensor)


            sensorContainer.addView(
                checkBox
            )


            sensorCheckBoxes[sensor] =
                checkBox
        }
    }


    private fun setupBattery() {

        chkBatteryLevel.isChecked =
            sensorPreferences.isBatteryLevelEnabled()
    }


    private fun setupSaveButton() {

        btnSave.setOnClickListener {

            saveSettings()

            Toast.makeText(
                this,
                "Settings saved",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }


    private fun saveSettings() {

        val cpuPosition =
            spCPU.selectedItemPosition

        val gpuPosition =
            spGPU.selectedItemPosition


        val cpuSensor =
            if (cpuPosition == 0)
                null
            else
                spCPU.selectedItem.toString()


        val gpuSensor =
            if (gpuPosition == 0)
                null
            else
                spGPU.selectedItem.toString()


        sensorPreferences.saveCpuSensor(
            cpuSensor
        )

        sensorPreferences.saveGpuSensor(
            gpuSensor
        )


        val selectedSensors =
            sensorCheckBoxes
                .filter {
                    it.value.isChecked
                }
                .keys
                .toList()


        sensorPreferences.saveSelectedSensors(
            selectedSensors
        )


        sensorPreferences.saveBatteryLevelEnabled(
            chkBatteryLevel.isChecked
        )
    }
}