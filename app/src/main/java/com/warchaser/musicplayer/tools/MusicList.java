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

    public static List<MusicInfo> musicInfoList = new ArrayList<MusicInfo>();
    private static MusicList musicList;
    private static ContentResolver contentResolver;
    private Uri contentUri = Media.EXTERNAL_CONTENT_URI;
    private String order = "title COLLATE LOCALIZED";

    /**
     * The music which is playing.
     */
    public static int iCurrentMusic;

    /**
     * The position of the music is playing.
     */
    public static int iCurrentPosition;

    /**
     * The length of each music;
     */
    public static int iCurrentMax;

    public static MyService.MyBinder mMyBinder;

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
        if (null == musicList) {
            contentResolver = pContentResolver;
            musicList = new MusicList();
        }
        return musicList;
    }

    private MusicList() {
        loadMusicList();
    }

    public void loadMusicList(){
        Cursor cursor = null;
        try {

            int i = 0;

            cursor = contentResolver.query(contentUri, null, null, null, order);
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
                        musicInfoList.add(musicInfo);
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
        return musicInfoList;
    }

    public Uri getMusicUriById(long id) {
        Uri uri = ContentUris.withAppendedId(contentUri, id);
        return uri;
    }

    public static boolean deleteSingleMusicFile(String uri){
        return new File(uri).delete();
    }

    public static boolean isListEmpty(){
        return musicInfoList.isEmpty();
    }

    public static MusicInfo getCurrentMusic(){
        return musicInfoList.get(iCurrentMusic);
    }

    public static MusicInfo getMusicWithPosition(int position){
        return musicInfoList.get(position);
    }

    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(artworkUri, albumId);
    }

}
