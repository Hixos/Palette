package com.hixos.smartwp.wallpaper;

import android.content.Context;
import android.graphics.Point;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.MiscUtils;

public class WallpaperUtils {
    public static Point getLandscapeWallpaperSize(Context context) {
        Point screen = MiscUtils.UI.getDisplaySize(context);
        return new Point(Math.max(screen.x, screen.y), Math.min(screen.x, screen.y));
    }

    public static float getDefaultCropRatio(int screenWidth, int screenHeight, Context context) {
        boolean tablet = context.getResources().getBoolean(R.bool.is_tablet);
        return tablet ? 1.5f
                : Math.min(screenWidth, screenHeight) * 2f / Math.max(screenWidth, screenHeight);
    }

    public static float getKitkatMinCropRatio(int screenWidth, int screenHeight) {
        return getPortraitCropRatio(screenWidth, screenHeight);
    }

    public static float getKitkatMaxCropRatio(int screenWidth, int screenHeight, Context context) {
        return getDefaultCropRatio(screenWidth, screenHeight, context);
    }

    public static float getPortraitCropRatio(int screenWidth, int screenHeight) {
        return (float) Math.min(screenWidth, screenHeight) / Math.max(screenWidth, screenHeight);
    }

    public static float getLandscapeCropRatio(int screenWidth, int screenHeight) {
        return (float) Math.max(screenWidth, screenHeight) / Math.min(screenWidth, screenHeight);
    }
}
