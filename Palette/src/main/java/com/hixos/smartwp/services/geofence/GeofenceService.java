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
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
//import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.wallpaper.OnWallpaperChangedCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 17/04/2014.
 */
public class GeofenceService{
    private final static String LOGTAG = "GeofenceService";

    private final static String ACTION_GEOFENCE_RECEIVED = "com.hixos.smartwp.geofence.ACTION_GEOFENCE_RECEIVED";

    private final static String ACTION_ADD_GEOFENCE = "com.hixos.smartwp.geofence.ACTION_ADD_GEOFENCE";
    private final static String ACTION_REMOVE_GEOFENCE = "com.hixos.smartwp.geofence.ACTION_REMOVE_GEOFENCE";
    private final static String ACTION_RELOAD_WALLPAPER = "com.hixos.smartwp.geofence.ACTION_RELOAD_WALLPAPER";

    private final static String EXTRA_UIDS = "com.hixos.smartwp.geofence.EXTRA_UIDS";

    public static final int NOTIFICATION_WIFI = 1;
    public static final int NOTIFICATION_LOCATION = 2;
    public static final int NOTIFICATION_BOTH = 3;

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

    public static void startListener(OnWallpaperChangedCallback callback, Context context){
        if(context == null || callback == null){
            throw new IllegalArgumentException();
        }
        if(sIstance != null && !sIstance.isStopped()){
            sIstance.stop();
        }
        sIstance = new GeofenceService(callback, context);
    }

    public static void stopListener(Context context){
        if(sIstance != null && !sIstance.isStopped()) {
            sIstance.stop();
        }else if(sIstance == null){
            GeofenceManager manager = new GeofenceManager(context, null);
            manager.sendRequest(GeofenceManager.REQUEST_REMOVE_ALL);
        }
    }

    public static void broadcastAddGeofence(Context context, List<String> uids){
        if(uids.size() == 0) return;

        Intent intent = new Intent(ACTION_ADD_GEOFENCE);
        String[] requestUids = new String[uids.size()];
        for(int i = 0; i < uids.size(); i++){
            requestUids[i] = uids.get(i);
        }
        intent.putExtra(EXTRA_UIDS, requestUids);
        context.sendBroadcast(intent);
    }

    public static void broadcastRemoveGeofence(Context context, List<String> uids) {
        if(uids.size() == 0) return;

        Intent intent = new Intent(ACTION_REMOVE_GEOFENCE);
        String[] requestUids = new String[uids.size()];
        for(int i = 0; i < uids.size(); i++){
            requestUids[i] = uids.get(i);
        }
        intent.putExtra(EXTRA_UIDS, requestUids);
        context.sendBroadcast(intent);
    }

