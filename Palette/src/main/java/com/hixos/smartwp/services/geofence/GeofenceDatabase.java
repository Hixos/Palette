package com.hixos.smartwp.services.geofence;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.GeoMath;
import com.hixos.smartwp.utils.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Luca on 11/04/2014.
 */
public class GeofenceDatabase extends DatabaseManager {
    public static final String COLUMN_DATA_ACTIVE = "[active]";
    public static final String COLUMN_DATA_COLOR = "[color]";
    public static final String COLUMN_DATA_DELETED = "[deleted]";
    public static final String COLUMN_DATA_DISTANCE = "[distance]";
    public static final String COLUMN_DATA_ID = "[_id]";
    public static final String COLUMN_DATA_LATITUDE = "[latitude]";
    public static final String COLUMN_DATA_LONGITUDE = "[longitude]";
    public static final String COLUMN_DATA_NAME = "[name]";
    public static final String COLUMN_DATA_RADIUS = "[radius]";
    public static final String COLUMN_DATA_ZOOM_LEVEL = "[zoom_level]";

    public static final String GEOFENCE_ID_PREFIX = "gf-";
    public static final String SNAPSHOT_ID_PREFIX = "sn-";

    public static final String DEFAULT_WALLPAPER_UID = GEOFENCE_ID_PREFIX + "1337";

    public static final String TABLE_DATA = "[geofence_data]";

    private static final String LOGTAG = "GeofenceDatabase";
    private static final String TAG = "geofence";

    public interface DatabaseObserver {
        public void onElementRemoved(String uid);
        public void onElementCreated(GeofenceData element);
        public void onDataSetChanged();
    }

    public interface OnElementRemovedListener {
        public void onElementRemoved(String uid);
    }

    public interface OnWallpapersLoadedListener {
        public void onWallpapersLoaded(List<GeofenceData> wallpapers);
    }

    private DatabaseObserver mObserver;
    private OnElementRemovedListener mOnElementRemovedListener;

    public void setDatabaseObserver(DatabaseObserver observer){
        mObserver = observer;
    }

