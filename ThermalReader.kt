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

        private const val THERMAL_PATH =
            "/sys/class/thermal"

    }


    private val preferences =
        SensorPreferences(context)


    private data class SensorInfo(
        val name: String,
        val tempFile: File
    )


    private var sensorCache:
            List<SensorInfo>? = null


    private fun getCurrentDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    }


// --------------------------------------------------
// Sensor List
// --------------------------------------------------

    /*
     * این تابع تمام سنسورهای موجود در گوشی را برمی‌گرداند.
     *
     * نکته:
     * اینجا همه سنسورها اسکن می‌شوند چون صفحه تنظیمات
     * باید بتواند لیست کامل سنسورها را به کاربر نشان دهد.
     *
     * اما در readAll() فقط سنسورهای مورد نیاز خوانده می‌شوند.
     */

    fun getSensorList(): List<String> {

        sensorCache?.let {

            return it
                .map { sensor ->
                    sensor.name
                }
                .sorted()

        }


        val saved =
            preferences.getSensorList()


        if (saved.isNotEmpty()) {

            val rebuilt =
                rebuildFromSavedList(
                    saved
                )


            if (rebuilt.isNotEmpty()) {

                sensorCache =
                    rebuilt

                return rebuilt
                    .map {
                        it.name
                    }
                    .sorted()

            }

        }


        val scanned =
            scanSensors()


        sensorCache =
            scanned


        preferences.saveSensorList(
            scanned.map {
                it.name
            }
        )


        return scanned
            .map {
                it.name
            }
            .sorted()

    }


    /*
     * اسکن مجدد سنسورها.
     *
     * در صورت تغییر سنسورهای گوشی یا نیاز به
     * به‌روزرسانی لیست تنظیمات استفاده می‌شود.
     */

    fun refreshSensorCache() {

        val list =
            scanSensors()


        sensorCache =
            list


        preferences.saveSensorList(
            list.map {
                it.name
            }
        )

    }


    private fun rebuildFromSavedList(
        names: List<String>
    ): List<SensorInfo> {

        val current =
            scanSensors()


        return current.filter {

            names.contains(
                it.name
            )

        }

    }


// --------------------------------------------------
// Scan Thermal Zones
// --------------------------------------------------

    private fun scanSensors():
            List<SensorInfo> {

        val result =
            mutableListOf<SensorInfo>()


        val thermalDir =
            File(
                THERMAL_PATH
            )


        if (!thermalDir.exists())
            return result


        val zones =
            thermalDir.listFiles()
                ?.filter {

                    it.name.startsWith(
                        "thermal_zone"
                    )

                }
                ?: emptyList()


        for (zone in zones) {

            try {

                val typeFile =
                    File(
                        zone,
                        "type"
                    )


                val tempFile =
                    File(
                        zone,
                        "temp"
                    )


                if (
                    !typeFile.exists() ||
                    !tempFile.exists()
                ) {
                    continue
                }


                val name =
                    readFileSafe(
                        typeFile
                    )
                        ?: continue


                result.add(
                    SensorInfo(
                        name = name,
                        tempFile = tempFile
                    )
                )


            } catch (_: Exception) {

                // سنسور مشکل‌دار نادیده گرفته می‌شود.

            }

        }


        return result

    }


