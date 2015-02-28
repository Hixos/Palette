package com.hixos.smartwp.bitmaps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;

import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.wallpaper.WallpaperUtils;

public class WallpaperCropper {

    /**
     * Crops the given wallpaper and stores it in the given uri.
     * Asynchronous task.
     *
     * @param inUri    The wallpaper uri
     * @param outUri   cropped wallpape uri
     * @param callback called when the cropping has finished.
     */

    public static void autoCropWallpaper(final Context context, final Uri inUri, final Uri outUri,
                                         final BitmapIO.OnImageCroppedCallback callback){
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            Rect size;
            @Override
            protected Boolean doInBackground(Void... voids) {
                size = BitmapIO.getImageSize(context.getApplicationContext(), inUri);
                return size != null && autoCropWallpaper(context, inUri, outUri, size);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean) {
                    callback.onImageCropped(outUri);
                } else {
                    callback.onImageCropFailed();
                }
            }
        };
        task.execute();
    }

    private static boolean autoCropWallpaper(final Context context, Uri inUri, Uri outUri, Rect imageSize) {
        Point screenSize;
        Rect crop;

        screenSize = MiscUtils.UI.getDisplaySize(context);

        boolean kk = MiscUtils.usingGoogleNowLauncher(context);
        float cropRatio = kk
                ? WallpaperUtils.getKitkatMinCropRatio(screenSize.x, screenSize.y)
                : WallpaperUtils.getDefaultCropRatio(screenSize.x, screenSize.y, context);

        crop = RectUtils.getCropArea(cropRatio, imageSize);

        if (kk) {
            float maxRatio = WallpaperUtils.getKitkatMaxCropRatio(screenSize.x, screenSize.y, context);
            crop.right = Math.min(Math.round(crop.left + crop.height() * maxRatio), imageSize.right);
        }

        int outHeight = Math.max(screenSize.x, screenSize.y);
        int outWidth = Math.round(outHeight * ((float) crop.width() / (float) crop.height()));

        return BitmapIO.cropImageToFile(context, inUri, outUri, crop, outWidth, outHeight);
    }


    public static void cropWallpaper(final Uri inUri, final Uri outUri, final RectF relativeCrop,
                                     final int outHeight,
                                     final BitmapIO.OnImageCroppedCallback callback,
                                     final Context context) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            Rect size;

            @Override
            protected Boolean doInBackground(Void... voids) {
                size = BitmapIO.getImageSize(context.getApplicationContext(), inUri);
                return cropWallpaper(context.getApplicationContext(), inUri, outUri, relativeCrop, size,
                        outHeight);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean) {
                    callback.onImageCropped(outUri);
                } else {
                    callback.onImageCropFailed();
                }
            }
        };
        task.execute();
    }

    private static boolean cropWallpaper(Context context, Uri inUri, Uri outUri, RectF relativeCrop,
                                         Rect imageSize, int outHeight) {
        Rect fullCrop;
        fullCrop = RectUtils.getFullRect(relativeCrop, imageSize);

        int outWidth = (int) (outHeight * ((float) fullCrop.width() / (float) fullCrop.height()));
        return BitmapIO.cropImageToFile(context, inUri, outUri, fullCrop, outWidth, outHeight);
    }
}
