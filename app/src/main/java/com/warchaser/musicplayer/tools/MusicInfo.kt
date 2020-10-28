package com.warchaser.musicplayer.tools

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * MusicInfo
 * bean
 * 传输数据类
 * */
class MusicInfo() : Parcelable{

    var id : Long = 0
    var title : String? = ""
    var album : String? = ""
    var albumId : Long = 0
    var duration : Int = 0
    var size : Long = 0
    var artist : String? = ""
    var url : String? = ""
    var pinyinInitial : String? = ""
    var pinyinTitle : String? = ""
    var uriWithCoverPic : String? = ""

    constructor(id: Long, title: String) : this(){
        this.id = id
        this.title = title
    }

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        title = parcel.readString()
        album = parcel.readString()
        albumId = parcel.readLong()
        duration = parcel.readInt()
        size = parcel.readLong()
        artist = parcel.readString()
        url = parcel.readString()
        pinyinInitial = parcel.readString()
        pinyinTitle = parcel.readString()
        uriWithCoverPic = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(album)
        parcel.writeLong(albumId)
        parcel.writeInt(duration)
        parcel.writeLong(size)
        parcel.writeString(artist)
        parcel.writeString(url)
        parcel.writeString(uriWithCoverPic)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MusicInfo> {
        override fun createFromParcel(parcel: Parcel): MusicInfo {
            return MusicInfo(parcel)
        }

        override fun newArray(size: Int): Array<MusicInfo?> {
            return arrayOfNulls<MusicInfo>(size)
        }
    }

    fun setUriWithCoverPic(uri: Uri) {
        this.uriWithCoverPic = uri.toString()
    }

}