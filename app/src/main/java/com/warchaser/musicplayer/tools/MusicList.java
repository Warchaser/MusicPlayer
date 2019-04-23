package com.warchaser.musicplayer.tools;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.warchaser.musicplayer.tools.CoverLoader.MAX_CACHE;

/**
 * Created by Wu on 2014/10/20.
 */
public class MusicList {

    private static final List<MusicInfo> MUSIC_INFO_LIST = new ArrayList<MusicInfo>();
    private static MusicList mInstance;
    private static ContentResolver mContentResolver;
    private final Uri CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
    private final String ORDER = "title COLLATE LOCALIZED";

    /**
     * The music which is playing.
     */
    private static int mCurrentMusic;

    /**
     * The position of the music is playing.
     */
    private static int mCurrentPosition;

    /**
     * The length of each music;
     */
    private static int mCurrentMax;

    public static MyService.MyBinder mMyBinder;

    public static final int FIRST_POSITION = 0;
    public static final int FIRST_PROGRESS = 0;

    private String[] projection = {
            Media._ID,
            Media.TITLE,
            Media.DATA,
            Media.ALBUM,
            Media.ARTIST,
            Media.DURATION,
            Media.SIZE
    };

    private String sSortOrder = Media.DATA;

    public static MusicList instance(ContentResolver pContentResolver) {
        if (null == mInstance) {
            mContentResolver = pContentResolver;
            mInstance = new MusicList();
        }
        return mInstance;
    }

    private MusicList() {
        loadMusicList();
    }

    public void loadMusicList(){
        Cursor cursor = null;
        try {

            int i = 0;

            cursor = mContentResolver.query(CONTENT_URI, null, null, null, ORDER);
            if (null == cursor) {
                return;
            } else if (!cursor.moveToFirst()) {
                return;
            } else {
                int displayNameCol = cursor.getColumnIndex(Media.TITLE);
                int albumCol = cursor.getColumnIndex(Media.ALBUM);
                int albumIdCol = cursor.getColumnIndex(Media.ALBUM_ID);
                int idCol = cursor.getColumnIndex(Media._ID);
                int durationCol = cursor.getColumnIndex(Media.DURATION);
                int sizeCol = cursor.getColumnIndex(Media.SIZE);
                int artistCol = cursor.getColumnIndex(Media.ARTIST);
                int urlCol = cursor.getColumnIndex(Media.DATA);
                do {
                    String url = cursor.getString(urlCol);

                    if(!new File(url).exists()){
                        continue;
                    }

                    String title = cursor.getString(displayNameCol);
                    String album = cursor.getString(albumCol);
                    long albumId = cursor.getLong(albumIdCol);
                    long id = cursor.getLong(idCol);
                    int duration = cursor.getInt(durationCol);
                    long size = cursor.getLong(sizeCol);
                    String artist = cursor.getString(artistCol);

                    MusicInfo musicInfo = new MusicInfo(id, title);
                    musicInfo.setAlbum(album);
                    musicInfo.setAlbumId(albumId);
                    musicInfo.setDuration(duration);
                    musicInfo.setSize(size);
                    musicInfo.setArtist(artist);
                    musicInfo.setUrl(url);

                    if (id > 0) {
                        musicInfo.setUriWithCoverPic("content://media/external/audio/media/" + id + "/albumart");
                    } else if (albumId > 0) {
                        musicInfo.setUriWithCoverPic(getMediaStoreAlbumCoverUri(albumId));
                    } else {
                        musicInfo.setUriWithCoverPic("");
                    }

//                    musicInfo.setPinyinInitial(StringHelper.getPingYin(title).substring(0,1).toUpperCase());
                    musicInfo.setPinyinInitial(StringHelper.getPingYin(title).substring(0, 1));
                    if (musicInfo.getDuration() / 1000 > 60) {
                        MUSIC_INFO_LIST.add(musicInfo);
                    }

                    if(i <= MAX_CACHE){
                        CoverLoader.get().loadThumb(albumId);
                        CoverLoader.get().loadBottomThumb(albumId);
                    }

                    i++;

                } while (cursor.moveToNext());

                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MusicInfo> getMusicList() {
        return MUSIC_INFO_LIST;
    }

    public Uri getMusicUriById(long id) {
        return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    public static boolean deleteSingleMusicFile(String uri){
        return new File(uri).delete();
    }

    public static boolean isListNotEmpty(){
        return !MUSIC_INFO_LIST.isEmpty();
    }

    public static MusicInfo getCurrentMusic(){
        return MUSIC_INFO_LIST.get(mCurrentMusic);
    }

    public static MusicInfo getMusicWithPosition(int position){
        return MUSIC_INFO_LIST.get(position);
    }

    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(artworkUri, albumId);
    }

    public static int size(){

        if(MUSIC_INFO_LIST == null){
            return 0;
        }

        return MUSIC_INFO_LIST.size();
    }

    public static void add(MusicInfo bean){

        if(MUSIC_INFO_LIST == null){
            return;
        }

        MUSIC_INFO_LIST.add(bean);
    }

    public static void remove(int position){

        if(MUSIC_INFO_LIST == null || MUSIC_INFO_LIST.isEmpty()){
            return;
        }

        MUSIC_INFO_LIST.remove(position);
    }

    public synchronized static void setCurrentPosition(int currentPosition){
        mCurrentPosition = currentPosition;
    }

    /**
     * 获取当前Music的播放进度
     * */
    public synchronized static int getCurrentPosition(){
        return mCurrentPosition;
    }

    public synchronized static void setCurrentMusic(int currentMusic){
        mCurrentMusic = currentMusic;
    }

    /**
     * 获取当前Music的Index
     * */
    public synchronized static int getCurrentMusicInt(){
        return mCurrentMusic;
    }

    public synchronized static void setCurrentMusicMax(int max){
        mCurrentMax = max;
    }

    /**
     * 获取当前Music最大进度
     * */
    public synchronized static int getCurrentMusicMax(){
        return mCurrentMax;
    }

    /**
     * 是否最后一首
     * */
    public static boolean isLastMusic(){
        return getCurrentMusicInt() == size() - 1;
    }

    /**
     * 获取下一首位置
     * */
    public static int getNextPosition(){
        return getCurrentMusicInt() + 1;
    }

    /**
     * 是否第一首
     * */
    public static boolean isFirstMusic(){
        return getCurrentMusicInt() == 0;
    }

    /**
     * 获取前一首位置
     * */
    public static int getPreviousPosition(){
        return getCurrentMusicInt() - 1;
    }

    /**
     * 获取最后一首位置
     * */
    public static int getLastPosition(){
        return size() - 1;
    }

}
