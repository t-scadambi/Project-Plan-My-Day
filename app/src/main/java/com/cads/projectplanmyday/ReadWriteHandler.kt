package com.cads.projectplanmyday

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.O)
object ReadWriteHandler {
    private const val FILE_NAME = "eventsData.txt"
    private val gson = Gson()

    fun saveDataToFile(context: Context, data: List<Event>) {
        val jsonData = gson.toJson(data)
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val writer = file.bufferedWriter()
            writer.use {
                it.write(jsonData)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadDataFromFile(context: Context): List<Event>? {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val jsonData = file.bufferedReader().use {
                it.readText()
            }
            val eventType = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(jsonData, eventType)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}