package com.warchaser.musicplayer.tools

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

object StringHelper {

    /**
     * 得到 全拼
     *
     * @param src
     * @return
     */
    @JvmStatic
    fun getPinYin(src : String) : String{
        val t1 : CharArray = src.toCharArray()
        var t2 : Array<String>? = null
        val t3 = HanyuPinyinOutputFormat().apply {
            caseType = HanyuPinyinCaseType.LOWERCASE
            toneType = HanyuPinyinToneType.WITHOUT_TONE
            vCharType = HanyuPinyinVCharType.WITH_V
        }
        var t4 = ""
        val size = t1.size
        try {
            for(i in 0 until size){
                //判断是否为汉字字符
                when {
                    t1[i].toString().matches(Regex("[\\u4E00-\\u9FA5]+")) -> {
                        t2 = PinyinHelper.toHanyuPinyinStringArray(t1[i], t3)
                        t4 += t2[0]
                    }
                    t1[i].toString().matches(Regex("[\\u0030-\\u0039]+")) -> {
                        t4 += "#"
                    }
                    else -> {
                        t4 += t1[i].toString()
                    }
                }
            }
            return t4
        } catch (e : BadHanyuPinyinOutputFormatCombination) {
            e.printStackTrace()
        }

        return t4

    }

}