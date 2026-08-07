package com.example.meandgpt2

import android.content.Context
import java.io.*
import java.util.concurrent.atomic.AtomicInteger


class Storage(
    private val context: Context
) {


    private val records =
        mutableListOf<Sample>()


    private val lock = Any()


    private val saveFile: File
        get() = File(
            context.filesDir,
            "temperature_records.dat"
        )


    private val tempFile: File
        get() = File(
            context.filesDir,
            "temperature_records.tmp"
        )


    private val pendingChanges =
        AtomicInteger(0)


    private val saveEvery =
        10



    init {

        load()

    }




    fun add(sample: Sample) {

        synchronized(lock) {

            records.add(sample)

            if(records.size > 50000)
                records.removeAt(0)

            if(
                pendingChanges.incrementAndGet()
                >= saveEvery
            ){

                saveLocked()

                pendingChanges.set(0)

            }

        }

    }




    fun addAndSave(sample: Sample) {


        synchronized(lock) {


            records.add(sample)

            saveLocked()

            pendingChanges.set(0)


        }


    }





    fun getAll(): List<Sample> {


        synchronized(lock) {


            return ArrayList(records)


        }


    }





    fun getLast(): Sample? {


        synchronized(lock) {


            return records.lastOrNull()


        }


    }





    fun count(): Int {


        synchronized(lock) {


            return records.size


        }


    }





    fun isEmpty(): Boolean {


        synchronized(lock) {


            return records.isEmpty()


        }


    }





    fun clear() {


        synchronized(lock) {


            records.clear()

            saveLocked()

            pendingChanges.set(0)


        }


    }





    fun removeLast(){

        synchronized(lock){

            if(records.isNotEmpty()){

                records.removeAt(
                    records.size-1
                )

                saveLocked()

                pendingChanges.set(0)

            }

        }

    }





    fun saveNow(){


        synchronized(lock){


            saveLocked()

            pendingChanges.set(0)


        }


    }





    private fun saveLocked(){


        try {


            ObjectOutputStream(
                tempFile.outputStream()
            ).use {


                it.writeObject(
                    ArrayList(records)
                )


            }



            if(saveFile.exists()){

                saveFile.delete()

            }



            tempFile.renameTo(saveFile)



        }
        catch(e:Exception){


            android.util.Log.e(
                "STORAGE",
                "Save failed: ${e.message}"
            )


        }


    }






    @Suppress("UNCHECKED_CAST")
    private fun load(){


        synchronized(lock){


            if(!saveFile.exists())
                return



            try{


                ObjectInputStream(
                    saveFile.inputStream()
                ).use {


                    val data =
                        it.readObject()



                    if(data is ArrayList<*>){


                        records.clear()


                        records.addAll(
                            data as ArrayList<Sample>
                        )


                    }


                }


                android.util.Log.d(
                    "STORAGE",
                    "Loaded ${records.size} records"
                )



            }
            catch(e:Exception){


                android.util.Log.e(
                    "STORAGE",
                    "Load failed"
                )


                records.clear()


            }


        }


    }






    fun getLastRecords(
        limit:Int
    ):List<Sample>{


        synchronized(lock){


            if(records.isEmpty())
                return emptyList()



            val start =
                maxOf(
                    0,
                    records.size-limit
                )



            return records
                .subList(
                    start,
                    records.size
                )
                .reversed()


        }


    }



}