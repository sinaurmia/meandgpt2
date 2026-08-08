package com.example.meandgpt2

import java.io.Serializable

data class Sample(
    val time: Long,
    val date: String,
    val cpu: Float?,
    val cpuSensorName: String?,
    val gpu: Float?,
    val gpuSensorName: String?,
    val battery: Float?,
    val batteryPercentage: Int?,
    val sensors: MutableMap<String, Float>
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