    public static void broadcastReloadWallpaper(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_WALLPAPER);
        context.sendBroadcast(intent);
    }

    private void showWifiNotification(Context context){
        if(!MiscUtils.Location.networkLocationProviderEnabled(context)) {
            showBothNotification(context);
            return;
        }

        Intent contentIntent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            contentIntent = new Intent(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }else{
            contentIntent = new Intent(context,ProviderFixActivity.class);
        }
        PendingIntent contentPIntent = PendingIntent.getActivity(context,0, contentIntent, 0);
        showNotification(context, NOTIFICATION_WIFI, R.drawable.ic_action_logo,
                context.getString(R.string.wifi_fix_notif_title),
                context.getString(R.string.wifi_fix_notif_text),
                context.getString(R.string.wifi_fix_notif_bigtext),
                contentPIntent);
    }

    private void showNetworkNotification(Context context) {
        if(!MiscUtils.Location.wifiLocationEnabled(context)) {
            showBothNotification(context);
            return;
        }
        Intent contentIntent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            contentIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        }else{
            contentIntent = new Intent(context,ProviderFixActivity.class);
        }
        PendingIntent contentPIntent = PendingIntent.getActivity(context,0, contentIntent, 0);
        showNotification(context, NOTIFICATION_LOCATION, R.drawable.ic_action_logo,
                context.getString(R.string.location_fix_notif_title),
                context.getString(R.string.location_fix_notif_text),
                context.getString(R.string.location_fix_notif_bigtext),
                contentPIntent);
    }

    private void showBothNotification(Context context){
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        Intent contentIntent;
        contentIntent = new Intent(context,ProviderFixActivity.class);
        PendingIntent contentPIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
        showNotification(context, NOTIFICATION_BOTH, R.drawable.ic_action_logo,
                context.getString(R.string.services_fix_notif_title),
                context.getString(R.string.services_fix_notif_text),
                context.getString(R.string.services_fix_notif_bigtext),
                contentPIntent);
    }

    private void showNotification(Context context, int id, int smallIcon, String title, String text,
                                  String bigText, PendingIntent contentPIntent){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent actionIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent actionPIntent = PendingIntent.getBroadcast(context, 1, actionIntent, 0);

        builder.setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentPIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
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

    private void hideNotification(Context context){
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_BOTH);
        notificationManager.cancel(NOTIFICATION_WIFI);
        notificationManager.cancel(NOTIFICATION_LOCATION);
    }

    private GeofenceService(OnWallpaperChangedCallback callback, Context context){
        mCallback = callback;
        mContext = context;
        mManager = new GeofenceManager(mContext, this);
        if(servicesConnected(context)){
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
            if(mRunning){
                mManager.sendRequest(GeofenceManager.REQUEST_ADD_ALL);
            }
        }else {
            mStopped = true;
        }
    }

    private void stop(){
        if(mStopped) return;

        mStopped = true;

        mContext.unregisterReceiver(mReceiver);
        mContext.unregisterReceiver(mReloadReceiver);
        mContext.unregisterReceiver(mAddReceiver);
        mContext.unregisterReceiver(mRemoveReceiver);
        mContext.unregisterReceiver(mProviderReceiver);
        mContext.unregisterReceiver(mWifiReceiver);

        GeofenceDatabase database = new GeofenceDatabase(mContext);
        database.clearActiveGeofences();

        mManager.sendRequest(GeofenceManager.REQUEST_REMOVE_ALL);
    }

    private boolean isStopped(){
        return mStopped;
    }

    private class GeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GeofenceDatabase database = new GeofenceDatabase(context);

          /*  if (LocationClient.hasError(intent)) {
                int errorCode = LocationClient.getErrorCode(intent);
                Logger.e("ReceiveTransitionsIntentService",
                        "Location Services error: " +
                                Integer.toString(errorCode));
                database.clearActiveGeofences();
                setWallpaper(context);
            } else {
                int transitionType = LocationClient.getGeofenceTransition(intent);
                List <Geofence> triggerList = LocationClient.getTriggeringGeofences(intent);


                if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    for(Geofence geofence : triggerList){
                        database.addActiveGeowallpaper(geofence.getRequestId());
                    }
                }else if(transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    for(Geofence geofence : triggerList){
                        database.removeActiveGeowallpaper(geofence.getRequestId());
                    }
                }

                setWallpaper(context);
            }*/
        }
    }

    private void setWallpaper(Context context){
        GeofenceDatabase database = new GeofenceDatabase(context);

        List<GeofenceData> datas = database.getActiveGeowallpapers();

        if(datas.size() > 0){
            do {
                int min = 0;
                for(int i = 1; i < datas.size(); i++){
                    if(datas.get(i).getRadius() < datas.get(min).getRadius()){
                        min = i;
                    }
                }
                Uri wpUri = ImageManager.getInstance().getPictureUri(datas.get(min).getUid());
                if(FileUtils.fileExistance(wpUri)){
                    setWallpaper(wpUri);
                    return;
                }
                datas.remove(min);
            }while (datas.size() > 0);

            Uri defaultUri = ImageManager.getInstance()
                    .getPictureUri(GeofenceDatabase.DEFAULT_WALLPAPER_UID);
            if(FileUtils.fileExistance(defaultUri)) {
                setWallpaper(defaultUri);
            }else{
                Uri wallpaper = Uri.parse("android.resource://" + context.getPackageName() + "/"
                        + R.raw.wallpaper);
                setWallpaper(wallpaper);
            }
        }else {
            if(mCallback != null) {
                setWallpaper(getBestWallpaperUri(context));
            }
        }
    }

    private boolean setWallpaper(Uri wallpaperUri){
        if(wallpaperUri != null && mCallback != null){
            mCallback.onWallpaperChanged(wallpaperUri);
            return true;
        }
        return false;
    }

    public static Uri getBestWallpaperUri(Context context){
        Uri uri = ImageManager.getInstance()
                .getPictureUri(GeofenceDatabase.DEFAULT_WALLPAPER_UID);

        if(FileUtils.fileExistance(uri)) return uri;
        return Uri.parse("android.resource://" + context.getPackageName() + "/"
                    + R.raw.wallpaper);
    }

    private static boolean servicesConnected(Context context) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }

    private class AddGeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] uids = intent.getStringArrayExtra(EXTRA_UIDS);

            if(uids.length > 0){
                List<String> requestUids = new ArrayList<String>();
                requestUids.clear();
                for(String s : uids){
                    requestUids.add(s);
                }
                mManager.sendRequest(GeofenceManager.REQUEST_ADD, requestUids);
            }
        }
    }

    private class RemoveGeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] uids = intent.getStringArrayExtra(EXTRA_UIDS);

            if(uids.length > 0){
                List<String> requestUids = new ArrayList<String>();
                requestUids.clear();
                for(String l : uids){
                    requestUids.add(l);
                }
                mManager.sendRequest(GeofenceManager.REQUEST_REMOVE, requestUids);
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
            if(Preferences.getBoolean(context, R.string.preference_show_provider_error, true)){
                if(!MiscUtils.Location.wifiLocationEnabled(context)){
                    showWifiNotification(context);
                }else{
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
            if(!mRunning && networkLocationEnabled){
                mManager.sendRequest(GeofenceManager.REQUEST_ADD_ALL);
            }
            if(Preferences.getBoolean(context, R.string.preference_show_provider_error, true)) {
                if (!networkLocationEnabled) {
                    showNetworkNotification(context);
                } else {
                    hideNotification(context);
                }
            }
            mRunning = networkLocationEnabled;
        }
    }

    public static class GeofenceManager implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener
        //LocationClient.OnAddGeofencesResultListener,
       // LocationClient.OnRemoveGeofencesResultListener{
    {

        private final static String LOGTAG = "GeofenceManager";

        public final static int REQUEST_ADD_ALL = 1;
        public final static int REQUEST_REMOVE_ALL = 2;
        public final static int REQUEST_ADD = 3;
        public final static int REQUEST_REMOVE = 4;

        private Context mContext;

       // private LocationClient mLocationClient;
        private int mRequest = 0;
        private List<String> mRequestUids = new ArrayList<String>();

        private GeofenceService mCallback;

        private GeofenceManager(Context context, GeofenceService callback) {
            mContext = context;
            mCallback = callback;
        }

        public void sendRequest(int request){
            sendRequest(request, null);
        }

        public void sendRequest(int request, List<String> requestUids){
            if(mRequest == 0){
               // mLocationClient = new LocationClient(mContext, this, this);
                //mLocationClient.connect();
                mRequest = request;
                mRequestUids = requestUids == null ? new ArrayList<String>() : requestUids;
            }else{
                Logger.fileW(mContext, LOGTAG, "LocationClient Busy");
                Logger.e(LOGTAG, "LocationClient busy");
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            switch (mRequest){
                case REQUEST_ADD_ALL: {
                    PendingIntent transition = getTransitionPendingIntent();
                    List<Geofence> geofences = new ArrayList<Geofence>();
                    GeofenceDatabase database = new GeofenceDatabase(mContext);
                    List<GeofenceData> datas = database.getGeofencesByDistance();
                    if(datas.size() <= 0) break;
                    for(GeofenceData data : datas){
                        geofences.add(data.toGeofence());
                    }
                    ///mLocationClient.addGeofences(geofences, transition, this);
                    Logger.fileW(mContext, LOGTAG, "Adding all");
                    break;
                }
                case REQUEST_REMOVE_ALL: {
                    //mLocationClient.removeGeofences(getTransitionPendingIntent(), this);
                    Logger.fileW(mContext, LOGTAG, "Removing all");
                    break;
                }
                case REQUEST_ADD: {
                    PendingIntent transition = getTransitionPendingIntent();
                    List<Geofence> geofences = new ArrayList<Geofence>();
                    GeofenceDatabase database = new GeofenceDatabase(mContext);

                    for(String uid : mRequestUids){
                        GeofenceData data = database.getGeofenceData(uid);
                        if(data != null){
                            geofences.add(data.toGeofence());
                        }
                    }

                    //mLocationClient.addGeofences(geofences, transition, this);
                    Logger.fileW(mContext, LOGTAG, "Add one");
                    break;
                }
                case REQUEST_REMOVE: {
                    List<String> requestUids = new ArrayList<String>();
                    for(String uid : mRequestUids){
                        requestUids.add(uid);
                    }
                    //mLocationClient.removeGeofences(requestUids, this);
                    Logger.fileW(mContext, LOGTAG, "Remove one");
                    if(mCallback != null) {
                        mCallback.setWallpaper(mContext);
                    }
                    break;
                }
            }
        }

        @Override
        public void onDisconnected() {
            //mLocationClient = null;
        }

      /*  @Override
        public void onAddGeofencesResult(int statusCode, String[] ids) {
            if(statusCode != LocationStatusCodes.SUCCESS){
                Logger.e(LOGTAG, "Error adding geofence(s): " + statusCode);
            }
            mRequest = 0;
            //mLocationClient.disconnect();
        }*/

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Logger.e(LOGTAG, "Error connecting to locationclient");
        }

   /*     @Override
        public void onRemoveGeofencesByRequestIdsResult(int i, String[] strings) {
            Logger.fileW(mContext, LOGTAG, "Removed by ID");
            mRequest = 0;
            //mLocationClient.disconnect();
        }*/

       /* @Override
        public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent) {
            mRequest = 0;
           // mLocationClient.disconnect();
            Logger.fileW(mContext, LOGTAG, "Removed by intent");
        }*/

        //
        private PendingIntent getTransitionPendingIntent() {
            Intent intent = new Intent(ACTION_GEOFENCE_RECEIVED);
            return PendingIntent.getBroadcast(
                    mContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
