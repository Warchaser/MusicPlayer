package com.warchaser.musicplayer.tools;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Wu on 2014/10/20.
 *
 */
public class MusicInfo implements Parcelable
{

    private long lId;
    private String sTitle;
    private String sAlbum;
    private long mAlbumId;
    private int iDuration;
    private long lSize;
    private String sArtist;
    private String sUrl;
    private String pinyinInitial;
    private String pinyinTitle;
    private String mUriWithCoverPic;

    //Getters & setters

    public MusicInfo(Parcel in)
    {
        lId = in.readLong();
        sTitle = in.readString();
        sAlbum = in.readString();
        mAlbumId = in.readLong();
        iDuration = in.readInt();
        lSize = in.readLong();
        sArtist = in.readString();
        sUrl = in.readString();
        pinyinInitial = in.readString();
        pinyinTitle = in.readString();
        mUriWithCoverPic = in.readString();

    }

    public static final Creator<MusicInfo> CREATOR = new Creator<MusicInfo>()
    {
        @Override
        public MusicInfo createFromParcel(Parcel in)
        {
            return new MusicInfo(in);
        }

        @Override
        public MusicInfo[] newArray(int size)
        {
            return new MusicInfo[size];
        }
    };

    public long getId() {
        return lId;
    }

    public void setId(long id) {
        this.lId = id;
    }

    public String getTitle() {
        return sTitle;
    }

    public void setTitle(String title) {
        this.sTitle = title;
    }

    public String getAlbum() {
        return sAlbum;
    }

    public void setAlbum(String album) {
        this.sAlbum = album;
    }

    public long getAlbumId()
    {
        return mAlbumId;
    }

    public void setAlbumId(long albumId)
    {
        this.mAlbumId = albumId;
    }

    public int getDuration() {
        return iDuration;
    }

    public void setDuration(int duration) {
        this.iDuration = duration;
    }

    public long getSize() {
        return lSize;
    }

    public void setSize(long size) {
        this.lSize = size;
    }

    public String getArtist() {
        return sArtist;
    }

    public void setArtist(String artist) {
        this.sArtist = artist;
    }

    public String getUrl() {
        return sUrl;
    }

    public void setUrl(String url) {
        this.sUrl = url;
    }

    public String getPinyinInitial() {
        return pinyinInitial;
    }

    public void setPinyinInitial(String pinyinInitial) {
        this.pinyinInitial = pinyinInitial;
    }

    public String getPinyinTitle() {
        return pinyinTitle;
    }

    public void setPinyinTitle(String pinyinTitle) {
        this.pinyinTitle = pinyinTitle;
    }

    public String getUriWithCoverPic()
    {
        return mUriWithCoverPic;
    }

    public void setUriWithCoverPic(String uri)
    {
        this.mUriWithCoverPic = uri;
    }

    public void setUriWithCoverPic(Uri uri)
    {
        this.mUriWithCoverPic = uri.toString();
    }

    public MusicInfo(){

    }

    public MusicInfo(long pId, String pTitle)
    {
        lId = pId;
        sTitle = pTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeLong(lId);
        parcel.writeString(sTitle);
        parcel.writeString(sAlbum);
        parcel.writeLong(mAlbumId);
        parcel.writeInt(iDuration);
        parcel.writeLong(lSize);
        parcel.writeString(sArtist);
        parcel.writeString(sUrl);
        parcel.writeString(mUriWithCoverPic);
    }

    public static final Creator<MusicInfo>
        creator = new Creator<MusicInfo>()
    {
        @Override
        public MusicInfo createFromParcel(Parcel parcel)
        {
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setId(parcel.readLong());
            musicInfo.setTitle(parcel.readString());
            musicInfo.setAlbum(parcel.readString());
            musicInfo.setAlbumId(parcel.readLong());
            musicInfo.setArtist(parcel.readString());
            musicInfo.setUrl(parcel.readString());
            musicInfo.setDuration(parcel.readInt());
            musicInfo.setSize(parcel.readLong());
            musicInfo.setUriWithCoverPic(parcel.readString());
            return musicInfo;
        }

        @Override
        public MusicInfo[] newArray(int i)
        {
            return new MusicInfo[i];
        }
    };
}
