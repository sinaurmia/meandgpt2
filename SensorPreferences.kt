package com.example.meandgpt2

import android.content.Context

class SensorPreferences(context: Context) {

    
    private val prefs =
        context.getSharedPreferences(
            "sensor_preferences",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val KEY_CPU_SENSOR =
            "cpu_sensor"

        private const val KEY_GPU_SENSOR =
            "gpu_sensor"

        private const val KEY_REFRESH_RATE =
            "refresh_rate"

        private const val KEY_SENSOR_LIST =
            "sensor_list"

        /*
         * سنسورهایی که کاربر برای نمایش/ثبت انتخاب کرده است.
         *
         * فقط سنسورهای داخل این لیست باید توسط برنامه
         * در حالت عادی خوانده و ذخیره شوند.
         */
        private const val KEY_ENABLED_SENSORS =
            "enabled_sensors"
    }


// --------------------------------------------------
// CPU Sensor
// --------------------------------------------------

    fun saveCpuSensor(name: String?) {

        prefs.edit()
            .putString(
                KEY_CPU_SENSOR,
                name
            )
            .apply()
    }


    fun getCpuSensor(): String? {

        return prefs.getString(
            KEY_CPU_SENSOR,
            null
        )
    }


// --------------------------------------------------
// GPU Sensor
// --------------------------------------------------

    fun saveGpuSensor(name: String?) {

        prefs.edit()
            .putString(
                KEY_GPU_SENSOR,
                name
            )
            .apply()
    }


    fun getGpuSensor(): String? {

        return prefs.getString(
            KEY_GPU_SENSOR,
            null
        )
    }


// --------------------------------------------------
// Refresh Rate
// --------------------------------------------------

    fun saveRefreshRate(
        interval: Long
    ) {

        prefs.edit()
            .putLong(
                KEY_REFRESH_RATE,
                interval
            )
            .apply()
    }


    fun getRefreshRate(): Long {

        return prefs.getLong(
            KEY_REFRESH_RATE,
            5000L
        )
    }


// --------------------------------------------------
// Sensor List Cache
// --------------------------------------------------

    fun saveSensorList(
        sensors: Collection<String>
    ) {

        prefs.edit()
            .putStringSet(
                KEY_SENSOR_LIST,
                sensors.toSet()
            )
            .apply()
    }


    fun getSensorList(): List<String> {

        return prefs.getStringSet(
            KEY_SENSOR_LIST,
            emptySet()
        )
            ?.toList()
            ?.sorted()
            ?: emptyList()
    }


    fun hasSensorCache(): Boolean {

        return prefs.contains(
            KEY_SENSOR_LIST
        )
    }


// --------------------------------------------------
// Enabled Sensors
// --------------------------------------------------

    /*
     * ذخیره سنسورهایی که کاربر انتخاب کرده است.
     */
    fun saveEnabledSensors(
        sensors: Collection<String>
    ) {

        prefs.edit()
            .putStringSet(
                KEY_ENABLED_SENSORS,
                sensors.toSet()
            )
            .apply()
    }


    /*
     * دریافت سنسورهای انتخاب‌شده.
     *
     * اگر هنوز هیچ انتخابی انجام نشده باشد،
     * یک لیست خالی برمی‌گردد.
     */
    fun getEnabledSensors(): Set<String> {

        return prefs.getStringSet(
            KEY_ENABLED_SENSORS,
            emptySet()
        )
            ?.toSet()
            ?: emptySet()
    }


    /*
     * بررسی اینکه یک سنسور فعال شده یا نه.
     */
    fun isSensorEnabled(
        sensorName: String
    ): Boolean {

        return getEnabledSensors()
            .contains(sensorName)
    }


    /*
     * فعال یا غیرفعال کردن یک سنسور.
     */
    fun setSensorEnabled(
        sensorName: String,
        enabled: Boolean
    ) {

        val sensors =
            getEnabledSensors()
                .toMutableSet()


        if (enabled) {

            sensors.add(
                sensorName
            )

        } else {

            sensors.remove(
                sensorName
            )

        }


        saveEnabledSensors(
            sensors
        )
    }


    /*
     * حذف تمام انتخاب‌های سنسورها.
     */
    fun clearEnabledSensors() {

        prefs.edit()
            .remove(
                KEY_ENABLED_SENSORS
            )
            .apply()
    }


// --------------------------------------------------
// Clear Sensor Settings
// --------------------------------------------------

    fun clearSensorCache() {

        prefs.edit()
            .remove(
                KEY_SENSOR_LIST
            )
            .remove(
                KEY_CPU_SENSOR
            )
            .remove(
                KEY_GPU_SENSOR
            )
            .remove(
                KEY_ENABLED_SENSORS
            )
            .apply()
    }


}
