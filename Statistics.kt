package com.example.meandgpt2


class Statistics {


    var cpuMin: Float? = null
        private set

    var cpuMax: Float? = null
        private set


    var gpuMin: Float? = null
        private set

    var gpuMax: Float? = null
        private set


    var batteryMin: Float? = null
        private set

    var batteryMax: Float? = null
        private set



    fun update(sample: Sample) {


        sample.cpu?.let { value ->


            if (cpuMin == null || value < cpuMin!!)
                cpuMin = value


            if (cpuMax == null || value > cpuMax!!)
                cpuMax = value


        }



        sample.gpu?.let { value ->


            if (gpuMin == null || value < gpuMin!!)
                gpuMin = value


            if (gpuMax == null || value > gpuMax!!)
                gpuMax = value


        }



        sample.battery?.let { value ->


            if (batteryMin == null || value < batteryMin!!)
                batteryMin = value


            if (batteryMax == null || value > batteryMax!!)
                batteryMax = value


        }


    }



    fun reset() {


        cpuMin = null
        cpuMax = null


        gpuMin = null
        gpuMax = null


        batteryMin = null
        batteryMax = null


    }


}