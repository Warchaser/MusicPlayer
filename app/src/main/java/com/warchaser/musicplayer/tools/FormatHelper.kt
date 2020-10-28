package com.warchaser.musicplayer.tools

/**
 * Created by Administrator on 2014/10/21.
 */
object FormatHelper {

    fun formatDuration(milliSecond : Int) : String{
        val second : Int = milliSecond / 1000
        val secondPart : Int = second % 60
        val minutePart : Int = second / 60
        val minutePartStr = if(minutePart >= 10) minutePart else "0$minutePart"
        val secondPartStr = if(secondPart >= 10) secondPart else "0$secondPart"
        return "$minutePartStr:$secondPartStr"
    }

    fun formatTitle(title : String, length : Int) : String{
        val len = if(title.length < length) title.length else length
        var subString = title.substring(0, len)
        if(len < title.length){
            subString += "..."
        }
        return subString
    }

}