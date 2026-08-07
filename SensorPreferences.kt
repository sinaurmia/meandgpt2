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


    }



    // -------------------------
    // CPU Sensor
    // -------------------------
    fun saveSensorList(list: List<String>) {

        prefs.edit()
            .putStringSet(
                KEY_SENSOR_LIST,
                list.toSet()
            )
            .apply()

    }



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



    // -------------------------
    // GPU Sensor
    // -------------------------

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



    // -------------------------
    // Refresh Rate
    // -------------------------

    fun saveRefreshRate(interval: Long) {

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



    // -------------------------
    // Sensor List Cache
    // -------------------------


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



    fun clearSensorCache() {


        prefs.edit()
            .remove(KEY_SENSOR_LIST)
            .remove(KEY_CPU_SENSOR)
            .remove(KEY_GPU_SENSOR)
            .apply()


    }


}