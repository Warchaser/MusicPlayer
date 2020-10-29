package com.warchaser.musicplayer.tools

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore.Audio.Media
import java.io.File
import kotlin.collections.ArrayList

/**
 * Created by Wu on 2014/10/20.
 */
class MusicList private constructor(){

    companion object{
        private val MUSIC_INFO_LIST : ArrayList<MusicInfo> = ArrayList()
        private val CONTENT_URI : Uri = Media.EXTERNAL_CONTENT_URI
        private const val ORDER : String = "title COLLATE LOCALIZED"

        private lateinit var mContentResolver : ContentResolver

        /**
         * The music which is playing.
         */
        private var mCurrentMusic : Int = 0

        /**
         * The position of the music is playing.
         */
        private var mCurrentPosition : Int = 0

        /**
         * The length of each music;
         */
        private var mCurrentMax : Int = 0

        const val FIRST_POSITION : Int = 0
        const val FIRST_PROGRESS : Int = 0

        @JvmStatic
        fun init(contentResolver: ContentResolver){
            mContentResolver = contentResolver
            loadMusicList()
        }

        private fun loadMusicList(){
            var cursor : Cursor? = null
            try {
                var i : Int = 0
                cursor = mContentResolver.query(CONTENT_URI, null, null, null, ORDER)
                if(cursor == null){
                    return
                } else if(!cursor.moveToFirst()){
                    return
                } else {
                    val displayNameCol = cursor.getColumnIndex(Media.TITLE)
                    val albumCol = cursor.getColumnIndex(Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndex(Media.ALBUM_ID)
                    val idCol = cursor.getColumnIndex(Media._ID)
                    val durationCol = cursor.getColumnIndex(Media.DURATION)
                    val sizeCol = cursor.getColumnIndex(Media.SIZE)
                    val artistCol = cursor.getColumnIndex(Media.ARTIST)
                    val urlCol = cursor.getColumnIndex(Media.DATA)
                    do {
                        val url : String = cursor.getString(urlCol)
                        if(!File(url).exists()){
                            continue
                        }

                        val title = cursor.getString(displayNameCol)
                        val album = cursor.getString(albumCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val id = cursor.getLong(idCol)
                        val duration = cursor.getInt(durationCol)
                        val size = cursor.getLong(sizeCol)
                        val artist = cursor.getString(artistCol)

                        val musicInfo = MusicInfo(id, title)
                        musicInfo.album = album
                        musicInfo.albumId = albumId
                        musicInfo.duration = duration
                        musicInfo.size = size
                        musicInfo.artist = artist
                        musicInfo.url = url

                        when {
                            id > 0 -> {
                                musicInfo.uriWithCoverPic = "content://media/external/audio/media/$id/albumart"
                            }
                            albumId > 0 -> {
                                musicInfo.setUriWithCoverPic(getMediaStoreAlbumCoverUri(albumId))
                            }
                            else -> {
                                musicInfo.uriWithCoverPic = ""
                            }
                        }
//                    musicInfo.pinyinInitial = StringHelper.getPinYin(title).substring(0,1).toUpperCase(Locale.getDefault())
                        musicInfo.pinyinInitial = StringHelper.getPinYin(title).substring(0, 1)

                        if(musicInfo.duration / 1000 > 60){
                            MUSIC_INFO_LIST.add(musicInfo)
                        }

                        if(i <= CoverLoader.MAX_CACHE){
                            CoverLoader.instance.loadThumb(albumId)
                            CoverLoader.instance.loadBottomThumb(albumId)
                        }

                        i++

                    } while (cursor.moveToNext())

                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun getMediaStoreAlbumCoverUri(albumId : Long) : Uri{
            val artworkUri = Uri.parse("content://media/external/audio/albumart")
            return ContentUris.withAppendedId(artworkUri, albumId)
        }

        @JvmStatic
        fun deleteSingleMusicFile(uri : String?) : Boolean{
            if(uri == null){
                return false
            }
            return File(uri).delete()
        }

        @JvmStatic
        fun isListNotEmpty() : Boolean{
            return MUSIC_INFO_LIST.isNotEmpty()
        }

        @JvmStatic
        fun getCurrentMusic() : MusicInfo{
            return MUSIC_INFO_LIST[mCurrentMusic]
        }

        @JvmStatic
        fun getMusicWithPosition(position : Int) : MusicInfo{
            return MUSIC_INFO_LIST[position]
        }

        @JvmStatic
        fun size() : Int{
            return MUSIC_INFO_LIST.size
        }

        @JvmStatic
        fun add(bean : MusicInfo){
            MUSIC_INFO_LIST.add(bean)
        }

        @JvmStatic
        fun remove(position: Int){
            if(MUSIC_INFO_LIST.isEmpty()){
                return
            }

            MUSIC_INFO_LIST.removeAt(position)
        }

        @Synchronized
        @JvmStatic
        fun setCurrentPosition(currentPosition : Int){
            mCurrentPosition = currentPosition
        }

        /**
         * 获取当前Music的播放进度
         * */
        @Synchronized
        @JvmStatic
        fun getCurrentPosition() : Int{
            return mCurrentPosition
        }

        @Synchronized
        @JvmStatic
        fun setCurrentMusic(currentMusic : Int){
            mCurrentMusic = currentMusic
        }

        /**
         * 获取当前Music的Index
         * */
        @Synchronized
        @JvmStatic
        fun getCurrentMusicInt() : Int{
            return mCurrentMusic
        }

        @Synchronized
        @JvmStatic
        fun setCurrentMusicMax(max : Int){
            mCurrentMax = max
        }

        /**
         * 获取当前Music最大进度
         * */
        @Synchronized
        @JvmStatic
        fun getCurrentMusicMax() : Int{
            return mCurrentMax
        }

        /**
         * 是否最后一首
         * */
        @JvmStatic
        fun isLastMusic() : Boolean{
            return getCurrentMusicInt() == size() - 1
        }

        /**
         * 获取下一首位置
         * */
        @JvmStatic
        fun getNextPosition() : Int{
            return getCurrentMusicInt() + 1
        }

        /**
         * 是否第一首
         * */
        @JvmStatic
        fun isFirstMusic() : Boolean{
            return getCurrentMusicInt() == 0
        }

        /**
         * 获取前一首位置
         * */
        @JvmStatic
        fun getPreviousPosition() : Int{
            return getCurrentMusicInt() - 1
        }

        /**
         * 获取最后一首位置
         * */
        @JvmStatic
        fun getLastPosition() : Int{
            return size() - 1
        }
    }

    fun getMusicList() : ArrayList<MusicInfo>{
        return MUSIC_INFO_LIST
    }

    fun getMusicUriById(id : Long) : Uri{
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

}