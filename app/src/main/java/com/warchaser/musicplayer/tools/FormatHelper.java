package com.warchaser.musicplayer.tools;

/**
 * Created by Administrator on 2014/10/21.
 */
public class FormatHelper {
    public static String formatDuration(int iMillisecond) {
        int iSecond = iMillisecond / 1000;
        int iSecondPart = iSecond % 60;
        int iMinutePart = iSecond / 60;
        return (iMinutePart >= 10 ? iMinutePart : "0" + iMinutePart) + ":" + (iSecondPart >= 10 ? iSecondPart : "0" + iSecondPart);
    }

    public static String formatTitle(String sTitle, int iLength) {
        int iLen = sTitle.length() < iLength ? sTitle.length() : iLength;
        String sSubString = sTitle.substring(0, iLen);
        if (iLen < sTitle.length()) {
            sSubString += "...";
        }
        return sSubString;
    }

}
