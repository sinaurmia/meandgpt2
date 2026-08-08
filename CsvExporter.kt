package com.example.meandgpt2

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(
    private val context: Context
) {

    fun export(
        records: List<Sample>
    ): File? {

        return try {
            val folder =
                context.getExternalFilesDir(null)
                    ?: return null

            if (!folder.exists())
                folder.mkdirs()

            val fileName =
                "log_" +
                        SimpleDateFormat(
                            "yyyy-MM-dd_HH-mm-ss",
                            Locale.ROOT
                        ).format(Date()) +
                        ".csv"

            val exportFile =
                File(folder, fileName)

            val preferences =
                SensorPreferences(context)

            val selectedSensors =
                preferences
                    .getSelectedSensors()
                    .sorted()

            val batteryPercentageEnabled =
                preferences
                    .isBatteryPercentageEnabled()

            exportFile
                .bufferedWriter(Charsets.UTF_8)
                .use { writer ->

                    // UTF-8 BOM
                    writer.write("\uFEFF")

                    val headers =
                        mutableListOf<String>()

                    headers.add("time")
                    headers.add("date")
                    headers.add("cpu")
                    headers.add("cpu_sensor")
                    headers.add("gpu")
                    headers.add("gpu_sensor")
                    headers.add("battery")

                    if (batteryPercentageEnabled) {
                        headers.add(
                            "battery_percentage"
                        )
                    }

                    headers.addAll(
                        selectedSensors
                    )

                    writer.appendLine(
                        headers.joinToString(",") {
                            escapeCsv(it)
                        }
                    )

                    for (sample in records) {

                        val values =
                            mutableListOf<String>()

                        values.add(
                            sample.time.toString()
                        )

                        values.add(
                            sample.date
                        )

                        values.add(
                            sample.cpu?.toString()
                                ?: ""
                        )

                        values.add(
                            sample.cpuSensorName
                                ?: ""
                        )

                        values.add(
                            sample.gpu?.toString()
                                ?: ""
                        )

                        values.add(
                            sample.gpuSensorName
                                ?: ""
                        )

                        values.add(
                            sample.battery?.toString()
                                ?: ""
                        )

                        if (batteryPercentageEnabled) {
                            values.add(
                                sample.batteryPercentage
                                    ?.toString()
                                    ?: ""
                            )
                        }

                        for (sensorName in selectedSensors) {
                            values.add(
                                sample.sensors[
                                    sensorName
                                ]?.toString()
                                    ?: ""
                            )
                        }

                        writer.appendLine(
                            values.joinToString(",") {
                                escapeCsv(it)
                            }
                        )
                    }
                }

            exportFile

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(
        value: String
    ): String {

        return if (
            value.contains(",") ||
            value.contains("\"") ||
            value.contains("\n")
        ) {
            "\"" +
                    value.replace(
                        "\"",
                        "\"\""
                    ) +
                    "\""
        } else {
            value
        }
    }
}

