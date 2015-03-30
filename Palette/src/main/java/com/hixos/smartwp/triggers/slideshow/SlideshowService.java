package com.hixos.smartwp.triggers.slideshow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.wallpaper.OnWallpaperChangedCallback;

public class SlideshowService {
    private final static String LOGTAG = "SlideshowService";

    private final static String ACTION_SLIDESHOW_SERVICE = "com.hixos.smartwp.slideshow.ACTION_SLIDESHOW_SERVICE";
    private final static String ACTION_CHANGE_INTERVAL = "com.hixos.smartwp.slideshow.ACTION_CHANGE_INTERVAL";
    private final static String ACTION_RELOAD_WALLPAPER = "com.hixos.smartwp.slideshow.ACTION_RELOAD_WALLPAPER";


    private static SlideshowService sIstance;

    private final OnWallpaperChangedCallback mCallback;

    private final SlideshowServiceReceiver mReceiver;
    private final ChangeIntervalReceiver mIntervalReceiver;
    private final ReloadWallpaperReceiver mReloadReceiver;

    private boolean mStopped = false;

    private SlideshowService(OnWallpaperChangedCallback callback, Context context) {
        mCallback = callback;

        mReceiver = new SlideshowServiceReceiver();
        mIntervalReceiver = new ChangeIntervalReceiver();
        mReloadReceiver = new ReloadWallpaperReceiver();

        context.registerReceiver(mReceiver, new IntentFilter(ACTION_SLIDESHOW_SERVICE));
        context.registerReceiver(mIntervalReceiver, new IntentFilter(ACTION_CHANGE_INTERVAL));
        context.registerReceiver(mReloadReceiver, new IntentFilter(ACTION_RELOAD_WALLPAPER));
    }

    public static void startListener(OnWallpaperChangedCallback callback, Context context) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException();
        }
        if (sIstance != null && !sIstance.isStopped()) {
            sIstance.stop(context);
        }
        sIstance = new SlideshowService(callback, context);

        startAlarm(context);
    }

    public static void stopListener(Context context) {
        if (sIstance != null) {
            sIstance.stop(context);
        }
        cancelAlarm(context);
    }

    private static void startAlarm(Context context) {
        Intent intent = new Intent(ACTION_SLIDESHOW_SERVICE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 123, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        SlideshowDatabase database = new SlideshowDatabase(context);
        long interval = database.getIntervalMs();
        interval = Math.max(60000, interval);
        manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + interval,
                interval, pi);
    }

    private static void cancelAlarm(Context context) {
        Intent intent = new Intent(ACTION_SLIDESHOW_SERVICE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 123, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
    }

    private static void updateInterval(Context context) {
        startAlarm(context);
    }

    public static void broadcastIntervalChanged(Context context) {
        Intent intent = new Intent(ACTION_CHANGE_INTERVAL);
        context.sendBroadcast(intent);
    }

    public static void broadcastReloadWallpaper(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_WALLPAPER);
        context.sendBroadcast(intent);
    }

    public static Uri getBestWallpaperUri(Context context) {
        SlideshowDatabase database = new SlideshowDatabase(context);
        SlideshowData current = database.getCurrentWallpaper();
        if (current != null) {
            Uri uri = ImageManager.getInstance().getPictureUri(current.getUid());
            if (FileUtils.fileExistance(uri)) return uri;
        }
        Logger.w(LOGTAG + ".getBest", "Wallpaper not found, returning default");
        return Uri.parse("android.resource://" + context.getPackageName() + "/"
                + R.raw.wallpaper);
    }

    private void stop(Context context) {
        if (mStopped) return;

        mStopped = true;

        context.unregisterReceiver(mReceiver);
        context.unregisterReceiver(mIntervalReceiver);
        context.unregisterReceiver(mReloadReceiver);
    }

    private boolean isStopped() {
        return mStopped;
    }

    private boolean setWallpaper(Uri wallpaperUri) {
        if (wallpaperUri != null) {
            mCallback.onWallpaperChanged(wallpaperUri);
            return true;
        }
        return false;
    }

    private static class ChangeIntervalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateInterval(context);
        }
    }

    private class SlideshowServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SlideshowDatabase database = new SlideshowDatabase(context);
            SlideshowData next = database.nextWallpaper();
            boolean success = false;
            if (next != null) {
                success = setWallpaper(ImageManager.getInstance()
                        .getPictureUri(next.getUid()));
            }
            if (!success) {
                Uri wallpaper = Uri.parse("android.resource://" + context.getPackageName() + "/"
                        + R.raw.wallpaper);
                Logger.w(LOGTAG + ".receive", "Wallpaper not found, using default");
                setWallpaper(wallpaper);
            }
        }
    }

    private class ReloadWallpaperReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.w(LOGTAG + ".reload", "Reload");
            setWallpaper(getBestWallpaperUri(context));
        }
    }
}
