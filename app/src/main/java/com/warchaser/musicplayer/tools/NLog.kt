package com.warchaser.musicplayer.tools

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.warchaser.musicplayer.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object NLog {

    private val DELETE_DAYS_DURATION : Int = 5
    private val LINE_SEPARATOR : String = System.getProperty("line.separator")

    private val IS_DEBUG : Boolean = BuildConfig.DEBUG
    private val ALLOW_WRITE_LOG : Boolean = BuildConfig.DEBUG

    private val JSON_RESULT : String = "<--[NetWork Response Result]-->"
    private var mLogPath : String = ""
    private val FATAL : String = "FATAL"

    /**
     * 今天日期
     */
    private var mTodayDate : Date? = null

    /**
     * 日期格式
     * 到日
     */
    private val DATE_FORMAT_DAY : SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * 日期格式
     * 到秒
     */
    private val DATE_FORMAT_SS : SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)


    /**
     * 初始化
     *
     * 写Log文件
     */
    fun initLogFile(context: Context){
        mLogPath = if(Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()){
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath + File.separator + "Log" + File.separator
        } else {
            context.filesDir.absolutePath + File.separator + "Log" + File.separator
        }

        val file = File(mLogPath)
        if(!file.exists()){
            file.mkdirs()
        }

        mTodayDate = Date()

        Thread(Runnable {
            try {
                deleteOldFiles(getFiles(mLogPath), mTodayDate)
            } catch (e: Exception) {
                printStackTrace("DELETE_LOGS", e)
            } catch (e: Error) {
                printStackTrace("DELETE_LOGS", e)
            }
        }).start()
    }

    @Synchronized
    @JvmStatic
    fun i(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.i(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun i(tag: String, format: String, vararg objects: Any) {
        i(tag, String.format(format, *objects))
    }

    @Synchronized
    @JvmStatic
    fun e(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.e(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun d(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.d(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun d(tag: String, format: String, vararg objects: Any){
        d(tag, String.format(format, objects))
    }

    @Synchronized
    @JvmStatic
    fun v(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.v(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun w(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.w(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun wtf(tag: String, msg: String){
        takeIf { IS_DEBUG }.apply { Log.wtf(tag, msg) }
    }

    @Synchronized
    @JvmStatic
    fun eWithFile(tag: String, msg: String){
        e(tag, msg)
        writeLog2File(tag, msg)
    }

    @JvmStatic
    private fun getLogPath() : String{
        return mLogPath
    }

    @JvmStatic
    private fun getFiles(path: String) : Array<File>?{
        val file = File(path)
        return if(!file.exists()){
            null
        } else {
            file.listFiles()
        }
    }

    @Throws(Exception::class)
    private fun deleteOldFiles(files: Array<File>?, todayDate: Date?){
        if(files == null || files.isEmpty() || todayDate == null){
            return
        }

        for (file in files){
            val fileName = file.name
            val realFileName = fileName.substring(0, fileName.lastIndexOf("."))
            if(daysBetween(DATE_FORMAT_DAY.parse(realFileName), todayDate) >= DELETE_DAYS_DURATION){
                file.delete()
            }
        }

    }

    private fun daysBetween(date1: Date, data2: Date) : Int{
        val cal = Calendar.getInstance()
        cal.time = date1
        val time1 = cal.timeInMillis
        cal.time = data2
        val time2 = cal.timeInMillis
        val daysBetween = (time2 - time1) / (1000 * 3600 * 24)
        return daysBetween.toInt()
    }

    @Synchronized
    @JvmStatic
    fun printStackTrace(tag: String, e: Throwable){
        if(IS_DEBUG){
            e(tag, FATAL)
            e(tag, e.javaClass.name)
            val message = e.message
            message?.run {
                e(tag, message)
            }
            for(string in e.stackTrace){
                e(tag, string.toString())
            }
        } else  {
            e.printStackTrace()
        }

        takeIf { ALLOW_WRITE_LOG }.apply {
            writeLog2File(tag, FATAL)
            writeLog2File(tag, e.javaClass.name)
            val message = e.message
            if (message != null) {
                writeLog2File(tag, message)
            }

            for (string in e.stackTrace) {
                writeLog2File(tag, string.toString())
            }

            val cause = e.cause
            if (cause != null) {
                for (string in cause.stackTrace) {
                    writeLog2File(tag, string.toString())
                }
            }
        }
    }

    /**
     * 将Throwable打印至logcat并根据开关写文件
     */
    @JvmStatic
    fun writeLog2File(tag: String, msg: String){
        if(TextUtils.isEmpty(getLogPath())){
            e(tag, "LOG_PATH is empty!")
            return
        }

        val file = File(getLogPath())
        if(!file.exists()){
            file.mkdirs()
        }

        val fileName = getLogPath() + DATE_FORMAT_DAY.format(mTodayDate) + ".log"
        var fos : FileOutputStream? = null
        var bw : BufferedWriter? = null
        val time : String = DATE_FORMAT_SS.format(Date())
        try {
            fos = FileOutputStream(fileName, true)
            bw = BufferedWriter(OutputStreamWriter(fos))
            bw.write("$time ")
            bw.write("$tag: ")
            bw.write(msg)
            bw.write(LINE_SEPARATOR)
        } catch (e : Exception) {
            e.printStackTrace()
        } catch (e : Error){
            e.printStackTrace()
        } finally {
            try {
                bw?.close()
                fos?.close()
            } catch (e : Exception) {
                e.printStackTrace()
            } catch (e : Error){
                e.printStackTrace()
            }
        }
    }

    /**
     * 打印json至logcat
     */
    @JvmStatic
    fun printJson(tag: String, msg: String){
        var message : String
        try {
            message = when {
                msg.startsWith("{") -> {
                    val jsonObject = JSONObject(msg)
                    //最重要的方法,就一行,返回格式化的json字符串,其中的数字4是缩进字符数
                    jsonObject.toString(4)
                }
                msg.startsWith("[") -> {
                    val jsonArray = JSONArray(msg)
                    jsonArray.toString(4)
                }
                else -> {
                    msg
                }
            }
        } catch (e : Exception) {
            e.printStackTrace()
            message = msg
        } catch (e : Error) {
            message = msg
        }

        message = JSON_RESULT + LINE_SEPARATOR + message
        val lines : Array<String> = message.split(LINE_SEPARATOR).toTypedArray()
        for (line in lines){
            e(tag, line)
        }
    }

    /**
     * 打印Html
     * */
    @JvmStatic
    @Synchronized
    fun printHtml(tag : String, msg : String){
        if(TextUtils.isEmpty(msg) || !IS_DEBUG){
            return
        }

        val lines : List<String> = msg.split("\r\n")
        for(line : String in lines){
            e(tag, line)

            //写Log
            if(ALLOW_WRITE_LOG){
                writeLog2File(tag, line)
            }
        }
    }

}