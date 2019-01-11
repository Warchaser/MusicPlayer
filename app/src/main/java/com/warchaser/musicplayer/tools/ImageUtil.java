package com.warchaser.musicplayer.tools;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Created by Wucn on 2017/2/3.
 *
 */

public class ImageUtil
{
    private ImageUtil()
    {

    }

    public static void setBackground(View view, Drawable drawable)
    {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
        {
            view.setBackgroundDrawable(drawable);
        }
        else
        {
            view.setBackground(drawable);
        }
    }

    public static Drawable getCoverDrawableFromMusicFile(String uriString, Context context, float px)
    {
        if(TextUtils.isEmpty(uriString))
        {
            return null;
        }
        else
        {
            ContentResolver res = context.getContentResolver();
            Uri uri = Uri.parse(uriString);

            if(uri != null)
            {
                InputStream is;
                try
                {
                    is = res.openInputStream(uri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, options);
                    options.inJustDecodeBounds = false;
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    is = res.openInputStream(uri);

                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

                    int width = dp2Px(context, px);
                    float ratio = 0;
                    if(bitmap.getHeight() > bitmap.getWidth()){
                        ratio = (float) width / (float) bitmap.getHeight();
                    } else {
                        ratio = (float) width / (float) bitmap.getWidth();
                    }
//
                    bitmap = scaleBitmap(bitmap, ratio);
                    if(is != null)
                    {
                        is.close();
                    }

                    return getDrawableFromBitmap(context, bitmap);
                }
                catch (Exception | Error e)
                {
                    e.printStackTrace();
                    return null;
                }
            }

            return null;
        }
    }

    public static Drawable getDrawableFromBitmap(Context context, Bitmap bitmap)
    {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getDrawableFromRes(Context context, int res)
    {
        return ContextCompat.getDrawable(context, res);
    }
    

    public static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static Bitmap zoomImage(Bitmap bitmap, double newWidth,
                                   double newHeight) {
        // 获取这个图片的宽和高
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, (int) width,
                (int) height, matrix, true);
    }

    public static Bitmap scaleBitmap(Bitmap origin, float ratio){
        if(origin == null){
            return null;
        }

        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap bitmap = Bitmap.createBitmap(origin, 0,0, width, height, matrix, true);
        if(bitmap.equals(origin)){
            return bitmap;
        }

        origin.recycle();
        return bitmap;
    }

    public static void setBottomBarDisc(Context context, String uri, float width, View imageView, int defaultImageId, boolean backgroundOrResource)
    {
        Drawable drawable;
        if(!TextUtils.isEmpty(uri))
        {
            drawable = getCoverDrawableFromMusicFile(uri, context, width);
            if(drawable == null)
            {
                drawable = getDrawableFromRes(context, defaultImageId);
            }
        }
        else
        {
            drawable = getDrawableFromRes(context, defaultImageId);
        }

        if(backgroundOrResource){
            setBackground(imageView, drawable);
        } else {
            if(imageView instanceof ImageView){
                imageView.setBackgroundResource(0);
                ((ImageView)imageView).setImageDrawable(drawable);
            }
        }
    }

}
