package com.example.meandgpt2

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThermalReader(
    private val context: Context
) {

    companion object {
        private const val THERMAL_PATH = "/sys/class/thermal"
    }

    private val preferences = SensorPreferences(context)

    private data class SensorInfo(
        val name: String,
        val tempFile: File
    )

    private var sensorCache: List<SensorInfo>? = null

    private fun getCurrentDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    fun getSensorList(): List<String> {
        sensorCache?.let {
            return it.map { sensor -> sensor.name }.sorted()
        }

        val saved = preferences.getSensorList()

        if (saved.isNotEmpty()) {
            val rebuilt = rebuildFromSavedList(saved)

            if (rebuilt.isNotEmpty()) {
                sensorCache = rebuilt
                return rebuilt.map { it.name }.sorted()
            }
        }

        val scanned = scanSensors()

        sensorCache = scanned

        preferences.saveSensorList(
            scanned.map { it.name }
        )

        return scanned.map { it.name }.sorted()
    }

    fun refreshSensorCache() {
        val list = scanSensors()

        sensorCache = list

        preferences.saveSensorList(
            list.map { it.name }
        )
    }

    fun getAutoCpuSensorName(): String? {
        val list = getSensorInfoList()

        return list.firstOrNull {
            detectSensor(it.name) == SensorType.CPU
        }?.name
    }

    fun getAutoGpuSensorName(): String? {
        val list = getSensorInfoList()

        return list.firstOrNull {
            detectSensor(it.name) == SensorType.GPU
        }?.name
    }

    private fun getSensorInfoList(): List<SensorInfo> {
        sensorCache?.let {
            return it
        }

        val scanned = scanSensors()

        sensorCache = scanned

        if (preferences.getSensorList().isEmpty()) {
            preferences.saveSensorList(
                scanned.map { it.name }
            )
        }

        return scanned
    }

    private fun rebuildFromSavedList(
        names: List<String>
    ): List<SensorInfo> {
        val current = scanSensors()

        return current.filter {
            names.contains(it.name)
        }
    }

    private fun scanSensors(): List<SensorInfo> {
        val result = mutableListOf<SensorInfo>()
        val thermalDir = File(THERMAL_PATH)

        if (!thermalDir.exists())
            return result

        val zones = thermalDir.listFiles()
            ?.filter {
                it.name.startsWith("thermal_zone")
            }
            ?: emptyList()

        for (zone in zones) {
            try {
                val typeFile = File(zone, "type")
                val tempFile = File(zone, "temp")

                if (!typeFile.exists() || !tempFile.exists())
                    continue

                val name = readFileSafe(typeFile)
                    ?: continue

                result.add(
                    SensorInfo(
                        name = name,
                        tempFile = tempFile
                    )
                )
            } catch (_: Exception) {
            }
        }

        return result
    }

    fun readAll(): Sample {
        val sensors = mutableMapOf<String, Float>()

        var cpu: Float? = null
        var cpuSensorName: String? = null
        var gpu: Float? = null
        var gpuSensorName: String? = null
        var battery: Float? = null

        val list = getSensorInfoList()

        val selectedCpu = preferences.getCpuSensor()
        val selectedGpu = preferences.getGpuSensor()
        val enabledSensors = preferences.getEnabledSensors()

        val autoCpu = if (selectedCpu == null)
            findAutoSensor(list, SensorType.CPU)
        else
            null

        val autoGpu = if (selectedGpu == null)
            findAutoSensor(list, SensorType.GPU)
        else
            null

        for (sensor in list) {
            val name = sensor.name

            val isSelectedCpu =
                selectedCpu != null &&
                        name.equals(selectedCpu, ignoreCase = true)

            val isSelectedGpu =
                selectedGpu != null &&
                        name.equals(selectedGpu, ignoreCase = true)

            val isAutoCpu =
                selectedCpu == null &&
                        name == autoCpu

            val isAutoGpu =
                selectedGpu == null &&
                        name == autoGpu

            val type = detectSensor(name)

            val isBattery =
                type == SensorType.BATTERY

            val isEnabled =
                enabledSensors.contains(name)

            val shouldRead =
                isSelectedCpu ||
                        isSelectedGpu ||
                        isAutoCpu ||
                        isAutoGpu ||
                        isBattery ||
                        isEnabled

            if (!shouldRead)
                continue

            try {
                val value = readTemperature(sensor.tempFile)
                    ?: continue

                if (isEnabled)
                    sensors[name] = value

                if (isSelectedCpu || isAutoCpu) {
                    cpu = value
                    cpuSensorName = name
                }

                if (isSelectedGpu || isAutoGpu) {
                    gpu = value
                    gpuSensorName = name
                }

                if (isBattery && battery == null) {
                    battery = value
                }
            } catch (_: Exception) {
            }
        }

        return Sample(
            time = System.currentTimeMillis(),
            date = getCurrentDate(),
            cpu = cpu,
            cpuSensorName = cpuSensorName,
            gpu = gpu,
            gpuSensorName = gpuSensorName,
            battery = battery,
            sensors = sensors.toSortedMap()
        )
    }

    private fun findAutoSensor(
        list: List<SensorInfo>,
        type: SensorType
    ): String? {
        return list.firstOrNull {
            detectSensor(it.name) == type
        }?.name
    }

    private enum class SensorType {
        CPU,
        GPU,
        BATTERY,
        UNKNOWN
    }

    private fun detectSensor(name: String): SensorType {
        val n = name.lowercase()

        return when {
            n.contains("cpu") ||
                    n.contains("mtktscpu") ||
                    n.contains("cluster") ||
                    n.contains("big") ||
                    n.contains("little") ->
                SensorType.CPU

            n.contains("gpu") ||
                    n.contains("kgsl") ||
                    n.contains("gpuss") ||
                    n.contains("adreno") ->
                SensorType.GPU

            n.contains("battery") ||
                    n.contains("bms") ||
                    n.contains("bat") ->
                SensorType.BATTERY

            else ->
                SensorType.UNKNOWN
        }
    }

    private fun readFileSafe(file: File): String? {
        return try {
            if (!file.exists())
                return null

            file.readText().trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun readTemperature(file: File): Float? {
        val text = readFileSafe(file)
            ?: return null

        var value = text.toFloatOrNull()
            ?: return null

        if (value > 1000f)
            value /= 1000f

        return value
    }
}