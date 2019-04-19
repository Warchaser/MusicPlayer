package com.warchaser.musicplayer.tools;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.warchaser.musicplayer.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NLog {

    private static final int DELETE_DAYS_DURATION = 5;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final boolean IS_DEBUG = BuildConfig.DEBUG;
    private static final boolean ALLOW_WRITE_LOG = BuildConfig.DEBUG;
    private static final String JSON_RESULT = "<--[NetWork Response Result]-->";
    private static String mLogPath = "";
    private static final String FATAL = "FATAL";

    /**
     * 今天日期
     */
    private static Date mTodayDate = null;

    /**
     * 日期格式
     * 到日
     */
    private static final SimpleDateFormat DATE_FORMAT_DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * 日期格式
     * 到秒
     */
    private static final SimpleDateFormat DATE_FORMAT_SS = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);

    /**
     * 初始化
     * 写Log文件
     */
    public static void initLogFile(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // 优先保存到SD卡中
            mLogPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()
                    + File.separator
                    + "Log"
                    + File.separator;
        } else {
            mLogPath = context.getFilesDir().getAbsolutePath()
                    + File.separator
                    + "Log"
                    + File.separator;
        }

        final File file = new File(mLogPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        mTodayDate = new Date();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    deleteOldFiles(DELETE_DAYS_DURATION, getFiles(mLogPath), mTodayDate);
                } catch (Exception | Error e) {
                    printStackTrace("DELETE_LOGS", e);
                }
            }
        }).start();
    }

    public synchronized static void i(String tag, String msg) {
        if (IS_DEBUG) {
            Log.i(tag, msg);
        }
    }

    public synchronized static void e(String tag, String msg) {
        if (IS_DEBUG) {
            Log.e(tag, msg);
        }
    }

    public synchronized static void d(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(tag, msg);
        }
    }

    public synchronized static void v(String tag, String msg) {
        if (IS_DEBUG) {
            Log.v(tag, msg);
        }
    }

    public synchronized static void w(String tag, String msg) {
        if (IS_DEBUG) {
            Log.w(tag, msg);
        }
    }

    public synchronized static void eWithFile(String tag, String msg){
        e(tag, msg);
        writeLog2File(tag, msg);
    }

    private static String getLogPath() {
        return mLogPath;
    }

    private static File[] getFiles(String path) {
        final File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        return file.listFiles();
    }

    private static int daysBetween(Date date1, Date date2) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        final long time1 = cal.getTimeInMillis();
        cal.setTime(date2);
        final long time2 = cal.getTimeInMillis();
        final long betweenDays = (time2 - time1) / (1000 * 3600 * 24);
        return Integer.parseInt(String.valueOf(betweenDays));
    }

    private static void deleteOldFiles(int days, File[] files, Date todayDate) throws Exception {
        if (files == null || files.length == 0 || todayDate == null) {
            return;
        }

        for (File file : files) {
            final String fileName = file.getName();
            final String realFileName = fileName.substring(0, fileName.lastIndexOf("."));
            if (daysBetween(DATE_FORMAT_DAY.parse(realFileName), todayDate) >= days) {
                file.delete();
            }
        }
    }

    public synchronized static void printStackTrace(String tag, Throwable e) {
        if (IS_DEBUG) {
            e(tag, FATAL);
            e(tag, e.getClass().getName());
            final String message = e.getMessage();
            if (message != null) {
                e(tag, message);
            }
            for (StackTraceElement string : e.getStackTrace()) {
                e(tag, string.toString());
            }
        } else {
            e.printStackTrace();
        }

        if (ALLOW_WRITE_LOG) {
            writeLog2File(tag, FATAL);
            writeLog2File(tag, e.getClass().getName());
            final String message = e.getMessage();
            if (message != null) {
                writeLog2File(tag, message);
            }

            for (StackTraceElement string : e.getStackTrace()) {
                writeLog2File(tag, string.toString());
            }

            final Throwable cause = e.getCause();
            if (cause != null) {
                for (StackTraceElement string : cause.getStackTrace()) {
                    writeLog2File(tag, string.toString());
                }
            }
        }
    }

    /**
     * 将Throwable打印至logcat并根据开关写文件
     */
    public static void writeLog2File(String tag, String msg) {
        if (TextUtils.isEmpty(getLogPath())) {
            e(tag, "LOG_PATH is empty!");
            return;
        }

        final File file = new File(getLogPath());
        if (!file.exists()) {
            file.mkdirs();
        }

        final String fileName = getLogPath() + DATE_FORMAT_DAY.format(mTodayDate) + ".log";
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        final String time = DATE_FORMAT_SS.format(new Date());
        try {
            fos = new FileOutputStream(fileName, true);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(time + " ");
            bw.write(tag + ": ");
            bw.write(msg);
            bw.write(LINE_SEPARATOR);
        } catch (Exception | Error e) {
            e.printStackTrace();
        } finally {
            try {
                if(bw != null){
                    bw.close();
                }

                if(fos != null){
                    fos.close();
                }
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打印json至logcat
     */
    public static void printJson(String tag, String msg) {
        String message;
        try {
            if (msg.startsWith("{")) {
                final JSONObject jsonObject = new JSONObject(msg);
                //最重要的方法,就一行,返回格式化的json字符串,其中的数字4是缩进字符数
                message = jsonObject.toString(4);
            } else if (msg.startsWith("[")) {
                final JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(4);
            } else {
                message = msg;
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
            message = msg;
        }

        message = JSON_RESULT + LINE_SEPARATOR + message;
        final String[] lines = message.split(LINE_SEPARATOR);
        for (String line : lines) {
            NLog.e(tag, line);
        }
    }
}
