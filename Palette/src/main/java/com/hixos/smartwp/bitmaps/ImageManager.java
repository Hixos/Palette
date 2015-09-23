package com.hixos.smartwp.bitmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.LruCache;
import android.util.TypedValue;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.triggers.geofence.GeofenceDB;
import com.hixos.smartwp.triggers.slideshow.SlideshowDB;
import com.hixos.smartwp.utils.ExternalStorageAccessException;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ImageManager {

    private static final String LOGTAG = "ImageManager";
    private static ImageManager sInstance;
    private LruCache<String, Bitmap> mCache;
    private Context mContext;
    private RecycleBin mRecycleBin;

    private ImageManager(Context context) {
        mRecycleBin = new RecycleBin();
        mContext = context;
    }

    public static ImageManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new IllegalStateException("ImageManager not instantiated");
        }
    }

    public static void reset() {
        if (sInstance != null) {
            sInstance.destroy();
            sInstance = null;
        }
    }

    private void createCache(int maxSize) {
        mCache = new LruCache<String, Bitmap>(maxSize) {
            protected synchronized void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                mRecycleBin.put(oldValue);
            }

            protected synchronized int sizeOf(String s, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }

    private void destroy() {
        if (mCache != null) {
            mCache.evictAll();
        }
        if (mRecycleBin != null) {
            mRecycleBin.empty();
        }
    }

    private Bitmap generateThumbnail(String uid) {
        if (!FileUtils.isExternalStorageReadable() || !FileUtils.isExternalStorageWritable()) {
            Logger.e("ImageManager", "Unable to read/write external storage");
            return null;
        }
        Rect imageSize = BitmapIO.getImageSize(mContext, getPictureUri(uid));
        float scale = getThumbnailScaleFactor(imageSize, uid);
        if (imageSize != null && scale > 0.0f) {
            int width = Math.round(scale * (float) imageSize.width());
            int height = Math.round(scale * (float) imageSize.height());
            Bitmap thumb = BitmapIO.loadBitmap(mContext, getPictureUri(uid), width, height);
            if (thumb != null) {
                try {
                    if (BitmapIO.saveBitmapToFile(mContext, thumb, getThumbnailUri(uid),
                            android.graphics.Bitmap.CompressFormat.JPEG, getThumbnailQuality())) {
                        Logger.e("ImageManager", "Error saving thumbnail to file: false");
                    }
                } catch (IOException IOE) {
                    Logger.e("ImageManager", "Error saving thumbnail to file: " + IOE.getMessage());
                } catch (ExternalStorageAccessException ESAE) {
                    Logger.e("ImageManager", "Error saving thumbnail to file: " + ESAE.getMessage());
                }
                return thumb;
            }
        }
        return null;
    }

    private int getThumbnailQuality() {
        return mContext.getResources().getInteger(R.integer.thumbnail_quality);
    }

    private float getThumbnailScaleFactor(Rect rect, String s) {
        return rect != null
                ? Math.min((float) getThumbnailSuggestedSize(s) /
                (float) Math.min(rect.width(), rect.height()), 1.0F)
                : 1.0f;
    }

    private int getThumbnailSuggestedSize(String uid) {
        Point point = MiscUtils.UI.getDisplaySize(mContext);
        TypedValue typedvalue = new TypedValue();
        float scale;
        if (uid.startsWith(SlideshowDB.SLIDESHOW_ID_PREFIX)) {
            mContext.getResources().getValue(R.dimen.thumbnail_slideshow_factor, typedvalue, true);
            scale = typedvalue.getFloat();
        } else if (uid.equals(GeofenceDB.DEFAULT_WALLPAPER_UID)) {
            mContext.getResources().getValue(R.dimen.thumbnail_geofence_default_factor, typedvalue, true);
            scale = typedvalue.getFloat();
        } else if (uid.startsWith(GeofenceDB.GEOFENCE_ID_PREFIX)) {
            mContext.getResources().getValue(R.dimen.thumbnail_geofence_factor, typedvalue, true);
            scale = typedvalue.getFloat();
        } else if (uid.startsWith(GeofenceDB.GEOFENCE_SNAPSHOT_ID_PREFIX)) {
            mContext.getResources().getValue(R.dimen.thumbnail_snapshot_factor, typedvalue, true);
            scale = typedvalue.getFloat();
        } else {
            scale = 1.0F;
        }
        return Math.round((float) Math.max(point.x, point.y) / scale);
    }

    public void disableCache() {
        if (mCache != null) {
            mCache.evictAll();
        }
        mCache = null;
    }

    public LruCache<String, Bitmap> getCache() {
        return mCache;
    }

    /*public void disableRecycleBin(){
        if (mRecycleBin != null){
            mRecycleBin.empty();
        }
        mRecycleBin = null;
    }*/

    public Bitmap getCachedThumbnail(String s) {
        if (mCache != null) {
            return mCache.get(s);
        } else {
            return null;
        }
    }

    public Uri getPictureUri(String uid) {
        return Uri.fromFile(new File((new StringBuilder())
                .append(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                .append(File.separator).append(uid).toString()));
    }

    public RecycleBin getRecycleBin() {
        return mRecycleBin;
    }

    public Bitmap getThumbnail(String uid, int width, int height) {
        Uri thumbUri = getThumbnailUri(uid);
        Bitmap thumb;
        if (mCache != null) {
            thumb = mCache.get(uid);
        } else {
            thumb = null;
        }
        if (thumb != null) {
            return thumb;
        } else {
            if (FileUtils.fileExistance(thumbUri)) {
                thumb = BitmapIO.loadBitmap(mContext, thumbUri, width, height);
                if (thumb != null) {
                    if (mCache != null) {
                        mCache.put(uid, thumb);
                    }
                    return thumb;
                } else {
                    return ((BitmapDrawable) mContext.getResources()
                            .getDrawable(R.drawable.ic_action_logo)).getBitmap();
                }
            } else {
                Bitmap generatedThumbnail = generateThumbnail(uid);
                if (generatedThumbnail != null) {
                    thumb = BitmapUtils.resizeBitmap(generatedThumbnail, width, height);
                    if (!thumb.equals(generatedThumbnail) && mRecycleBin != null) {
                        mRecycleBin.put(generatedThumbnail);
                    }
                    if (mCache != null) {
                        mCache.put(uid, thumb);
                    }
                    return thumb;
                } else {
                    if (mCache != null) {
                        mCache.remove(uid);
                    }
                    return ((BitmapDrawable) mContext.getResources()
                            .getDrawable(R.drawable.ic_action_logo)).getBitmap();
                }
            }
        }
    }

    public Uri getThumbnailUri(String uid) {
        return Uri.fromFile(new File((new StringBuilder()).append(mContext.getExternalCacheDir()).append(File.separator).append(uid).toString()));
    }

    public boolean isCached(String uid) {
        return mCache != null && mCache.get(uid) != null;
    }

    public boolean saveBitmap(Bitmap bitmap, String uid) {
        try {
            return BitmapIO.saveBitmapToFile(mContext, bitmap, getPictureUri(uid),
                    Bitmap.CompressFormat.JPEG, getThumbnailQuality());
        } catch (ExternalStorageAccessException ESAE) {
            Logger.e(LOGTAG, "Error saving bitmap to file, " + ESAE.getMessage());
        } catch (IOException IOE) {
            Logger.e(LOGTAG, "Error saving bitmap to file, " + IOE.getMessage());
        }
        return false;
    }

    public interface OnImageLoadedListener {
        void onImageLoaded(Bitmap bitmap, String s);
    }

    public static class RecycleBin {
        private static final String LOGTAG = "RecycleBin";
        private final List<SoftReference<Bitmap>> mItems;

        private RecycleBin() {
            mItems = Collections.synchronizedList(new ArrayList<SoftReference<Bitmap>>());
        }

        private static int getBytesPerPixel(Bitmap.Config config) {
            switch (config) {
                case RGB_565:
                case ARGB_4444:
                    return 2;
                case ALPHA_8:
                    return 1;
                case ARGB_8888:
                    return 4;
            }
            return 1;
        }

        private void empty() {
            synchronized (mItems) {
                Iterator<SoftReference<Bitmap>> iterator = mItems.iterator();
                for (SoftReference<Bitmap> bitmapSoftReference : mItems) {
                    Bitmap bmp = bitmapSoftReference.get();
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                    }
                    bmp = null;
                    bitmapSoftReference.clear();
                    bitmapSoftReference = null;
                }
                mItems.clear();
            }
        }

        public Bitmap restore(int width, int height, Bitmap.Config config) {
            int requiredByteCount = width * height * getBytesPerPixel(config);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                synchronized (mItems) {
                    Bitmap bestBitmap = null;
                    int bestByteDifference = Integer.MAX_VALUE;

                    Iterator<SoftReference<Bitmap>> iterator = mItems.iterator();
                    while (iterator.hasNext()) {
                        SoftReference<Bitmap> bitmapSoftReference = iterator.next();
                        Bitmap bmp = bitmapSoftReference.get();
                        if (bmp == null) {
                            bitmapSoftReference.clear();
                            iterator.remove();
                        } else {
                            if (bestBitmap == null) {
                                int byteDifference = bmp.getAllocationByteCount() - requiredByteCount;
                                if (byteDifference > 0) {
                                    bestBitmap = bmp;
                                    bestByteDifference = byteDifference;
                                }
                            } else {
                                int byteDifference = bmp.getAllocationByteCount() - requiredByteCount;
                                if (byteDifference == 0) {
                                    iterator.remove();
                                    return bmp;
                                } else if (byteDifference > 0 && byteDifference < bestByteDifference) {
                                    bestByteDifference = byteDifference;
                                    bestBitmap = bmp;
                                }
                            }
                        }
                    }

                    iterator = mItems.iterator();
                    while (iterator.hasNext()) {
                        SoftReference<Bitmap> bitmapSoftReference = iterator.next();
                        Bitmap bmp = bitmapSoftReference.get();
                        if (bmp == bestBitmap) {
                            iterator.remove();
                            break;
                        }
                    }
                    return bestBitmap;
                }
            } else {
                synchronized (mItems) {
                    Iterator<SoftReference<Bitmap>> iterator = mItems.iterator();

                    while (iterator.hasNext()) {
                        SoftReference<Bitmap> bitmapSoftReference = iterator.next();
                        Bitmap bmp = bitmapSoftReference.get();
                        if (bmp == null) {
                            bitmapSoftReference.clear();
                            iterator.remove();
                        } else {
                            if (bmp.getWidth() == width && bmp.getHeight() == height) {
                                iterator.remove();
                                return bmp;
                            }
                        }
                    }
                }
            }
            return null;
        }

        public boolean put(Bitmap bitmap) {
            if (bitmap == null || !bitmap.isMutable()) {
                return false;
            }
            synchronized (mItems) {
                Iterator<SoftReference<Bitmap>> iterator = mItems.iterator();
                while (iterator.hasNext()) {
                    SoftReference<Bitmap> bitmapSoftReference = iterator.next();
                    if (bitmap.equals(bitmapSoftReference.get())) {
                        return false;
                    }
                }
            }
            mItems.add(new SoftReference<Bitmap>(bitmap));
            bitmap = null;
            return true;
        }
    }

    public static class Builder {
        private int mCacheSize;
        private Context mContext;
        private boolean mHasCache;

        public Builder(Context context) {
            mHasCache = false;
            if (ImageManager.sInstance != null) {
                throw new IllegalStateException("Imageloader already instatiated");
            }
            if (context == null) {
                throw new IllegalArgumentException("Context is null");
            } else {
                mCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8L);
                mContext = context;
            }
        }

        public ImageManager build() {
            ImageManager.sInstance = new ImageManager(mContext);
            if (mHasCache) {
                ImageManager.sInstance.createCache(mCacheSize);
            }
            return ImageManager.sInstance;
        }

        public Builder cacheSize(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Cache size must be > 0 bytes");
            } else {
                mCacheSize = size;
                return this;
            }
        }

        public Builder hasCache(boolean hasFlag) {
            mHasCache = hasFlag;
            return this;
        }
    }
}
