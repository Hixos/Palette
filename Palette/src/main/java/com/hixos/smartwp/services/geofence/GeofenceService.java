package com.hixos.smartwp.services.geofence;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.wallpaper.OnWallpaperChangedCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeofenceService {
    public static final int NOTIFICATION_WIFI = 1;
    public static final int NOTIFICATION_LOCATION = 2;
    public static final int NOTIFICATION_BOTH = 3;
    public static final int NOTIFICATION_PLAY_ERROR = 4;
    private final static String LOGTAG = "GeofenceService";
    private final static String ACTION_GEOFENCE_RECEIVED = "com.hixos.smartwp.geofence.ACTION_GEOFENCE_RECEIVED";
    private final static String ACTION_ADD_GEOFENCE = "com.hixos.smartwp.geofence.ACTION_ADD_GEOFENCE";
    private final static String ACTION_REMOVE_GEOFENCE = "com.hixos.smartwp.geofence.ACTION_REMOVE_GEOFENCE";
    private final static String ACTION_RELOAD_WALLPAPER = "com.hixos.smartwp.geofence.ACTION_RELOAD_WALLPAPER";
    private final static String EXTRA_UIDS = "com.hixos.smartwp.geofence.EXTRA_UIDS";
    private static GeofenceService sIstance;

    private Context mContext;

    private OnWallpaperChangedCallback mCallback;

    private GeofenceReceiver mReceiver;
    private ReloadWallpaperReceiver mReloadReceiver;
    private AddGeofenceReceiver mAddReceiver;
    private RemoveGeofenceReceiver mRemoveReceiver;
    private ProviderChangedReceiver mProviderReceiver;
    private WifiStateChangedReceiver mWifiReceiver;

    private GeofenceManager mManager;

    private boolean mStopped = false;

    private boolean mRunning = false;

    private GeofenceService(OnWallpaperChangedCallback callback, Context context) {
        mCallback = callback;
        mContext = context;
        mManager = new GeofenceManager(mContext, this);

        if (servicesConnected(context)) {
            mReceiver = new GeofenceReceiver();
            mReloadReceiver = new ReloadWallpaperReceiver();
            mAddReceiver = new AddGeofenceReceiver();
            mRemoveReceiver = new RemoveGeofenceReceiver();
            mProviderReceiver = new ProviderChangedReceiver();
            mWifiReceiver = new WifiStateChangedReceiver();

            context.registerReceiver(mReceiver, new IntentFilter(ACTION_GEOFENCE_RECEIVED));
            context.registerReceiver(mReloadReceiver, new IntentFilter(ACTION_RELOAD_WALLPAPER));
            context.registerReceiver(mAddReceiver, new IntentFilter(ACTION_ADD_GEOFENCE));
            context.registerReceiver(mRemoveReceiver, new IntentFilter(ACTION_REMOVE_GEOFENCE));
            context.registerReceiver(mProviderReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            context.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

            mRunning = MiscUtils.Location.networkLocationProviderEnabled(context);
            if (mRunning) {
                mManager.addAllGeofences();
            }
        } else {
            mStopped = true;
        }
    }

    public static void startListener(OnWallpaperChangedCallback callback, Context context) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException();
        }
        if (sIstance != null && !sIstance.isStopped()) {
            sIstance.stop();
        }
        sIstance = new GeofenceService(callback, context);
    }

    public static void stopListener(Context context) {
        if (sIstance != null && !sIstance.isStopped()) {
            sIstance.stop();
        } else if (sIstance == null) {
            GeofenceManager manager = new GeofenceManager(context, null);
            manager.removeAllGeofences();
            manager.disconnect();
        }
    }

    public static void broadcastAddGeofence(Context context, List<String> uids) {
        if (uids.size() == 0) return;

        Intent intent = new Intent(ACTION_ADD_GEOFENCE);
        String[] requestUids = new String[uids.size()];
        for (int i = 0; i < uids.size(); i++) {
            requestUids[i] = uids.get(i);
        }
        intent.putExtra(EXTRA_UIDS, requestUids);
        context.sendBroadcast(intent);
    }

    public static void broadcastRemoveGeofence(Context context, List<String> uids) {
        if (uids.size() == 0) return;

        Intent intent = new Intent(ACTION_REMOVE_GEOFENCE);
        String[] requestUids = new String[uids.size()];
        for (int i = 0; i < uids.size(); i++) {
            requestUids[i] = uids.get(i);
        }
        intent.putExtra(EXTRA_UIDS, requestUids);
        context.sendBroadcast(intent);
    }

    public static void broadcastReloadWallpaper(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_WALLPAPER);
        context.sendBroadcast(intent);
    }

    private static void showNotification(Context context, int id, int smallIcon, String title, String text,
                                         String bigText, PendingIntent contentPIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent actionIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent actionPIntent = PendingIntent.getBroadcast(context, 1, actionIntent, 0);

        builder.setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentPIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.setBigContentTitle(title).bigText(bigText);
            builder.setStyle(style);

            builder.addAction(R.drawable.ic_action_search_cancel,
                    context.getString(R.string.donot_show_again),
                    actionPIntent);
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id, builder.build());
    }

    public static Uri getBestWallpaperUri(Context context) {
        Uri uri = ImageManager.getInstance()
                .getPictureUri(GeofenceDatabase.DEFAULT_WALLPAPER_UID);

        if (FileUtils.fileExistance(uri)) return uri;
        return Uri.parse("android.resource://" + context.getPackageName() + "/"
                + R.raw.wallpaper);
    }

    private static boolean servicesConnected(Context context) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }

    private void showWifiNotification(Context context) {
        if (!MiscUtils.Location.networkLocationProviderEnabled(context)) {
            showBothNotification(context);
            return;
        }

        Intent contentIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentIntent = new Intent(new Intent(Settings.ACTION_WIFI_SETTINGS));
        } else {
            contentIntent = new Intent(context, ProviderFixActivity.class);
        }
        PendingIntent contentPIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
        showNotification(context, NOTIFICATION_WIFI, R.drawable.ic_action_logo,
                context.getString(R.string.wifi_fix_notif_title),
                context.getString(R.string.wifi_fix_notif_text),
                context.getString(R.string.wifi_fix_notif_bigtext),
                contentPIntent);
    }

    private void showNetworkNotification(Context context) {
        if (!MiscUtils.Location.wifiLocationEnabled(context)) {
            showBothNotification(context);
            return;
        }
        Intent contentIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        } else {
            contentIntent = new Intent(context, ProviderFixActivity.class);
        }
        PendingIntent contentPIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
        showNotification(context, NOTIFICATION_LOCATION, R.drawable.ic_action_logo,
                context.getString(R.string.notif_title_location_fix),
                context.getString(R.string.notif_text_location_fix),
                context.getString(R.string.notif_bigtext_location_fix),
                contentPIntent);
    }

    private void showBothNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        Intent contentIntent;
        contentIntent = new Intent(context, ProviderFixActivity.class);
        PendingIntent contentPIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
        showNotification(context, NOTIFICATION_BOTH, R.drawable.ic_action_logo,
                context.getString(R.string.notif_title_services_fix),
                context.getString(R.string.notif_text_services_fix),
                context.getString(R.string.notif_bigtext_services_fix),
                contentPIntent);
    }

    private void hideNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_BOTH);
        notificationManager.cancel(NOTIFICATION_WIFI);
        notificationManager.cancel(NOTIFICATION_LOCATION);
    }

    private void stop() {
        if (mStopped) return;

        mStopped = true;

        mContext.unregisterReceiver(mReceiver);
        mContext.unregisterReceiver(mReloadReceiver);
        mContext.unregisterReceiver(mAddReceiver);
        mContext.unregisterReceiver(mRemoveReceiver);
        mContext.unregisterReceiver(mProviderReceiver);
        mContext.unregisterReceiver(mWifiReceiver);

        GeofenceDatabase database = new GeofenceDatabase(mContext);
        database.clearActiveGeofences();

        mManager.removeAllGeofences();
        mManager.disconnect();
    }

    private boolean isStopped() {
        return mStopped;
    }

    private void setWallpaper(Context context) {
        GeofenceDatabase database = new GeofenceDatabase(context);

        List<GeofenceData> datas = database.getActiveGeowallpapers();

        if (datas.size() > 0) {
            do {
                int min = 0;
                for (int i = 1; i < datas.size(); i++) {
                    if (datas.get(i).getRadius() < datas.get(min).getRadius()) {
                        min = i;
                    }
                }
                Uri wpUri = ImageManager.getInstance().getPictureUri(datas.get(min).getUid());
                if (FileUtils.fileExistance(wpUri)) {
                    setWallpaper(wpUri);
                    return;
                }
                datas.remove(min);
            } while (datas.size() > 0);

            Uri defaultUri = ImageManager.getInstance()
                    .getPictureUri(GeofenceDatabase.DEFAULT_WALLPAPER_UID);
            if (FileUtils.fileExistance(defaultUri)) {
                setWallpaper(defaultUri);
            } else {
                Uri wallpaper = Uri.parse("android.resource://" + context.getPackageName() + "/"
                        + R.raw.wallpaper);
                setWallpaper(wallpaper);
            }
        } else {
            if (mCallback != null) {
                setWallpaper(getBestWallpaperUri(context));
            }
        }
    }

    private boolean setWallpaper(Uri wallpaperUri) {
        if (wallpaperUri != null && mCallback != null) {
            mCallback.onWallpaperChanged(wallpaperUri);
            return true;
        }
        return false;
    }

    public static class GeofenceManager implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
        private final static String LOGTAG = "GeofenceManager";

        //Highest number -> highest importance
        private final static int REQUEST_NOPE = 0;
        private final static int REQUEST_ADD = 1;
        private final static int REQUEST_REMOVE = 2;
        private final static int REQUEST_ADD_ALL = 3;
        private final static int REQUEST_REMOVE_ALL = 4;

        private Context mContext;
        private GoogleApiClient mGoogleClient;
        private GeofenceService mCallback;

        private int mRequest = REQUEST_ADD_ALL;
        private List<String> mRequestData;

        private GeofenceManager(Context context, GeofenceService callback) {
            mContext = context;
            mCallback = callback;
            initGoogleClient();
        }

        private void initGoogleClient() {
            mGoogleClient = null;
            mGoogleClient = new GoogleApiClient.Builder(mContext)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleClient.connect();
        }

        public void disconnect() {
            if (mGoogleClient != null &&
                    (mGoogleClient.isConnected() || mGoogleClient.isConnecting())) {
                mGoogleClient.disconnect();
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            Logger.fileW(mContext, LOGTAG, "Google client connected");
            switch (mRequest) {
                case REQUEST_ADD_ALL:
                    addAllGeofences();
                    break;
                case REQUEST_ADD:
                    if (mRequestData != null)
                        addGeofences(mRequestData);
                    break;
                case REQUEST_REMOVE_ALL:
                    removeAllGeofences();
                    break;
                case REQUEST_REMOVE:
                    if (mRequestData != null)
                        removeGeofence(mRequestData);
                    break;
            }
            mRequest = REQUEST_NOPE;
            mRequestData = null;
        }


        @Override
        public void onConnectionSuspended(int i) {
            Logger.e(LOGTAG, "Connection suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (connectionResult.hasResolution()) {
                PendingIntent pendingIntent = connectionResult.getResolution();
                showNotification(mContext, NOTIFICATION_PLAY_ERROR, R.drawable.ic_location_fix,
                        mContext.getString(R.string.notif_title_play_services_fix),
                        mContext.getString(R.string.notif_text_play_services_fix),
                        mContext.getString(R.string.notif_bigtext_play_services_fix),
                        pendingIntent);
            }
        }

        private void request(int requestCode, List<String> requestData) {
            if (requestCode > mRequest) {
                mRequest = requestCode;
                mRequestData = requestData;
            }
        }

        public void addAllGeofences() {
            if (mGoogleClient != null) {
                if (mGoogleClient.isConnected()) {
                    Logger.fileW(mContext, LOGTAG, "Adding all");
                    PendingIntent transition = getTransitionPendingIntent();
                    List<Geofence> geofences = new ArrayList<>();
                    GeofenceDatabase database = new GeofenceDatabase(mContext);
                    List<GeofenceData> datas = database.getGeofencesByDistance();
                    if (datas.size() > 0) {
                        for (GeofenceData data : datas) {
                            geofences.add(data.toGeofence());
                        }
                        GeofencingRequest geoRequest = new GeofencingRequest.Builder()
                                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                .addGeofences(geofences)
                                .build();
                        LocationServices.GeofencingApi.addGeofences(mGoogleClient, geoRequest, transition);
                    }
                } else {
                    Logger.fileW(mContext, LOGTAG, "(NOT_CONNECTED)Adding all");
                    request(REQUEST_ADD_ALL, null);
                    if (!mGoogleClient.isConnecting()) {
                        initGoogleClient();
                    }
                }
            }
        }

        public void removeAllGeofences() {
            if (mGoogleClient != null) {
                if (mGoogleClient.isConnected()) {
                    Logger.fileW(mContext, LOGTAG, "Removing all");
                    LocationServices.GeofencingApi.removeGeofences(mGoogleClient,
                            getTransitionPendingIntent());
                } else {
                    Logger.fileW(mContext, LOGTAG, "(NOT_CONNECTED)Removing all");
                    request(REQUEST_REMOVE_ALL, null);
                    if (!mGoogleClient.isConnecting()) {
                        initGoogleClient();
                    }
                }
            }
        }

        public void addGeofences(List<String> geofenceUids) {
            if (mGoogleClient != null) {
                if (mGoogleClient.isConnected()) {
                    Logger.fileW(mContext, LOGTAG, "Adding one");
                    PendingIntent transition = getTransitionPendingIntent();
                    List<Geofence> geofences = new ArrayList<>();
                    GeofenceDatabase database = new GeofenceDatabase(mContext);

                    for (String uid : geofenceUids) {
                        GeofenceData data = database.getGeofenceData(uid);
                        if (data != null) {
                            geofences.add(data.toGeofence());
                        }
                    }
                    GeofencingRequest geoRequest = new GeofencingRequest.Builder()
                            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                            .addGeofences(geofences)
                            .build();
                    LocationServices.GeofencingApi.addGeofences(mGoogleClient, geoRequest, transition);
                } else {
                    Logger.fileW(mContext, LOGTAG, "(NOT_CONNECTED)Adding one");
                    request(REQUEST_ADD, geofenceUids);
                    if (!mGoogleClient.isConnecting()) {
                        initGoogleClient();
                    }
                }
            }
        }

        public void removeGeofence(List<String> geofenceUids) {
            if (mGoogleClient != null) {
                if (mGoogleClient.isConnected()) {
                    Logger.fileW(mContext, LOGTAG, "Removing one");
                    LocationServices.GeofencingApi.removeGeofences(mGoogleClient, geofenceUids);
                    if (mCallback != null) {
                        mCallback.setWallpaper(mContext);
                    }
                } else {
                    Logger.fileW(mContext, LOGTAG, "(NOT_CONNECTED)Removing one");
                    request(REQUEST_REMOVE, geofenceUids);
                    if (!mGoogleClient.isConnecting()) {
                        initGoogleClient();
                    }
                }
            }
        }

        private PendingIntent getTransitionPendingIntent() {
            Intent intent = new Intent(ACTION_GEOFENCE_RECEIVED);
            return PendingIntent.getBroadcast(
                    mContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private class GeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GeofenceDatabase database = new GeofenceDatabase(context);
            GeofencingEvent geoEvent = GeofencingEvent.fromIntent(intent);
            Logger.fileW(context, LOGTAG, "Wallpaper received");
            if (geoEvent.hasError()) {
                int errorCode = geoEvent.getErrorCode();
                Logger.e("ReceiveTransitionsIntentService",
                        "Location Services error: " +
                                Integer.toString(errorCode));
                database.clearActiveGeofences();
                setWallpaper(context);
            } else {
                int transitionType = geoEvent.getGeofenceTransition();
                List<Geofence> triggerList = geoEvent.getTriggeringGeofences();

                if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    for (Geofence geofence : triggerList) {
                        database.addActiveGeowallpaper(geofence.getRequestId());
                    }
                } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    for (Geofence geofence : triggerList) {
                        database.removeActiveGeowallpaper(geofence.getRequestId());
                    }
                }
                setWallpaper(context);
            }
        }
    }

    private class AddGeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] uids = intent.getStringArrayExtra(EXTRA_UIDS);

            if (uids.length > 0) {
                List<String> requestUids = new ArrayList<>(Arrays.asList(uids));
                mManager.addGeofences(requestUids);
            }
        }
    }

    private class RemoveGeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] uids = intent.getStringArrayExtra(EXTRA_UIDS);

            if (uids.length > 0) {
                List<String> requestUids = new ArrayList<>(Arrays.asList(uids));
                mManager.removeGeofence(requestUids);
            }
        }
    }

    private class ReloadWallpaperReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setWallpaper(context);
        }
    }

    private class WifiStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Preferences.getBoolean(context, R.string.preference_show_provider_error, true)) {
                if (!MiscUtils.Location.wifiLocationEnabled(context)) {
                    showWifiNotification(context);
                } else {
                    hideNotification(context);
                }
            }
        }
    }

    private class ProviderChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean networkLocationEnabled
                    = MiscUtils.Location.networkLocationProviderEnabled(context);
            if (!mRunning && networkLocationEnabled) {
                mManager.addAllGeofences();
            }
            if (Preferences.getBoolean(context, R.string.preference_show_provider_error, true)) {
                if (!networkLocationEnabled) {
                    showNetworkNotification(context);
                } else {
                    hideNotification(context);
                }
            }
            mRunning = networkLocationEnabled;
        }
    }
}
