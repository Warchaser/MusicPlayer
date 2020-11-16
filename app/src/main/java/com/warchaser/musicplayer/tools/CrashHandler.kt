package com.warchaser.musicplayer.tools

class CrashHandler private constructor(): Thread.UncaughtExceptionHandler {

    private val TAG : String = "CrashHandler"

    /**
     * 系统默认的UncaughtException处理类
     * */
    private lateinit var mDefaultHandler : Thread.UncaughtExceptionHandler

    companion object{
        @JvmStatic
        val instance: CrashHandler = Holder.mHolder
    }

    private object Holder{
        val mHolder = CrashHandler()
    }

    /**
     * 初始化
     */
    fun init(){
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        t?.run{
            handleException(e)

            mDefaultHandler.run {
                uncaughtException(t, e)
            }
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     */
    private fun handleException(ex : Throwable?){
        ex?.run {
            NLog.printStackTrace(TAG, this)
        }
    }
}