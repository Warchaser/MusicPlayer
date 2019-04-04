package com.warchaser.musicplayer.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Wu on 2014/12/1.
 */
public class SlideBar extends View {

    private Paint paint = new Paint();
    private OnLetterTouchChangeListener onLetterTouchChangeListener;
    //是否画出背景
    private boolean showBg = false;
    //选中的项
    private int selection = -1;
    //SlideBar的元素
    public final String[] letters = {"#", "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    public SlideBar(Context context) {
        super(context);
    }

    public SlideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlideBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //获取宽和高
        int width = getWidth();
        int height = getHeight();
        //每个元素的高度
        int height_SingleElement = height / letters.length;
        if (showBg) {
            //画出背景
            canvas.drawColor(Color.parseColor("#55000000"));
        }

        //画SlideBar中的元素
        for (int i = 0; i < letters.length; i++) {
            paint.setColor(Color.BLACK);
            //设置元素字体格式
            paint.setAntiAlias(false);
            paint.setTextSize(30F);
            //如果这一个元素被选中，则换一个颜色
            if (i == selection) {
                paint.setColor(Color.parseColor("#33CCCC"));
                paint.setFakeBoldText(true);
            }
            //要画的元素的x,y的值
            float posX = width / 2 - paint.measureText(letters[i]) / 2;
            float posY = i * height_SingleElement + height_SingleElement;

            //画出字母
            canvas.drawText(letters[i], posX, posY, paint);
            //重置画笔
            paint.reset();
        }
    }

    /**
     * 处理SlideBar的状态
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float y = event.getY();
        //画出点击的字母的索引
        final int index = (int) (y / getHeight() * letters.length);
        //保存上次点击的字母的索引到oldSelection
        final int oldSelection = selection;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                showBg = true;
                if (oldSelection != index && onLetterTouchChangeListener != null && index >= 0 && index < letters.length) {
                    selection = index;
                    onLetterTouchChangeListener.onLetterTouchChange(showBg, letters[index]);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (oldSelection != index && onLetterTouchChangeListener != null && index >= 0 && index < letters.length) {
                    selection = index;
                    onLetterTouchChangeListener.onLetterTouchChange(showBg, letters[index]);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                showBg = false;
//                selection = -1;
                if (onLetterTouchChangeListener != null) {
                    if (index <= 0) {
                        onLetterTouchChangeListener.onLetterTouchChange(showBg, "#");
                    } else if (index > 0 && index < letters.length) {
                        onLetterTouchChangeListener.onLetterTouchChange(showBg, letters[index]);
                    } else if (index >= letters.length) {
                        onLetterTouchChangeListener.onLetterTouchChange(showBg, "Z");
                    }
                }
                invalidate();
                break;
            default:
                showBg = false;
//                selection = -1;
                if (oldSelection != index && onLetterTouchChangeListener != null && index >= 0 && index < letters.length) {
                    onLetterTouchChangeListener.onLetterTouchChange(showBg, letters[index]);
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * set
     */

    public void setOnLetterTouchChangeListener(OnLetterTouchChangeListener onLetterTouchChangeListener) {
        this.onLetterTouchChangeListener = onLetterTouchChangeListener;
    }

    public interface OnLetterTouchChangeListener {
        void onLetterTouchChange(boolean isTouched, String s);
    }
}