    public GeofenceDatabase(Context context) {
        super(context);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getNewUid() {
        return GEOFENCE_ID_PREFIX + super.getNewUid();
    }

    public static String getSnapshotUid(String geofenceUid){
        return geofenceUid.replace(GEOFENCE_ID_PREFIX, SNAPSHOT_ID_PREFIX);
    }

    public static boolean hasDefaultWallpaper() {
        return FileUtils.fileExistance(ImageManager.getInstance()
                .getPictureUri(DEFAULT_WALLPAPER_UID));
    }

    public static void deleteDefaultWallpaper(Context context) {
        FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(DEFAULT_WALLPAPER_UID));
        FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(DEFAULT_WALLPAPER_UID));
    }

    /**
     * Creates a new geowallpaper and stores it into the database
     * @param uid Unique id
     * @param location Center location
     * @param radius Radius
     * @param color Color
     * @param distance Distance from the user position
     * @param zoomLevel Zoom level on the map
     * @return GeowallpaperData
     */
    public GeofenceData createGeowallpaper(String uid, LatLng location, float radius,
                                               int color, float distance, float zoomLevel) {
        SQLiteDatabase database = openDatabase();

        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_ID, uid);
        v.put(COLUMN_DATA_LATITUDE, location.latitude);
        v.put(COLUMN_DATA_LONGITUDE, location.longitude);
        v.put(COLUMN_DATA_RADIUS, radius);
        v.put(COLUMN_DATA_COLOR, color);
        v.put(COLUMN_DATA_DELETED, false);
        v.put(COLUMN_DATA_DISTANCE, distance);
        v.put(COLUMN_DATA_ZOOM_LEVEL, zoomLevel);
        v.put(COLUMN_DATA_NAME, "");

        database.insert(TABLE_DATA, null, v);

        Cursor c = database.query(TABLE_DATA, GeofenceData.DATA_COLUMNS,
                COLUMN_DATA_ID + " = ?", new String[]{uid}, null, null, null);
        c.moveToFirst();
        GeofenceData d = GeofenceData.fromCursor(c);
        c.close();

        closeDatabase();

        if(mObserver != null)
            mObserver.onElementCreated(d);
        return d;
    }

    public void updateGeofence(GeofenceData geoData) {
        SQLiteDatabase database = openDatabase();

        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_LATITUDE, geoData.getLatitude());
        v.put(COLUMN_DATA_LONGITUDE, geoData.getLongitude());
        v.put(COLUMN_DATA_RADIUS, geoData.getRadius());
        v.put(COLUMN_DATA_COLOR, geoData.getColor());
        v.put(COLUMN_DATA_DISTANCE, geoData.getDistance());
        v.put(COLUMN_DATA_DELETED, geoData.isDeleted());
        v.put(COLUMN_DATA_ZOOM_LEVEL, geoData.getZoomLevel());
        v.put(COLUMN_DATA_NAME, geoData.getName());

        database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = " + geoData.getUid(), null);

        closeDatabase();
    }

    public void deleteGeofence(String uid) {
        deleteGeofence(uid, true);
    }

    public void deleteGeofence(String uid, boolean notifyObserver) {
        SQLiteDatabase database = openDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_DELETED, 1);
        database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?",  new String[]{uid});
        closeDatabase();

        if(notifyObserver && mObserver != null){
            mObserver.onElementRemoved(uid);
        }
        if(mOnElementRemovedListener != null)
            mOnElementRemovedListener.onElementRemoved(uid);

    }

    public void adapterDeleteGeofence(String uid){
        AsyncTask<String, Void, Void> deleteTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                deleteGeofence(strings[0], false);
                return null;
            }
        };
        deleteTask.execute(uid);
    }

    public List<String> getDeletedGeofences(){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA,
                new String[]{COLUMN_DATA_ID}, COLUMN_DATA_DELETED + " = 1", null, null, null, null);

        List<String> deleted = new ArrayList<String>();
        c.moveToFirst();

        while (!c.isAfterLast()){
            deleted.add(c.getString(0));
            c.moveToNext();
        }

        c.close();
        closeDatabase();
        return deleted;
    }

    public void restoreGeofences() {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA_DELETED, false);
        database.update(TABLE_DATA, values, COLUMN_DATA_DELETED + " = 1", null);
        closeDatabase();
    }

    public void restoreGeofencesAsync(){
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                restoreGeofences();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(mObserver != null){
                    mObserver.onDataSetChanged();
                }
            }
        };
        task.execute();
    }

    /**
     * Permanently deletes all the logically deleted geowallpapers and all the files
     * associated with them.
     */
    public void clearDeletedGeofences() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[] {COLUMN_DATA_ID, COLUMN_DATA_DELETED}, COLUMN_DATA_DELETED + " = 1", null, null, null, null);
        c.moveToFirst();

        if(c.isAfterLast()) return;

        List<String> deletedIds = new ArrayList<String>();

        do {
            deletedIds.add(c.getString(0));
            c.moveToNext();
        }while(!c.isAfterLast());

        database.delete(TABLE_DATA, COLUMN_DATA_DELETED + " = 1", null);

        for(String uid : deletedIds) {
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(getSnapshotUid(uid)));
        }
        c.close();
        closeDatabase();
    }

    public void clearDeletedGeofencesAsync(){
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                clearDeletedGeofences();
                return null;
            }
        };
        task.execute();
    }

    public int getDeletedGeofencesCount(){
        SQLiteDatabase database = openDatabase();
        String query = "SELECT count(" + COLUMN_DATA_ID + ") FROM " + TABLE_DATA + " WHERE "
                + COLUMN_DATA_DELETED + " = 1";
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();
        int count = 0;
        if(!cursor.isAfterLast()){
            count = cursor.getInt(0);
        }
        cursor.close();
        closeDatabase();
        return count;
    }

    /**
     * Returns the Geowallpaper of the given id
     * @param uid
     * @return The geowallpaper, or null if the geowallpaper doesn't exist
     */
    public GeofenceData getGeofenceData(String uid) {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, GeofenceData.DATA_COLUMNS,
                COLUMN_DATA_ID + " = ?", new String[]{uid},
                null, null, null);
        c.moveToFirst();


        if(!c.isAfterLast()) {
            GeofenceData d = GeofenceData.fromCursor(c);
            c.close();
            return d;
        }else{
            c.close();
            return null;
        }
    }


    /**
     * Returns the name associated with the geowallpaper of the given id
     * @param uid
     * @return Name, or empty String if geowallpaper does not have a name
     */
    public String getGeofenceName(String uid){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{COLUMN_DATA_NAME},
                COLUMN_DATA_ID + " = ?",  new String[]{uid}, null, null, null);
        c.moveToFirst();
        String name = "";
        if(!c.isAfterLast()){
            name = c.getString(0);
        }

        c.close();
        closeDatabase();
        return name;
    }

    /**
     * Sets the name to the geowallpaper of the given id
     * @param uid Uid of the geowallpaper
     * @param name Name to assign
     */
    public void setGeofenceName(String uid, String name){
        SQLiteDatabase database = openDatabase();

        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_NAME, name);
        database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?",  new String[]{uid});
        closeDatabase();
    }

    public void getGeofencesByDistanceAsync(final OnWallpapersLoadedListener listener){
        if(listener == null) throw new IllegalArgumentException("Listener must be defined!");

        AsyncTask<Void, Void, List<GeofenceData>> task = new AsyncTask<Void, Void, List<GeofenceData>>() {
            @Override
            protected List<GeofenceData> doInBackground(Void... voids) {
                return getGeofencesByDistance();
            }

            @Override
            protected void onPostExecute(List<GeofenceData> geofenceDatas) {
                super.onPostExecute(geofenceDatas);
                listener.onWallpapersLoaded(geofenceDatas);
            }
        };
        task.execute();
    }

    /**
     * Returns the list of geowallpapers ordered by distance
     * @return
     */
    public ArrayList<GeofenceData> getGeofencesByDistance() {
        SQLiteDatabase database = openDatabase();
        ArrayList<GeofenceData> out = new ArrayList<GeofenceData>();
        Cursor c = database.query(TABLE_DATA, GeofenceData.DATA_COLUMNS,
                COLUMN_DATA_DELETED + " = 0", null, null, null, COLUMN_DATA_DISTANCE + " ASC");
        c.moveToFirst();
        if(!c.isAfterLast()) {
            do {
                out.add(GeofenceData.fromCursor(c));
                c.moveToNext();
            }while(!c.isAfterLast());
        }
        c.close();
        return out;
    }

    /**
     * Returns the number of geowallpapers currently stored in the database
     * @return Number of geowallpapers
     */
    public int getGeofenceCount(){
        SQLiteDatabase database = openDatabase();
        Cursor data = database.query(TABLE_DATA, new String[] {COLUMN_DATA_ID, COLUMN_DATA_DELETED},
                COLUMN_DATA_DELETED + " = 0", null, null, null, null);
        int size = 0;
        data.moveToFirst();

        while(!data.isAfterLast()) {
            size++;
            data.moveToNext();
        }

        data.close();
        closeDatabase();
        return size;
    }

    /**
     * Returns the least used color by geowallpapers
     * @return
     */
    public int getLeastUsedColor(){
        SQLiteDatabase database = openDatabase();
        TypedArray tcolors = getContext().getResources().obtainTypedArray(R.array.geofence_colors);
        Map<Integer, Integer> colors = new HashMap<Integer, Integer>();

        for(int index = 0; index < tcolors.length(); index++){
            colors.put(tcolors.getColor(index,
                    getContext().getResources().getColor(R.color.geofence_default_color)), 0);
        }


        Cursor c = database.rawQuery("SELECT " + COLUMN_DATA_COLOR + ", " +
                "count(" + COLUMN_DATA_COLOR + ") FROM " + TABLE_DATA +
                " WHERE " + COLUMN_DATA_DELETED + " = 0 " +
                " GROUP BY " + COLUMN_DATA_COLOR, null);
        c.moveToFirst();
        while (!c.isAfterLast()){
            if(colors.containsKey(c.getInt(0)))
                colors.put(c.getInt(0), c.getInt(1));
            c.moveToNext();
        }

        c.close();
        closeDatabase();

        int outColor = getContext().getResources().getColor(
                R.color.geofence_default_color), uses = Integer.MAX_VALUE;
        for(int index = 0; index < tcolors.length(); index++){
            Integer color = tcolors.getColor(index, getContext().getResources().getColor(
                    R.color.geofence_default_color));
            if(colors.get(color) < uses){
                uses = colors.get(color);
                outColor = color;
                if(uses == 0){
                    break;
                }
            }
        }
        tcolors.recycle();
        return outColor;
    }

    /**
     * Returns the location associated with the distances values of the geofencepapers
     * @return LatLng, or null if not last location
     */
    public LatLng getLastLocation() {
        boolean hasLocation = Preferences.getBoolean(getContext(), R.string.preference_geofence_last_location_stored, false);
        if(hasLocation){
            float lat = Preferences.getFloat(getContext(), R.string.preference_geofence_last_location_lat, 45);
            float lon = Preferences.getFloat(getContext(), R.string.preference_geofence_last_location_lon, 8);
            return new LatLng(lat, lon);
        }
        return null;
    }

    /**
     * Sets the last known user location and updates the geofencepapers distances.
     * @param location The user location
     */
    public void setLastLocation(Location location) {
        if(location == null) {
            Preferences.setBoolean(getContext(), R.string.preference_geofence_last_location_stored, false);
            updateDistances(null);
            return;
        }
        Preferences.setBoolean(getContext(), R.string.preference_geofence_last_location_stored, true);
        Preferences.setFloat(getContext(), R.string.preference_geofence_last_location_lat, (float)location.getLatitude());
        Preferences.setFloat(getContext(), R.string.preference_geofence_last_location_lon, (float)location.getLongitude());

        updateDistances(location);
    }

    /**
     * Return the distance of the given geowallpaper from the user's last known location
     * @param uid
     * @return
     */
    public float getDistance(String uid){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{COLUMN_DATA_DISTANCE},
                COLUMN_DATA_ID + " = ?",  new String[]{uid}, null, null, null);
        c.moveToFirst();
        float dist = -1;
        if(!c.isAfterLast()){
            dist = c.getFloat(0);
        }
        c.close();
        closeDatabase();
        return dist;
    }

    /**
     * Updates the distance of all geowallpapers
     */
    private void updateDistances(Location location) {
        SQLiteDatabase database = openDatabase();
        boolean n = location == null;

        ArrayList<GeofenceData> data = new ArrayList<GeofenceData>();
        Cursor c = database.query(TABLE_DATA, GeofenceData.DATA_COLUMNS, null, null, null, null,
                null);
        c.moveToFirst();
        if(!c.isAfterLast()) {
            do {
                data.add(GeofenceData.fromCursor(c));
                c.moveToNext();
            }while(!c.isAfterLast());
        }
        c.close();

        for(GeofenceData d : data) {
            float distance = n ? -1 : GeoMath.getDistance(location.getLatitude(), location.getLongitude(),
                    d.getLatitude(), d.getLongitude()) - d.getRadius();
            if(!n && distance < 0) distance = 0;
            ContentValues v = new ContentValues();
            v.put(COLUMN_DATA_DISTANCE, distance);
            database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?", new String[]{d.getUid()});
        }
        closeDatabase();
    }

    /**
     * Returns a list of currently active geowallpapers
     * @return
     */
    public ArrayList<GeofenceData> getActiveGeowallpapers(){
        SQLiteDatabase database = openDatabase();
        ArrayList<GeofenceData> out = new ArrayList<GeofenceData>();
        Cursor cursor = database.query(TABLE_DATA, GeofenceData.DATA_COLUMNS,
                COLUMN_DATA_DELETED + " = 0 AND " + COLUMN_DATA_ACTIVE + " = 1",
                null, null, null, null);

        cursor.moveToFirst();
        if(!cursor.isAfterLast()) {
            do {
                out.add(GeofenceData.fromCursor(cursor));
                cursor.moveToNext();
            }while(!cursor.isAfterLast());
        }
        cursor.close();
        closeDatabase();
        return out;
    }

    /**
     * Adds a new active geowallpaper
     * @param uid
     */
    public void addActiveGeowallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_ACTIVE, true);
        database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?",  new String[]{uid});
        closeDatabase();
    }

    /**
     * Removes the geowallpaper of the given id from the active geowallpapers
     * @param uid uid
     */
    public void removeActiveGeowallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_ACTIVE, false);
        database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?",  new String[]{uid});
        closeDatabase();
    }

    /**
     * Removes all the active geowallpapers
     */
    public void clearActiveGeofences(){
        SQLiteDatabase database = openDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_ACTIVE, false);
        database.update(TABLE_DATA, v, COLUMN_DATA_ACTIVE + " = 1", null);
        closeDatabase();
    }

    public void setOnElementRemovedListener(OnElementRemovedListener listener){
        mOnElementRemovedListener = listener;
    }
}
