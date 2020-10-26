package com.warchaser.musicplayer.tools

import android.content.Intent
import java.util.ArrayList

/**
 * Created by Achan on 2020/10/26
 * 观察者类
 * 双向通知
 * */
class CallObserver private constructor(){

    companion object{
        @JvmStatic
        val instance = Holder.mHolder
        private val mObservers : ArrayList<UIObserver> = ArrayList()

        private class ValueBean{
            var isFlag : Boolean = false
        }
    }

    private object Holder{
        val mHolder = CallObserver()
    }

    fun registerObserver(observer: UIObserver){
        mObservers.add(observer)
    }

    fun isNeedCallObserver() : Boolean{
        return mObservers.isNotEmpty()
    }

    private fun size() : Int{
        return mObservers.size
    }

    fun removeSingleObserver(observer: UIObserver){
        mObservers.remove(observer)
    }

    fun removeAllObservers(){
        mObservers.clear()
    }

    /**
     * 根据条件执行
     * */
    private fun process(observerSubFunction : SubFunction<UIObserver>){
        if(!isNeedCallObserver()){
            return
        }

        val size = size()
        for(i in 0 until size){
            val observer : UIObserver? = mObservers[i]
            observer?.run {
                if(observerEnabled){
                    observerSubFunction.processor(observer)
                }
            }
        }
    }

    /**
     * 无条件执行
     * */
    private fun processAll(observerSubFunction : SubFunction<UIObserver>){
        if(!isNeedCallObserver()){
            return
        }

        val size = size()
        for(i in 0 until size){
            val observer : UIObserver? = mObservers[i]
            observer?.run {
                observerSubFunction.processor(observer)
            }
        }
    }

    /**
     * 通知UI更新逻辑
     *
     * @param repeatTime 1 重复一次(playOrPause); 2重复两次(next)
     * @return boolean 是否存在UI Observer
     */
    fun callPlay(repeatTime : Int) : Boolean{
        val bean = ValueBean()
        process(object  : SubFunction<UIObserver>{
            override fun processor(t: UIObserver) {
                t.notify2Play(repeatTime)
                bean.isFlag = true
            }
        })

        return !bean.isFlag
    }

    /**
     * 通知UI刷新
     * */
    @Synchronized
    fun callObserver(intent: Intent){
        process(object : SubFunction<UIObserver>{
            override fun processor(t: UIObserver) {
                t.notifySeekBar2Update(intent)
            }
        })
    }

    fun stopUI(){
        processAll(object : SubFunction<UIObserver>{
            override fun processor(t: UIObserver) {
                t.stopServiceAndExit()
            }
        })
        removeAllObservers()
    }

    private interface SubFunction<T>{
        fun processor(t : T)
    }

}