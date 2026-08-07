package com.example.meandgpt2

import android.content.Context

class ThermalSettings(
    context: Context
) {


    private val prefs =
        context.getSharedPreferences(
            "thermal_settings",
            Context.MODE_PRIVATE
        )



    companion object {

        const val AUTO =
            "AUTO"

        private const val CPU_SENSOR =
            "cpu_sensor"

        private const val GPU_SENSOR =
            "gpu_sensor"

        private const val REFRESH_RATE =
            "refresh_rate"

    }



    fun setCpuSensor(name: String?) {

        prefs.edit()
            .putString(
                CPU_SENSOR,
                name ?: AUTO
            )
            .apply()

    }



    fun getCpuSensor(): String {

        return prefs.getString(
            CPU_SENSOR,
            AUTO
        ) ?: AUTO

    }



    fun setGpuSensor(name: String?) {

        prefs.edit()
            .putString(
                GPU_SENSOR,
                name ?: AUTO
            )
            .apply()

    }



    fun getGpuSensor(): String {

        return prefs.getString(
            GPU_SENSOR,
            AUTO
        ) ?: AUTO

    }



    fun setRefreshRate(
        value: Long
    ) {

        prefs.edit()
            .putLong(
                REFRESH_RATE,
                value
            )
            .apply()

    }



    fun getRefreshRate(): Long {

        return prefs.getLong(
            REFRESH_RATE,
            5000L
        )

    }


}