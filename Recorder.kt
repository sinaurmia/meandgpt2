package com.example.meandgpt2

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

class Recorder(
    private val thermalReader: ThermalReader,
    private val storage: Storage,
    private val statistics: Statistics
) {

    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    @Volatile
    private var interval = 5000L

    @Volatile
    private var running = false

    private val lock = Object()


    fun start(){

        synchronized(lock){

            if(running)
                return

            statistics.reset()

            running=true

            executor.execute {
                recordLoop()
            }

        }

    }



    private fun recordLoop(){

        while(running){

            try{

                val sample =
                    thermalReader.readAll()


                synchronized(lock){

                    statistics.update(sample)

                    storage.add(sample)

                }


                android.util.Log.d(
                    "RECORDER_DATA",
                    "Captured: ${storage.count()}"
                )


            }catch(e:Exception){

                android.util.Log.e(
                    "RECORDER_ERROR",
                    e.message ?: ""
                )

            }



            synchronized(lock){

                try{

                    lock.wait(interval)

                }catch(_:Exception){}

            }


        }

    }



    fun stop(){

        synchronized(lock){

            running=false

            lock.notifyAll()

        }


        storage.saveNow()

    }



    fun setInterval(milliseconds:Long){

        synchronized(lock){

            interval=milliseconds

            lock.notifyAll()

        }

    }



    fun isRunning():Boolean{

        return running

    }

}