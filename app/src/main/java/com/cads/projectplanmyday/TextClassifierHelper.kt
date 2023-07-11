package com.cads.projectplanmyday

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RequiresApi(api = Build.VERSION_CODES.O)
class TextClassifierHelper(private val context: Context, private val classifierListener: ClassifierListener?) {
    private var interpreter:Interpreter? = null
    /** Executor to run inference task in the background. */
    private var executor: ExecutorService? = null

    /**This lock guarantees that only one thread is performing training and
    inference at any point in time.*/
    private val lock = Any()
    private var maxlen: Int = 0 // will be inferred from TF Lite model.
    private val handler = Handler(Looper.getMainLooper())
    private val mappedSentences : MutableList<IntArray> = mutableListOf()
    private val labels : MutableList<IntArray> = mutableListOf()
    private var wordIndexMap :Map<String,Int>? = null
    init {
        if (setupModelPersonalization()) {
            maxlen = interpreter!!.getInputTensor(0).shape()[1]
//            Log.d(TAG, interpreter!!.getInputTensorFromSignature("x","train").numBytes().toString())
        } else {
            classifierListener?.onError("TFLite failed to init.")
        }
    }
    fun close() {
        executor?.shutdownNow()
        executor = null
        interpreter = null
    }

    fun pauseTraining() {
        executor?.shutdownNow()
    }

    private fun setupModelPersonalization(): Boolean {
        return try {
            val assetManager = context.assets
            val modelFile = loadModelFile(assetManager, "model.tflite")
            interpreter = Interpreter(modelFile)
            restore()
            true
        } catch (e: IOException) {
            classifierListener?.onError(
                "Model personalization failed to " +
                        "initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
            false
        }
    }
    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun addTasks(trainingData: List<TaskPlanner>){
        mappedSentences.clear()
        labels.clear()
        wordIndexMap = readWordIndexMappingFromAssets(context ,"tokenizer.json")
        val sentences :MutableList<String> = mutableListOf()
        for(todoTask in trainingData){
            sentences.add(todoTask.title)
        }
        val trainingLabels : MutableList<Boolean> = mutableListOf()
        for(todoTask in trainingData){
            trainingLabels.add(todoTask.isSelected)
        }
        mapSentencesToNumbers(sentences,wordIndexMap!!,maxlen)
        getCorresLabels(trainingLabels)
    }
    fun getCorresLabels(trainingLabels: List<Boolean>){
        var index=0
        for(isSelected in trainingLabels){
            val willDo  = when(isSelected){
                true -> listOf<Int>(1)
                else -> listOf(0)
            }
            labels.add(willDo.toIntArray())
        }
    }
    fun mapSentencesToNumbers(
        sentences: List<String>,
        wordIndexMap: Map<String, Int>,
        maxLen: Int
    ) {
        for (sentence in sentences) {
            val words = sentence.split(" ")
            val mappedWords = words.map { wordIndexMap.getOrDefault(it, 0) }
            val paddedWords = mappedWords.takeLast(maxLen).toMutableList()
            while (paddedWords.size < maxLen) {
                paddedWords.add(0, 0) // Add padding at the beginning
            }
            mappedSentences.add(paddedWords.toIntArray())
        }
    }
    fun readWordIndexMappingFromAssets(context: Context, fileName: String): Map<String, Int> {
        val jsonString = context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
        val jsonObject = JSONObject(jsonString)
        val wordIndexMap = mutableMapOf<String, Int>()
        for (key in jsonObject.keys()) {
            val value = jsonObject.getInt(key)
            wordIndexMap[key] = value
        }
        return wordIndexMap
    }
    fun startTraining(){
        if (interpreter == null) {
            setupModelPersonalization()
        }
        executor = Executors.newSingleThreadExecutor()
        executor?.execute{
            synchronized(lock){
                val loss = training(mappedSentences, labels)
                Log.d(TAG, loss)
                handler.post {
                    classifierListener?.onLossResults(1f)
                }
            }
        }
    }

    private fun training(mappedSentences: MutableList<IntArray>, labels: MutableList<IntArray>): String {
        val inputs: MutableMap<String, Any> = HashMap()
        inputs["x"] = mappedSentences.toTypedArray()
        inputs["y"] = labels.toTypedArray()
        val outputs: MutableMap<String, Any> = HashMap()
        val loss = FloatBuffer.allocate(labels.size)
        outputs["loss"] = loss
        interpreter?.runSignature(inputs, outputs, "train")
        val floatList: MutableList<Float> = mutableListOf()
        loss.rewind()
        while (loss.hasRemaining()) {
            val floatValue: Float = loss.get()
            floatList.add(floatValue)
        }
        // Export the trained weights as a checkpoint file.
        save()
        return floatList.toString()
    }
    private fun save(){
        val outputFile = File(context.filesDir, "checkpoint.ckpt")
        if (!outputFile.exists()) {
            outputFile.createNewFile() ;
            Log.d("SAVE FUNCTION","File created")
        }
        val inputs: MutableMap<String, Any> = HashMap()
        inputs["checkpoint_path"] = outputFile.absolutePath
        val outputs: MutableMap<String, Any> = HashMap()
        interpreter?.runSignature(inputs, outputs, "save")
        Log.d("SAVE FUNCTION","File saved")

    }
    private fun restore(){
        val outputFile = File(context.filesDir, "checkpoint.ckpt")
        if(outputFile.exists()) {
            val inputs: MutableMap<String, Any> = HashMap()
            inputs["checkpoint_path"] = outputFile.absolutePath
            val outputs: MutableMap<String, Any> = HashMap()
            interpreter?.runSignature(inputs, outputs, "restore")
            Log.d("RESTORE FUNCTION","File restored")
        }
    }
     fun classify(sentences :List<String>) {
         mappedSentences.clear()
         mapSentencesToNumbers(sentences,wordIndexMap!!,maxlen)
        synchronized(lock) {
            if (interpreter == null) {
                setupModelPersonalization()
            }
            // Inference time is the difference between the system time at the start and finish of the
            // process
            var inferenceTime = SystemClock.uptimeMillis()

            val inputs: MutableMap<String, Any> = HashMap()
            inputs["x"] = mappedSentences.toTypedArray()
            // Run inference with the input data.
            val outputs: MutableMap<String, Any> = HashMap()
            val output = FloatBuffer.allocate(mappedSentences.size)
            outputs["logits"] = output
            interpreter?.runSignature(inputs, outputs, "infer")
            output.rewind()

            // Post-processing
            val NUM_TESTS = mappedSentences.size
            val testLabels = mutableListOf<Float>()
            for (i in 0 until NUM_TESTS) {
                testLabels.add(output.get(i))
            }

            inferenceTime = SystemClock.uptimeMillis() - inferenceTime
            classifierListener?.onResults(testLabels, inferenceTime)
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Float>?, inferenceTime: Long)
        fun onLossResults(lossNumber: Float)
    }
    companion object {
        private const val TAG = "TextClassifier"

    }
}