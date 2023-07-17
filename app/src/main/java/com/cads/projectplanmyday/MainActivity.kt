package com.cads.projectplanmyday

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.textclassifier.TextClassifier
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.cads.projectplanmyday.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader

@RequiresApi(Build.VERSION_CODES.O)

class MainActivity : AppCompatActivity(),TextClassifierHelper.ClassifierListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var textClassifierHelper : TextClassifierHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        val appContext = applicationContext // or use "this" if you're inside an Activity
        textClassifierHelper = TextClassifierHelper(
            context = applicationContext,
            classifierListener = this
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startTraining()
    }

    private fun startTraining(){
        readJsonFileFromInternalStorage(applicationContext , "usertaskinfo.txt"){ todoList->
            Log.d("VIEW MODEL", todoList.size.toString())
            if (todoList.size > 100) {
                Log.d("Main Activity", "Training model")
                val labelsList = todoList.map { it.isSelected }
                val tasks = todoList.map { it.title }
                textClassifierHelper.startTraining(tasks, labelsList)
            } else {
                Log.d("Main Activity", "Model cannot be trained - very few datasets")
            }
        }
    }

    override fun onError(error: String) {
//        TODO("Not yet implemented")
        textClassifierHelper.close()
        Log.d("Main Activity" , "Error in the classifier helper")
    }

    override fun onResults(results: List<Float>?, inferenceTime: Long) {
//        TODO("Not yet implemented")
        textClassifierHelper.close()
        Log.d("Main Activity" , "Predicted")

    }

    override fun onLossResults(lossNumber: String) {
//        TODO("Not yet implemented")
        textClassifierHelper.close()
        Log.d("Main Activity" , "Model trained with loss values: $lossNumber")
    }

    companion object{
         fun writeData( context:Context,newData:List<TodoTask>, fileName: String) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val fileDirectory = context.filesDir
                    val file = File(fileDirectory, fileName)
                    if (!file.exists()) {
                        Log.d("Main Activity","Creating new user info file")
                        file.createNewFile() ;
                    }

                    val writer = BufferedWriter(FileWriter(file, true))

                    newData.forEach { (title, isSelected) ->
                        writer.append("$title, $isSelected")
                        writer.newLine()
                    }

                    writer.close()

                } catch (e: IOException) {
                    Log.e("Main Activity", "Error writing data")
                }

            }
        }
        fun readJsonFileFromInternalStorage(context:Context,fileName: String, callback:(List<TodoTask>) -> Unit) {

            GlobalScope.launch(Dispatchers.IO){
                var todoList = mutableListOf<TodoTask>()
                try {
                    val fileDirectory = context.filesDir
                    val file = File(fileDirectory, fileName)

                    if (file.exists()) {
                        file.useLines { lines ->
                            lines.forEach { line->
                                val parts = line.split(",").map {line.trim() }
                                if(parts.size ==2){
                                    todoList.add(TodoTask(parts[0] , parts[1].toIntOrNull()?:0))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                launch(Dispatchers.Main) {
                    callback(todoList)
                }
            }
        }

    }
data class TodoTask(
    val title: String, val isSelected : Int
)
}