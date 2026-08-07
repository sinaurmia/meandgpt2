package com.example.meandgpt2

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(
    private val context: Context
) {

    fun export(records: List<Sample>): File? {

        return try {

            val folder =
                context.getExternalFilesDir(null)
                    ?: return null

            if (!folder.exists()) {
                folder.mkdirs()
            }

            val fileName =
                "log_" +
                        SimpleDateFormat(
                            "yyyy-MM-dd_HH-mm-ss",
                            Locale.ROOT
                        ).format(Date()) +
                        ".csv"

            val exportFile =
                File(folder, fileName)

            exportFile.bufferedWriter(Charsets.UTF_8).use { writer ->

                // UTF-8 BOM
                writer.write("\uFEFF")

                writer.appendLine(
                    "time,date,cpu,cpu_sensor,gpu,gpu_sensor,battery,sensors"
                )

                for (sample in records) {

                    val sensorsText =
                        sample.sensors.entries.joinToString("|") {
                            "${it.key}:${it.value}"
                        }

                    val line =
                        listOf(

                            sample.time,

                            sample.date,

                            sample.cpu ?: "",

                            sample.cpuSensorName ?: "",

                            sample.gpu ?: "",

                            sample.gpuSensorName ?: "",

                            sample.battery ?: "",

                            sensorsText

                        ).joinToString(",") {

                            escapeCsv(it.toString())

                        }

                    writer.appendLine(line)

                }

            }

            exportFile

        } catch (e: Exception) {

            e.printStackTrace()

            null

        }

    }

    private fun escapeCsv(value: String): String {

        return if (

            value.contains(",") ||
            value.contains("\"") ||
            value.contains("\n")

        ) {

            "\"" +
                    value.replace("\"", "\"\"") +
                    "\""

        } else {

            value

        }

    }

}