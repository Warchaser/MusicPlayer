package com.warchaser.musicplayer.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SlideBar : View {

    private val mPaint : Paint = Paint()

    private var mOnLetterTouchListener : OnLetterTouchListener? = null

    /**
     * 是否画出背景
     * */
    private var mIsShowBg : Boolean = false

    /**
     * 选中的项
     * */
    private var mSelection : Int = -1

    /**
     * SlideBar中的元素
     * */
    private val mLetters : Array<String> = arrayOf("#", "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z")

    constructor(context : Context) : super(context)

    constructor(context: Context, attrs : AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr : Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //获取宽和高
        val width = width
        val height = height
        //每个元素的高度
        val singleElementHeight = height / mLetters.size
        if(mIsShowBg){
            //画出背景
            canvas?.drawColor(Color.parseColor("#55000000"))
        }

        //画SlideBar中的元素
        for (i in mLetters.indices){
            mPaint.color = Color.BLACK
            //设置字体格式
            mPaint.isAntiAlias = false
            mPaint.textSize = 30f
            //如果这一个元素被选中,设置选中颜色
            if( i == mSelection){
                mPaint.color = Color.parseColor("#33CCCC")
                mPaint.isFakeBoldText = true
            }

            //要画的元素的x和y值
            val posX = width / 2 - mPaint.measureText(mLetters[i]) / 2
            val posY = i * singleElementHeight + singleElementHeight

            //画出字母
            canvas?.drawText(mLetters[i], posX, posY.toFloat(), mPaint)

            //重置画笔
            mPaint.reset()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.run {
            val y = event.y
            //画出点击的字母的索引
            val index : Int = (y / height * mLetters.size).toInt()
            //保存上次点击的字母的索引
            val lastSelection = mSelection
            mOnLetterTouchListener?.run {
                when(action){
                    MotionEvent.ACTION_DOWN -> {
                        mIsShowBg = true
                        handleActionMoveNDown(lastSelection, index, ::onLetterTouchChange)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        handleActionMoveNDown(lastSelection, index, ::onLetterTouchChange)
                    }
                    MotionEvent.ACTION_UP -> {
                        mIsShowBg = false
                        if(index <= 0){
                            onLetterTouchChange(mIsShowBg, "#")
                        } else if(index > 0 && index < mLetters.size){
                            onLetterTouchChange(mIsShowBg, mLetters[index])
                        } else if(index >= mLetters.size){
                            onLetterTouchChange(mIsShowBg, "Z")
                        }
                        invalidate()
                    }
                    else -> {
                        mIsShowBg = false
                        if(index in mLetters.indices && lastSelection != index){
                            onLetterTouchChange(mIsShowBg, mLetters[index])
                        }
                        invalidate()
                    }
                }
            }

        }

        return true
    }

    private fun handleActionMoveNDown(lastSelection : Int, index : Int, onLetterTouchChange : (isShowBg : Boolean, s : String) -> Unit){
        if(lastSelection != index && index in mLetters.indices){
            mSelection = index
            onLetterTouchChange(mIsShowBg, mLetters[index])
            invalidate()
        }
    }

    fun setOnLetterTouchChangeListener(onLetterTouchListener: OnLetterTouchListener){
        mOnLetterTouchListener = onLetterTouchListener
    }

    interface OnLetterTouchListener{
        fun onLetterTouchChange(isTouched : Boolean, s : String)
    }

}