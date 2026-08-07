
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

        private const val KEY_SELECTED_SENSORS =
            "selected_sensors"

        private const val KEY_BATTERY_PERCENTAGE =
            "battery_percentage"

    }


    // --------------------------------------------------
    // CPU SENSOR
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
    // GPU SENSOR
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
    // REFRESH RATE
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
    // SENSOR CACHE
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
    // SELECTED SENSORS
    // --------------------------------------------------

    fun saveSelectedSensors(
        sensors: Collection<String>
    ) {

        prefs.edit()
            .putStringSet(
                KEY_SELECTED_SENSORS,
                sensors.toSet()
            )
            .apply()

    }


    fun getSelectedSensors(): Set<String> {

        return prefs.getStringSet(
            KEY_SELECTED_SENSORS,
            emptySet()
        )
            ?.toSet()
            ?: emptySet()

    }


    fun isSensorSelected(
        name: String
    ): Boolean {

        return getSelectedSensors()
            .contains(name)

    }


    // --------------------------------------------------
    // BATTERY PERCENTAGE
    // --------------------------------------------------

    fun saveBatteryPercentageEnabled(
        enabled: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                KEY_BATTERY_PERCENTAGE,
                enabled
            )
            .apply()

    }


    fun isBatteryPercentageEnabled(): Boolean {

        return prefs.getBoolean(
            KEY_BATTERY_PERCENTAGE,
            false
        )

    }


    // --------------------------------------------------
    // CLEAR SENSOR SETTINGS
    // --------------------------------------------------

    fun clearSensorSettings() {

        prefs.edit()
            .remove(KEY_SELECTED_SENSORS)
            .remove(KEY_BATTERY_PERCENTAGE)
            .remove(KEY_CPU_SENSOR)
            .remove(KEY_GPU_SENSOR)
            .apply()

    }


    // --------------------------------------------------
    // CLEAR SENSOR CACHE
    // --------------------------------------------------

    fun clearSensorCache() {

        prefs.edit()
            .remove(KEY_SENSOR_LIST)
            .apply()

    }

}

