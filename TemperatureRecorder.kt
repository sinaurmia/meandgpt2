package com.example.meandgpt2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log


class TemperatureRecorder(
    context: Context,
    private val interval: Long = 5000
) {


    private val thermalReader =
        ThermalReader(context)


    private val storage =
        Storage(context)


    private val handler =
        Handler(Looper.getMainLooper())


    private var running = false



    private val task = object : Runnable {

        override fun run() {

            if (!running)
                return


            try {

                val sample =
                    thermalReader.readAll()


                storage.addAndSave(sample)


                Log.d(
                    "RECORDER_DATA",
                    "Saved records: ${storage.count()}"
                )


            } catch (e: Exception) {

                Log.e(
                    "RECORDER_ERROR",
                    e.message ?: "unknown error"
                )

            }


            handler.postDelayed(
                this,
                interval
            )

        }

    }



    fun start() {

        if (running)
            return


        running = true


        Log.d(
            "RECORDER_TEST",
            "Recorder started"
        )


        handler.post(task)

    }



    fun stop() {

        running = false


        handler.removeCallbacks(task)


        Log.d(
            "RECORDER_TEST",
            "Recorder stopped"
        )

    }



    fun isRunning(): Boolean {

        return running

    }



    fun getCount(): Int {

        return storage.count()

    }


    fun getSamples(): List<Sample> {

        return storage.getAll()

    }

}