package com.warchaser.musicplayer.tools

import android.content.Intent
import java.util.ArrayList

/**
 * Created by Achan on 2020/10/26
 * 观察者类
 * 双向通知
 * */

private typealias Processor<T> = (t : T) -> Unit

class CallObserver private constructor(){

    companion object{
        @JvmStatic
        val instance = Holder.mHolder
        private val mObservers : ArrayList<UIObserver?> = ArrayList()

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

        mObservers.forEach {
            it?.run {
                if(observerEnabled){
                    observerSubFunction.processor(this)
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

        mObservers.forEach {
            it?.run {
                observerSubFunction.processor(this)
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
        process(registerFunction {
            processor {
                it.notify2Play(repeatTime)
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
        process(registerFunction {
            processor { it.notifySeekBar2Update(intent) }
        })
    }

    fun stopUI(){
        processAll(registerFunction {
            processor { it.stopServiceAndExit() }
        })
        removeAllObservers()
    }

    private interface SubFunction<T>{
        fun processor(t : T)
    }

    inner class SubFunctionDSL<T : UIObserver> : SubFunction<T>{

        private var mProcessor : Processor<T>? = null

        override fun processor(t: T) {
            mProcessor?.invoke(t)
        }

        fun processor(processor: Processor<T>){
            mProcessor = processor
        }
    }

    private fun registerFunction(function : SubFunctionDSL<UIObserver>.() -> Unit) = SubFunctionDSL<UIObserver>().also(function)

}