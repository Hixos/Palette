package com.hixos.smartwp.services.geofence;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Preferences;

/**
 * Created by Luca on 23/10/2014.
 */
public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences.setBoolean(context, R.string.preference_show_provider_error, false);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GeofenceService.NOTIFICATION_BOTH);
        notificationManager.cancel(GeofenceService.NOTIFICATION_WIFI);
        notificationManager.cancel(GeofenceService.NOTIFICATION_LOCATION);
    }
}
