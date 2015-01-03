package com.hixos.smartwp.services;

import android.content.Context;
import android.content.Intent;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.wallpaper.LiveWallpaperService;

public class ServiceUtils {
    public final static int SERVICE_SLIDESHOW = 1;
    public final static int SERVICE_GEOFENCE = 2;

    public static void setActiveService(Context context, int service) {
        Preferences.setInt(context, R.string.preference_active_service, service);
    }

    public static int getActiveService(Context context) {
        return Preferences.getInt(context, R.string.preference_active_service, 0);
    }

    public static void broadcastServiceActivated(Context context) {
        Intent intent = new Intent(LiveWallpaperService.ACTION_SERVICE_ACTIVATED);
        context.sendBroadcast(intent);
    }
}
