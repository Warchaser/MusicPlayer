package com.warchaser.musicplayer.tools;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.warchaser.musicplayer.R;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import androidx.collection.LruCache;


/**
 * 专辑封面图片加载器
 */
public class CoverLoader {
    private static final String KEY_NULL = "null";
    public static final int MAX_CACHE = 20;

    private Context context;
    private Map<Type, LruCache<String, Bitmap>> cacheMap;

    private int THUMB_WIDTH;
    private int COVER_WIDTH;
    private int BOTTOM_THUMB_WIDTH;

    private enum Type {
        THUMB,
        COVER,
        BOTTOM_THUMB
    }

    public static CoverLoader get() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static CoverLoader instance = new CoverLoader();
    }

    private CoverLoader() {
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        THUMB_WIDTH = ImageUtil.dp2Px(this.context, this.context.getResources().getDimension(R.dimen.notification_cover_width));
        COVER_WIDTH = ImageUtil.screenWidth() - ImageUtil.dp2Px(this.context, this.context.getResources().getDimension(R.dimen.display_cover_width));
        BOTTOM_THUMB_WIDTH = ImageUtil.dp2Px(this.context, this.context.getResources().getDimension(R.dimen.bottom_bar_disc_width_and_height));

        // 获取当前进程的可用内存（单位KB）
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 缓存大小为当前进程可用内存的1/8
        int cacheSize = maxMemory / 8;
        LruCache<String, Bitmap> thumbCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    return bitmap.getAllocationByteCount() / 1024;
                } else {
                    return bitmap.getByteCount() / 1024;
                }
            }
        };
        LruCache<String, Bitmap> roundCache = new LruCache<>(10);
        LruCache<String, Bitmap> blurCache = new LruCache<>(10);

        cacheMap = new HashMap<>(3);
        cacheMap.put(Type.THUMB, thumbCache);
        cacheMap.put(Type.COVER, roundCache);
        cacheMap.put(Type.BOTTOM_THUMB, blurCache);
    }


    public Bitmap loadThumb(long albumId) {
        return loadCover(albumId, Type.THUMB);
    }

    public Bitmap loadDisplayCover(long albumId) {
        return loadCover(albumId, Type.COVER);
    }

    public Bitmap loadBottomThumb(long albumId) {
        return loadCover(albumId, Type.BOTTOM_THUMB);
    }

    private Bitmap loadCover(long albumId, Type type) {
        Bitmap bitmap;
        String key = String.valueOf(albumId);
        LruCache<String, Bitmap> cache = cacheMap.get(type);
        if (TextUtils.isEmpty(key) || "-1".equals(key)) {
            bitmap = cache.get(KEY_NULL);
            if (bitmap != null) {
                return bitmap;
            }

            bitmap = getDefaultCover(type);
            cache.put(KEY_NULL, bitmap);
            return bitmap;
        }

        bitmap = cache.get(key);
        if (bitmap != null) {
            return bitmap;
        }

        bitmap = loadCoverByType(albumId, type);
        if (bitmap != null) {
            cache.put(key, bitmap);
            return bitmap;
        }

        return loadCover(-1, type);
    }

    private Bitmap getDefaultCover(Type type) {
        switch (type) {
            case COVER:
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.disc);
                bitmap = ImageUtil.scaleBitmap(bitmap, COVER_WIDTH);
                return bitmap;
            case BOTTOM_THUMB:
                return ImageUtil.scaleBitmap(BitmapFactory.decodeResource(context.getResources(), R.mipmap.disc), BOTTOM_THUMB_WIDTH);
            default:
                return ImageUtil.scaleBitmap(BitmapFactory.decodeResource(context.getResources(), R.mipmap.disc), THUMB_WIDTH);
        }
    }

    private Bitmap loadCoverByType(long albumId, Type type) {
        Bitmap bitmap = loadCoverFromMediaStore(albumId);

        switch (type) {
            case COVER:
                return ImageUtil.scaleBitmap(bitmap, COVER_WIDTH);
            case BOTTOM_THUMB:
                return ImageUtil.scaleBitmap(bitmap, BOTTOM_THUMB_WIDTH);
            default:
                return ImageUtil.scaleBitmap(bitmap, THUMB_WIDTH);
        }
    }

    /**
     * 从媒体库加载封面<br>
     * 本地音乐
     */
    private Bitmap loadCoverFromMediaStore(long albumId) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MusicList.getMediaStoreAlbumCoverUri(albumId);
        InputStream is;
        try {
            is = resolver.openInputStream(uri);
        } catch (FileNotFoundException ignored) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeStream(is, null, options);
    }

    /**
     * 从下载的图片加载封面<br>
     * 网络音乐
     */
    private Bitmap loadCoverFromFile(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path, options);
    }
}
