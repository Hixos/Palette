package com.hixos.smartwp.triggers.timeofday;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.wallpaper.OnWallpaperChangedCallback;

import java.util.Calendar;

/**
 * Created by Luca on 27/02/2015.
 */
public class TimeOfDayService {
    private final static String LOGTAG = "TimeOfDayService";

    private final static String ACTION_TIMEOFDAY_SERVICE = "com.hixos.smartwp.timeofday.ACTION_TIMEOFDAY_SERVICE";
    private final static String ACTION_RELOAD_WALLPAPER = "com.hixos.smartwp.timeofday.ACTION_RELOAD_WALLPAPER";

    private static TimeOfDayService sIstance;

    private final OnWallpaperChangedCallback mCallback;

    private final TimeOfDayServiceReceiver mReceiver;
    private final ReloadWallpaperReceiver mReloadReceiver;

    private boolean mStopped = false;

    private TimeOfDayService(OnWallpaperChangedCallback callback, Context context) {
        mCallback = callback;

        mReceiver = new TimeOfDayServiceReceiver();
        mReloadReceiver = new ReloadWallpaperReceiver();

        context.registerReceiver(mReceiver, new IntentFilter(ACTION_TIMEOFDAY_SERVICE));
        context.registerReceiver(mReloadReceiver, new IntentFilter(ACTION_RELOAD_WALLPAPER));
    }
    public static void startListener(OnWallpaperChangedCallback callback, Context context) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException();
        }
        if (sIstance != null && !sIstance.isStopped()) {
            sIstance.stop(context);
        }
        sIstance = new TimeOfDayService(callback, context);

        startAlarm(context);
    }

    public static void stopListener(Context context) {
        if (sIstance != null) {
            sIstance.stop(context);
        }
        cancelAlarm(context);
    }

    private static void startAlarm(Context context) {
        Intent intent = new Intent(ACTION_TIMEOFDAY_SERVICE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 123, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT >= 19){
            Logger.w(LOGTAG, "First alarm nominal");
            manager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 1, pi);
        }else{
            manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1, pi);
        }
    }

    private static void cancelAlarm(Context context) {
        Logger.w(LOGTAG, "Alarm canceled");
        Intent intent = new Intent(ACTION_TIMEOFDAY_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 123, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
    }

    public static void broadcastReloadWallpaper(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_WALLPAPER);
        context.sendBroadcast(intent);
    }

    public static Uri getBestWallpaperUri(Context context) {
        Calendar calendar = Calendar.getInstance();
        TimeOfDayDB database = new TimeOfDayDB(context);
        TimeOfDayWallpaper current = database.getCurrentWallpaper(calendar);
        if (current != null) {
            Logger.w(LOGTAG, "Wallpaper is not null");
            Uri uri = ImageManager.getInstance().getPictureUri(current.getUid());
            if (FileUtils.fileExistance(uri)) return uri;
            else Logger.e(LOGTAG, "Wallpaper file not found");
        }else{
            Uri def = ImageManager.getInstance().getPictureUri(TimeOfDayDB.DEFAULT_WALLPAPER_UID);
            if(FileUtils.fileExistance(def)) {
                Logger.w(LOGTAG, "Returning default wallpaper");
                return def;
            }
        }
        Logger.w(LOGTAG + ".getBest", "Wallpaper not found, returning default");
        return Uri.parse("android.resource://" + context.getPackageName() + "/"
                + R.raw.wallpaper);
    }

    private void stop(Context context) {
        if (mStopped) return;

        mStopped = true;

        context.unregisterReceiver(mReceiver);
        context.unregisterReceiver(mReloadReceiver);
    }

    private boolean isStopped() {
        return mStopped;
    }

    private void updateWallpaper(Context context){
        Logger.w(LOGTAG, "Updating wallpaper");
        Calendar calendar = Calendar.getInstance();
        TimeOfDayDB database = new TimeOfDayDB(context);
        TimeOfDayWallpaper current = database.getCurrentWallpaper(calendar);
        Calendar next;
        if(current != null){
            Logger.fileW(context, LOGTAG, "Current: %s", current.getStartHour().toString());
            next = current.getEndHour().toCalendar();
        }else{
            next = database.getNextWallpaperStart(calendar);
        }

        setWallpaper(context, current);

        if(next != null){
            Intent nextIntent = new Intent(ACTION_TIMEOFDAY_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(context, 123, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Logger.fileW(context, LOGTAG, "Next alarm nominal: %d %d:%d", next.get(Calendar.DAY_OF_MONTH),
                    next.get(Calendar.HOUR_OF_DAY), next.get(Calendar.MINUTE));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                manager.setExact(AlarmManager.RTC, next.getTimeInMillis(), pi);
            }else{
                manager.set(AlarmManager.RTC, next.getTimeInMillis(), pi);
            }
        }
    }

    private boolean setWallpaper(Context context, TimeOfDayWallpaper wallpaper) {
        if (wallpaper != null) {
            mCallback.onWallpaperChanged(
                    ImageManager.getInstance().getPictureUri(wallpaper.getUid()));
            return true;
        }else{
            Uri def = ImageManager.getInstance().getPictureUri(TimeOfDayDB.DEFAULT_WALLPAPER_UID);
            if(FileUtils.fileExistance(def)){
                Logger.w(LOGTAG, "Setting default wallpaper");
                mCallback.onWallpaperChanged(def, true);
            }else {
                Uri bck = Uri.parse("android.resource://" + context.getPackageName() + "/"
                        + R.raw.wallpaper);
                Logger.e(LOGTAG + ".receive", "Wallpaper not found, using default");
                //TODO: show notification
                mCallback.onWallpaperChanged(bck, true);
            }
        }
        return false;
    }

    private class TimeOfDayServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.w(LOGTAG, "Alarm received");
            updateWallpaper(context);
        }
    }

    private class ReloadWallpaperReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.w(LOGTAG, "Reload received");
            updateWallpaper(context);
        }
    }
}