// --------------------------------------------------
// Read Selected Sensors
// --------------------------------------------------

    fun readAll(): Sample {

        val sensors =
            mutableMapOf<String, Float>()


        var cpu: Float? = null

        var cpuSensorName:
                String? = null


        var gpu: Float? = null

        var gpuSensorName:
                String? = null


        var battery: Float? = null


        /*
         * لیست سنسورها را از Cache می‌گیریم.
         *
         * اگر Cache وجود نداشت، یک بار اسکن می‌کنیم.
         */

        val list =
            sensorCache
                ?: scanSensors()
                    .also {

                        sensorCache =
                            it

                    }


        /*
         * سنسور CPU و GPU که کاربر به صورت دستی
         * انتخاب کرده است.
         */

        val selectedCpu =
            preferences.getCpuSensor()


        val selectedGpu =
            preferences.getGpuSensor()


        /*
         * سنسورهای اضافی که کاربر در صفحه Settings
         * فعال کرده است.
         */

        val enabledSensors =
            preferences.getEnabledSensors()


        /*
         * اگر CPU یا GPU به صورت دستی انتخاب نشده باشد،
         * اولین سنسور مناسب به صورت خودکار انتخاب می‌شود.
         *
         * فقط اسم سنسورها بررسی می‌شود و هنوز دمای آنها
         * خوانده نشده است.
         */

        val cpuSensor =
            if (selectedCpu != null) {

                list.firstOrNull {

                    it.name.equals(
                        selectedCpu,
                        true
                    )

                }

            } else {

                list.firstOrNull {

                    detectSensor(
                        it.name
                    ) == SensorType.CPU

                }

            }


        val gpuSensor =
            if (selectedGpu != null) {

                list.firstOrNull {

                    it.name.equals(
                        selectedGpu,
                        true
                    )

                }

            } else {

                list.firstOrNull {

                    detectSensor(
                        it.name
                    ) == SensorType.GPU

                }

            }


        /*
         * باتری فعلاً به صورت خودکار تشخیص داده می‌شود.
         *
         * در مرحله بعد می‌توانیم Battery Level،
         * Voltage و سایر اطلاعات باتری را جدا کنیم.
         */

        val batterySensor =
            list.firstOrNull {

                detectSensor(
                    it.name
                ) == SensorType.BATTERY

            }


        /*
         * مجموعه سنسورهایی که حتماً باید خوانده شوند.
         *
         * CPU و GPU اصلی و Battery همیشه جزو این مجموعه هستند.
         *
         * سنسورهای اضافی فقط در صورتی اضافه می‌شوند
         * که کاربر آنها را فعال کرده باشد.
         */

        val requiredSensors =
            mutableSetOf<String>()


        cpuSensor?.let {

            requiredSensors.add(
                it.name
            )

        }


        gpuSensor?.let {

            requiredSensors.add(
                it.name
            )

        }


        batterySensor?.let {

            requiredSensors.add(
                it.name
            )

        }


        requiredSensors.addAll(
            enabledSensors
        )


        /*
         * حالا فقط سنسورهای مورد نیاز را می‌خوانیم.
         *
         * این قسمت مهم‌ترین تغییر این نسخه است.
         *
         * اگر گوشی ۳۰ سنسور داشته باشد ولی کاربر فقط
         * ۳ سنسور را انتخاب کرده باشد، سنسورهای دیگر
         * اصلاً temp آنها خوانده نمی‌شود.
         */

        for (sensor in list) {

            /*
             * اگر سنسور مورد نیاز نیست، کاملاً رد می‌شود.
             */

            if (
                !requiredSensors.contains(
                    sensor.name
                )
            ) {
                continue
            }


            try {

                val value =
                    readTemperature(
                        sensor.tempFile
                    )
                        ?: continue


                /*
                 * سنسورهای اضافی انتخاب‌شده توسط کاربر
                 * داخل Sample ذخیره می‌شوند.
                 *
                 * CPU/GPU/Battery هم در صورت وجود
                 * داخل sensors قرار می‌گیرند.
                 */

                sensors[
                    sensor.name
                ] =
                    value


                /*
                 * CPU اصلی
                 */

                if (
                    cpuSensor != null &&
                    sensor.name.equals(
                        cpuSensor.name,
                        true
                    )
                ) {

                    cpu =
                        value

                    cpuSensorName =
                        sensor.name

                }


                /*
                 * GPU اصلی
                 */

                if (
                    gpuSensor != null &&
                    sensor.name.equals(
                        gpuSensor.name,
                        true
                    )
                ) {

                    gpu =
                        value

                    gpuSensorName =
                        sensor.name

                }


                /*
                 * Battery
                 */

                if (
                    batterySensor != null &&
                    sensor.name.equals(
                        batterySensor.name,
                        true
                    )
                ) {

                    battery =
                        value

                }


            } catch (_: Exception) {

                // سنسور مشکل‌دار نادیده گرفته می‌شود.

            }

        }


        return Sample(

            time =
                System.currentTimeMillis(),


            date =
                getCurrentDate(),


            cpu =
                cpu,


            cpuSensorName =
                cpuSensorName,


            gpu =
                gpu,


            gpuSensorName =
                gpuSensorName,


            battery =
                battery,


            sensors =
                sensors.toSortedMap()

        )

    }


// --------------------------------------------------
// Sensor Detection
// --------------------------------------------------

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
            name.lowercase()


        return when {

            /*
             * CPU
             */

            n.contains("cpu") ->

                SensorType.CPU


            n.contains("mtktscpu") ->

                SensorType.CPU


            n.contains("cluster") ->

                SensorType.CPU


            n.contains("big") ->

                SensorType.CPU


            n.contains("little") ->

                SensorType.CPU


            /*
             * GPU
             */

            n.contains("gpu") ->

                SensorType.GPU


            n.contains("kgsl") ->

                SensorType.GPU


            n.contains("gpuss") ->

                SensorType.GPU


            n.contains("adreno") ->

                SensorType.GPU


            /*
             * Battery
             */

            n.contains("battery") ->

                SensorType.BATTERY


            n.contains("bms") ->

                SensorType.BATTERY


            n.contains("bat") ->

                SensorType.BATTERY


            else ->

                SensorType.UNKNOWN

        }

    }


// --------------------------------------------------
// Safe File Reading
// --------------------------------------------------

    private fun readFileSafe(
        file: File
    ): String? {

        return try {

            if (!file.exists())
                return null


            file.readText()
                .trim()


        } catch (_: Exception) {

            null

        }

    }


// --------------------------------------------------
// Temperature Reading
// --------------------------------------------------

    private fun readTemperature(
        file: File
    ): Float? {

        val text =
            readFileSafe(
                file
            )
                ?: return null


        var value =
            text.toFloatOrNull()
                ?: return null


        /*
         * اکثر thermal zone ها مقدار را
         * به صورت milli-degree Celsius می‌دهند.
         *
         * مثال:
         *
         * 42600 -> 42.6
         */

        if (value > 1000f) {

            value /= 1000f

        }


        return value

    }


}
