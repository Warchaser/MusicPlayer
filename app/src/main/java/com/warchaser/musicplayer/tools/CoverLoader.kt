package com.warchaser.musicplayer.tools

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.LruCache
import com.warchaser.musicplayer.R
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * 专辑封面图片加载器
 */
class CoverLoader private constructor(){

    companion object{
        @JvmStatic
        val instance = SingletonHolder.mHolder
        private val KEY_NULL : String = "null"
        @JvmStatic
        val MAX_CACHE : Int = 20
    }

    private object SingletonHolder{
        val mHolder = CoverLoader()
    }

    private var THUMB_WIDTH : Int = 0
    private var COVER_WIDTH : Int = 0
    private var BOTTOM_THUMB_WIDTH : Int = 0

    private lateinit var mContext : Context
    private val mCacheMap : HashMap<Type, LruCache<String, Bitmap>> = HashMap(3)

    private enum class Type{
        THUMB,
        COVER,
        BOTTOM_THUMB
    }

    fun init(context: Context){
        mContext = context.applicationContext
        THUMB_WIDTH = ImageUtil.dp2PX(mContext, mContext.resources.getDimension(R.dimen.notification_cover_width))
        COVER_WIDTH = ImageUtil.screenWidth() - ImageUtil.dp2PX(mContext, mContext.resources.getDimension(R.dimen.display_cover_width))
        BOTTOM_THUMB_WIDTH = ImageUtil.dp2PX(mContext, mContext.resources.getDimension(R.dimen.bottom_bar_disc_width_and_height))

        // 获取当前进程的可用内存（单位KB）
        val maxMemory : Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // 缓存大小为当前进程可用内存的1/8
        val cacheSize : Int = maxMemory / 8

        val thumbCache : LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize){
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    bitmap.allocationByteCount / 1024
                } else {
                    bitmap.byteCount / 1024
                }
            }
        }

        val roundCache : LruCache<String, Bitmap> = LruCache(10)
        val blurCache : LruCache<String, Bitmap> = LruCache(10)

        mCacheMap[Type.THUMB] = thumbCache
        mCacheMap[Type.COVER] = roundCache
        mCacheMap[Type.BOTTOM_THUMB] = blurCache
    }

    fun loadThumb(albumId : Long) : Bitmap?{
        return loadCover(albumId, Type.THUMB)
    }

    fun loadDisplayCover(albumId: Long) : Bitmap?{
        return loadCover(albumId, Type.COVER)
    }

    fun loadBottomThumb(albumId: Long) : Bitmap?{
        return loadCover(albumId, Type.BOTTOM_THUMB)
    }

    private fun loadCover(albumId: Long, type : Type) : Bitmap{
        var bitmap : Bitmap?
        val key : String = albumId.toString()
        val cache : LruCache<String, Bitmap>? = mCacheMap[type]
        cache?.run {
            if(TextUtils.isEmpty(key) || "-1" == key){
                bitmap = this[KEY_NULL]
                if(bitmap != null){
                    return bitmap as Bitmap
                }

                bitmap = getDefaultCover(type)
                cache.put(KEY_NULL, bitmap)
                return bitmap as Bitmap
            }

            bitmap = cache.get(key)
            if(bitmap != null){
                return bitmap as Bitmap
            }

            bitmap = loadCoverByType(albumId, type)
            if(bitmap != null){
                cache.put(key, bitmap)
                return bitmap as Bitmap
            }
        }

        return loadCover(-1, type)
    }

    private fun getDefaultCover(type : Type) : Bitmap?{
        return when(type){
            Type.COVER -> {
                var bitmap : Bitmap? = BitmapFactory.decodeResource(mContext.resources, R.mipmap.disc)
                bitmap = ImageUtil.scaleBitmap(bitmap, COVER_WIDTH)
                bitmap
            }
            Type.THUMB -> {
                ImageUtil.scaleBitmap(BitmapFactory.decodeResource(mContext.resources, R.mipmap.disc), BOTTOM_THUMB_WIDTH)
            }
            else -> {
                ImageUtil.scaleBitmap(BitmapFactory.decodeResource(mContext.resources, R.mipmap.disc), THUMB_WIDTH)
            }
        }
    }

    private fun loadCoverByType(albumId: Long, type : Type) : Bitmap?{
        val bitmap : Bitmap? = loadCoverFromMediaStore(albumId)

        return when(type){
            Type.COVER -> {
                ImageUtil.scaleBitmap(bitmap, COVER_WIDTH)
            }
            Type.BOTTOM_THUMB -> {
                ImageUtil.scaleBitmap(bitmap, BOTTOM_THUMB_WIDTH)
            }
            else -> {
                ImageUtil.scaleBitmap(bitmap, THUMB_WIDTH)
            }
        }
    }

    /**
     * 从媒体库加载封面
     *
     * 本地音乐
     */
    private fun loadCoverFromMediaStore(albumId: Long) : Bitmap?{
        val resolver : ContentResolver = mContext.contentResolver
        val uri : Uri = MusicList.getMediaStoreAlbumCoverUri(albumId)
        val inputStream : InputStream?
        try {
            inputStream = resolver.openInputStream(uri)
        } catch (e : FileNotFoundException) {
            e.printStackTrace()
            return null
        }

        val options : BitmapFactory.Options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeStream(inputStream, null, options)
    }

    /**
     * 从下载的图片加载封面
     *
     * 网络音乐
     */
    private fun loadCoverFromFile(path : String) : Bitmap{
        val options : BitmapFactory.Options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(path, options)
    }

    fun currentCacheSize() : Int{
        return mCacheMap.size
    }

}