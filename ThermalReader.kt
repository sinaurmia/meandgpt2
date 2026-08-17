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
        private const val THERMAL_VIRTUAL_PATH = "/sys/devices/virtual/thermal"
        private const val HWMON_PATH = "/sys/class/hwmon"
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
            Locale.US
        ).format(Date())
    }

    fun getSensorList(): List<String> {
        sensorCache?.let {
            return it.map { sensor -> sensor.name }.sorted()
        }

        val scanned = scanSensors()

        /*
         * اگر سیستم فعلاً هیچ سنسوری پیدا نکرد،
         * لیست خالی را به عنوان cache معتبر ذخیره نمی‌کنیم.
         *
         * این موضوع برای بعضی دستگاه‌ها مهم است که thermal
         * subsystem آنها کمی دیرتر در دسترس قرار می‌گیرد.
         */
        if (scanned.isNotEmpty()) {
            sensorCache = scanned

            preferences.saveSensorList(
                scanned.map { it.name }
            )
        } else {
            sensorCache = emptyList()
        }

        return scanned.map { it.name }.sorted()
    }

    fun refreshSensorCache() {
        val list = scanSensors()

        /*
         * فقط وقتی واقعاً چیزی پیدا شده cache را جایگزین می‌کنیم.
         * در غیر این صورت cache قبلی را بی‌دلیل نابود نمی‌کنیم.
         */
        if (list.isNotEmpty()) {
            sensorCache = list

            preferences.saveSensorList(
                list.map { it.name }
            )
        }
    }

    private fun scanSensors(): List<SensorInfo> {

        val result = mutableListOf<SensorInfo>()

        scanThermalDirectory(
            File(THERMAL_PATH),
            result
        )

        scanThermalDirectory(
            File(THERMAL_VIRTUAL_PATH),
            result
        )

        scanHwmonDirectory(
            File(HWMON_PATH),
            result
        )

        return result
            .distinctBy {
                it.name + "|" + it.tempFile.absolutePath
            }
    }

    private fun scanThermalDirectory(
        directory: File,
        result: MutableList<SensorInfo>
    ) {

        if (!directory.exists() ||
            !directory.isDirectory
        ) {
            return
        }

        val zones =
            directory.listFiles()
                ?: return

        for (zone in zones) {

            if (!zone.isDirectory)
                continue

            val tempFile =
                File(zone, "temp")

            if (!tempFile.exists())
                continue

            val typeFile =
                File(zone, "type")

            val name =
                if (typeFile.exists()) {
                    readFileSafe(typeFile)
                } else {
                    null
                } ?: zone.name

            if (name.isBlank())
                continue

            result.add(
                SensorInfo(
                    name = name,
                    tempFile = tempFile
                )
            )
        }
    }

    private fun scanHwmonDirectory(
        directory: File,
        result: MutableList<SensorInfo>
    ) {

        if (!directory.exists() ||
            !directory.isDirectory
        ) {
            return
        }

        val devices =
            directory.listFiles()
                ?: return

        for (device in devices) {

            if (!device.isDirectory)
                continue

            val deviceName =
                readFileSafe(
                    File(device, "name")
                ) ?: device.name

            val files =
                device.listFiles()
                    ?: continue

            for (file in files) {

                if (!file.name.startsWith("temp"))
                    continue

                if (!file.name.endsWith("_input"))
                    continue

                val index =
                    file.name
                        .removePrefix("temp")
                        .removeSuffix("_input")

                val label =
                    readFileSafe(
                        File(
                            device,
                            "temp${index}_label"
                        )
                    )

                val name =
                    when {
                        !label.isNullOrBlank() ->
                            "$deviceName $label"

                        index.isNotBlank() ->
                            "$deviceName temp$index"

                        else ->
                            deviceName
                    }

                result.add(
                    SensorInfo(
                        name = name,
                        tempFile = file
                    )
                )
            }
        }
    }

    fun readAll(): Sample {

        val sensors =
            mutableMapOf<String, Float>()

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

        val selectedSensors =
            preferences.getSelectedSensors()

        for (sensor in list) {

            try {

                val sensorType =
                    detectSensor(sensor.name)

                val isSelectedSensor =
                    selectedSensors.contains(
                        sensor.name
                    )

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

                /*
                 * سنسورهای اصلی همیشه خوانده می‌شوند،
                 * سنسورهای اضافی فقط در صورت انتخاب شدن.
                 */
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
                    readTemperature(
                        sensor.tempFile
                    ) ?: continue

                /*
                 * CPU انتخاب‌شده توسط کاربر
                 */
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

                /*
                 * GPU انتخاب‌شده توسط کاربر
                 */
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

                /*
                 * انتخاب خودکار CPU
                 */
                if (
                    selectedCpu == null &&
                    cpu == null &&
                    sensorType == SensorType.CPU
                ) {
                    cpu = value
                    cpuSensorName = sensor.name
                }

                /*
                 * انتخاب خودکار GPU
                 */
                if (
                    selectedGpu == null &&
                    gpu == null &&
                    sensorType == SensorType.GPU
                ) {
                    gpu = value
                    gpuSensorName = sensor.name
                }

                /*
                 * Battery thermal
                 */
                if (
                    sensorType == SensorType.BATTERY &&
                    battery == null
                ) {
                    battery = value
                }

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

                /*
                 * فقط سنسورهای انتخاب‌شده اضافی
                 * وارد Sample.sensors می‌شوند.
                 */
                if (
                    isSelectedSensor &&
                    !isMainCpu &&
                    !isMainGpu &&
                    !isMainBattery
                ) {
                    sensors[sensor.name] = value
                }

            } catch (_: Exception) {
                // یک سنسور خراب نباید خواندن بقیه را متوقف کند.
            }
        }

        /*
         * اگر thermal battery وجود نداشت،
         * درصد باتری همچنان از Battery API خوانده می‌شود.
         */
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
                    IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                    )
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
                (
                        level * 100f / scale
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )
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

        val n =
            name
                .lowercase(Locale.US)
                .replace(
                    "_",
                    ""
                )
                .replace(
                    "-",
                    ""
                )
                .replace(
                    " ",
                    ""
                )

        return when {

            /*
             * Qualcomm
             */
            n.contains("cpu") ||
                    n.contains("cpub") ||
                    n.contains("cpul") ||
                    n.contains("cpuss") ||
                    n.contains("cpucluster") ||
                    n.contains("cluster") ||
                    n.contains("big") ||
                    n.contains("little") ||
                    n.contains("apc") ||
                    n.contains("msm") ||
                    n.contains("krait") ||
                    n.contains("x1") ||
                    n.contains("x2") ||
                    n.contains("x3") ||
                    n.contains("x4") ->

                SensorType.CPU

            /*
             * MediaTek
             */
            n.contains("mtktscpu") ||
                    n.contains("mtkcpu") ||
                    n.contains("mtktsapus") ||
                    n.contains("mtktsap") ||

                    /*
                     * Samsung / Exynos
                     */
                    n.contains("exynos") ||
                    n.contains("exynoscpu") ||

                    /*
                     * HiSilicon / Huawei
                     */
                    n.contains("kirin") ||
                    n.contains("hisi") ||
                    n.contains("hi3660") ||
                    n.contains("hi3670") ||
                    n.contains("hi6250") ||

                    /*
                     * Unisoc / Spreadtrum
                     */
                    n.contains("unisoc") ||
                    n.contains("sprd") ||

                    /*
                     * Rockchip
                     */
                    n.contains("rockchip") ||
                    n.contains("rk33") ||
                    n.contains("rk35") ->

                SensorType.CPU

            /*
             * GPU
             */
            n.contains("gpu") ||
                    n.contains("kgsl") ||
                    n.contains("gpuss") ||
                    n.contains("adreno") ||
                    n.contains("mali") ||
                    n.contains("panfrost") ||
                    n.contains("powervr") ||
                    n.contains("vivante") ||
                    n.contains("graphics") ||
                    n.contains("gfx") ->

                SensorType.GPU

            /*
             * Battery
             */
            n.contains("battery") ||
                    n.contains("bms") ||
                    n.contains("battery") ||
                    n == "bat" ||
                    n.startsWith("bat") ||
                    n.contains("pmic") ||
                    n.contains("charger") ->

                SensorType.BATTERY

            else ->
                SensorType.UNKNOWN
        }
    }

    private fun readFileSafe(
        file: File
    ): String? {

        return try {

            if (
                !file.exists() ||
                !file.isFile ||
                !file.canRead()
            ) {
                return null
            }

            file.readText()
                .trim()

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

        /*
         * Linux thermal معمولاً millidegree Celsius
         * برمی‌گرداند.
         */
        if (value > 1000f) {
            value /= 1000f
        }

        /*
         * بعضی hwmonها ممکن است microdegree باشند.
         */
        if (value > 1000f) {
            value /= 1000f
        }

        /*
         * دمای غیرواقعی را کنار می‌گذاریم.
         * بازه عمداً نسبتاً وسیع است تا گوشی‌هایی
         * که بیش از 80 یا حتی 90 درجه گزارش می‌کنند
         * حذف نشوند.
         */
        if (
            value < -40f ||
            value > 150f
        ) {
            return null
        }

        return value
    }
}