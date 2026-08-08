package com.example.meandgpt2

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

        val zones =
            thermalDir.listFiles()
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

                val name =
                    readFileSafe(typeFile)
                        ?: continue

                result.add(
                    SensorInfo(
                        name,
                        tempFile
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
        var batteryPercentage: Int? = null

        val list =
            sensorCache
                ?: scanSensors().also {
                    sensorCache = it
                }

        val selectedCpu =
            preferences.getCpuSensor()

        val selectedGpu =
            preferences.getGpuSensor()

        val enabledSensors =
            preferences.getSelectedSensors()

        for (sensor in list) {
            try {
                val sensorType =
                    detectSensor(sensor.name)

                val isSelectedSensor =
                    enabledSensors.contains(sensor.name)

                val isSelectedCpu =
                    selectedCpu != null &&
                            sensor.name.equals(
                                selectedCpu,
                                true
                            )

                val isSelectedGpu =
                    selectedGpu != null &&
                            sensor.name.equals(
                                selectedGpu,
                                true
                            )

                val shouldRead =
                    isSelectedSensor ||
                            isSelectedCpu ||
                            isSelectedGpu ||
                            sensorType == SensorType.CPU ||
                            sensorType == SensorType.GPU ||
                            sensorType == SensorType.BATTERY

                if (!shouldRead)
                    continue

                val value =
                    readTemperature(sensor.tempFile)
                        ?: continue

                // CPU اصلی
                if (
                    selectedCpu != null &&
                    sensor.name.equals(
                        selectedCpu,
                        true
                    )
                ) {
                    cpu = value
                    cpuSensorName = sensor.name
                }

                // GPU اصلی
                if (
                    selectedGpu != null &&
                    sensor.name.equals(
                        selectedGpu,
                        true
                    )
                ) {
                    gpu = value
                    gpuSensorName = sensor.name
                }

                // انتخاب خودکار CPU
                if (
                    sensorType == SensorType.CPU &&
                    selectedCpu == null &&
                    cpu == null
                ) {
                    cpu = value
                    cpuSensorName = sensor.name
                }

                // انتخاب خودکار GPU
                if (
                    sensorType == SensorType.GPU &&
                    selectedGpu == null &&
                    gpu == null
                ) {
                    gpu = value
                    gpuSensorName = sensor.name
                }

                // Battery thermal
                if (
                    sensorType == SensorType.BATTERY &&
                    battery == null
                ) {
                    battery = value
                }

                /*
                 * فقط سنسورهای فرعی انتخاب‌شده ذخیره می‌شوند.
                 *
                 * سنسور CPU اصلی
                 * سنسور GPU اصلی
                 * سنسور Battery
                 *
                 * در sensors قرار نمی‌گیرند.
                 */
                val isMainCpu =
                    sensor.name.equals(
                        cpuSensorName,
                        true
                    )

                val isMainGpu =
                    sensor.name.equals(
                        gpuSensorName,
                        true
                    )

                val isMainBattery =
                    sensorType == SensorType.BATTERY

                if (
                    isSelectedSensor &&
                    !isMainCpu &&
                    !isMainGpu &&
                    !isMainBattery
                ) {
                    sensors[sensor.name] = value
                }

            } catch (_: Exception) {
            }
        }

        if (
            preferences.isBatteryPercentageEnabled()
        ) {
            batteryPercentage =
                readBatteryPercentage()
        }

        return Sample(
            time = System.currentTimeMillis(),
            date = getCurrentDate(),
            cpu = cpu,
            cpuSensorName = cpuSensorName,
            gpu = gpu,
            gpuSensorName = gpuSensorName,
            battery = battery,
            batteryPercentage = batteryPercentage,
            sensors = sensors.toSortedMap()
        )
    }

    private fun readBatteryPercentage(): Int? {
        return try {
            val intent =
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                ) ?: return null

            val level =
                intent.getIntExtra(
                    "level",
                    -1
                )

            val scale =
                intent.getIntExtra(
                    "scale",
                    -1
                )

            if (
                level < 0 ||
                scale <= 0
            ) {
                null
            } else {
                ((level * 100f) / scale)
                    .toInt()
                    .coerceIn(0, 100)
            }

        } catch (_: Exception) {
            null
        }
    }

    private enum class SensorType {
        CPU,
        GPU,
        BATTERY,
        UNKNOWN
    }

    private fun detectSensor(
        name: String
    ): SensorType {
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

    private fun readFileSafe(
        file: File
    ): String? {
        return try {
            if (!file.exists())
                return null

            file.readText().trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun readTemperature(
        file: File
    ): Float? {
        val text =
            readFileSafe(file)
                ?: return null

        var value =
            text.toFloatOrNull()
                ?: return null

        if (value > 1000f)
            value /= 1000f

        return value
    }
}

