package com.warchaser.musicplayer.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.warchaser.musicplayer.global.AppData
import java.io.InputStream

/**
 * Created by Wucn on 2017/2/3.
 */
object ImageUtil{

    @JvmStatic
    fun screenWidth() : Int{
        val dm = DisplayMetrics()
        val wmgr = AppData.getApp().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmgr.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }

    @JvmStatic
    fun screenHeight() : Int{
        val dm = DisplayMetrics()
        val wmgr = AppData.getApp().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmgr.defaultDisplay.getMetrics(dm)
        return dm.heightPixels
    }

    @JvmStatic
    fun setBackground(view: View, drawable: Drawable?){
        val sdk = android.os.Build.VERSION.SDK_INT
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN){
            view.setBackgroundDrawable(drawable)
        } else {
            view.background = drawable
        }
    }

    @JvmStatic
    fun dp2PX(context: Context, dp : Float) : Int{
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    @JvmStatic
    fun getDrawableFromBitmap(context: Context, bitmap: Bitmap?) : BitmapDrawable?{
        if(bitmap == null){
            return null
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    @JvmStatic
    fun getDrawableFromRes(context: Context, res : Int) : Drawable?{
        return ContextCompat.getDrawable(context, res)
    }

    @JvmStatic
    fun getCoverDrawableFromMusicFile(uriString: String?, context: Context?, px: Float) : Drawable?{
        return if(TextUtils.isEmpty(uriString) || context == null){
            null
        } else {
            getDrawableFromBitmap(context, getCoverBitmapFromMusicFile(uriString, context, px))
        }
    }

    @JvmStatic
    fun getCoverBitmapFromMusicFile(uriString: String?, context: Context?, px: Float) : Bitmap? {
        return if(TextUtils.isEmpty(uriString) || context == null){
            null
        } else {
            val res = context.contentResolver
            val uri = Uri.parse(uriString)
            uri?.run {
                var inputStream : InputStream?
                try {
                    inputStream = res.openInputStream(uri)
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 1
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, options)
                    options.inJustDecodeBounds = false
                    options.inDither = false
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    inputStream = res.openInputStream(uri)

                    var bitmap : Bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                            ?: return null

                    val width = dp2PX(context, px)
                    val ratio: Float = if(bitmap.height > bitmap.width){
                        width.toFloat() / bitmap.height.toFloat()
                    } else {
                        width.toFloat() / bitmap.width.toFloat()
                    }

                    bitmap = scaleBitmap(bitmap, ratio)!!
                    inputStream?.run {
                        close()
                    }
                    return bitmap

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } catch (e: Error) {
                    e.printStackTrace()
                    null
                }
            }
            null
        }
    }

    @JvmStatic
    fun zoomImage(bitmap : Bitmap, newWidth : Double, newHeight : Double) : Bitmap{
        //获取这个图片的款和高
        val width = bitmap.width
        val height = bitmap.height
        //创建操作图片用的Matrix对象
        val matrix = Matrix()
        //计算宽高缩放率
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        //缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    @JvmStatic
    fun scaleBitmap(origin : Bitmap?, width : Int) : Bitmap?{
        if(origin == null){
            return null
        }

        val ratio: Float = if(origin.height > origin.width){
            width.toFloat() / origin.height.toFloat()
        } else {
            width.toFloat() / origin.width.toFloat()
        }

        return scaleBitmap(origin, ratio)
    }

    @JvmStatic
    fun scaleBitmap(origin: Bitmap?, ratio : Float) : Bitmap?{
        if(origin == null){
            return null
        }

        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.preScale(ratio, ratio)
        val bitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true)
        if(bitmap == origin){
            return bitmap
        }

        origin.recycle()
        return bitmap
    }

    @JvmStatic
    fun setBottomBarDisc(context: Context, uri : String, width : Float, imageView : View, defaultImageId : Int, backgroundOrResource : Boolean){
        var drawable : Drawable?
        if(!TextUtils.isEmpty(uri)){
            drawable = getCoverDrawableFromMusicFile(uri, context, width)
            if(drawable == null){
                drawable = getDrawableFromRes(context, defaultImageId)
            }
        } else {
            drawable = getDrawableFromRes(context, defaultImageId)
        }

        if(backgroundOrResource){
            setBackground(imageView, drawable)
        } else {
            if(imageView is ImageView){
                imageView.setBackgroundResource(0)
                imageView.setImageDrawable(drawable)
            }
        }
    }

    /**
     * 将图片放大或缩小到指定尺寸
     */
    @JvmStatic
    fun resizeImage(source : Bitmap?, dstWidth : Int, dstHeight : Int) : Bitmap?{
        if(source == null){
            return null
        }

        if(source.width == dstWidth && source.height == dstHeight){
            return source
        }

        return Bitmap.createScaledBitmap(source, dstWidth, dstWidth, true)
    }

    @JvmStatic
    fun getNewDrawable(context: Context, resId : Int, dstWidth : Int) : BitmapDrawable{
        val bitmapOrigin = BitmapFactory.decodeResource(context.resources, resId)
        val bmp = scaleBitmap(bitmapOrigin, dstWidth)
        val d = BitmapDrawable(bmp)
        val bitmap = d.bitmap
        if(bitmap.density == Bitmap.DENSITY_NONE){
            d.setTargetDensity(context.resources.displayMetrics)
        }
        bitmapOrigin.recycle()
        return d
    }

}