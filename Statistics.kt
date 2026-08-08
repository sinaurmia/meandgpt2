package com.example.meandgpt2

class Statistics {

    var cpuMin: Float? = null
        private set

    var cpuMax: Float? = null
        private set

    var cpuAverage: Float? = null
        private set

    var gpuMin: Float? = null
        private set

    var gpuMax: Float? = null
        private set

    var gpuAverage: Float? = null
        private set

    var batteryMin: Float? = null
        private set

    var batteryMax: Float? = null
        private set

    var batteryAverage: Float? = null
        private set

    private var cpuSum = 0.0
    private var cpuCount = 0L

    private var gpuSum = 0.0
    private var gpuCount = 0L

    private var batterySum = 0.0
    private var batteryCount = 0L

    fun update(sample: Sample) {

        sample.cpu?.let { value ->
            if (cpuMin == null || value < cpuMin!!)
                cpuMin = value

            if (cpuMax == null || value > cpuMax!!)
                cpuMax = value

            cpuSum += value
            cpuCount++

            cpuAverage =
                (cpuSum / cpuCount).toFloat()
        }

        sample.gpu?.let { value ->
            if (gpuMin == null || value < gpuMin!!)
                gpuMin = value

            if (gpuMax == null || value > gpuMax!!)
                gpuMax = value

            gpuSum += value
            gpuCount++

            gpuAverage =
                (gpuSum / gpuCount).toFloat()
        }

        sample.battery?.let { value ->
            if (
                batteryMin == null ||
                value < batteryMin!!
            )
                batteryMin = value

            if (
                batteryMax == null ||
                value > batteryMax!!
            )
                batteryMax = value

            batterySum += value
            batteryCount++

            batteryAverage =
                (batterySum / batteryCount).toFloat()
        }
    }

    fun reset() {
        cpuMin = null
        cpuMax = null
        cpuAverage = null
        cpuSum = 0.0
        cpuCount = 0L

        gpuMin = null
        gpuMax = null
        gpuAverage = null
        gpuSum = 0.0
        gpuCount = 0L

        batteryMin = null
        batteryMax = null
        batteryAverage = null
        batterySum = 0.0
        batteryCount = 0L
    }
}