package com.hixos.smartwp.triggers.geofence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.Wallpaper;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.GeoMath;
import com.hixos.smartwp.utils.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hixos on 13/09/2015.
 */
public class GeofenceDB extends DatabaseManager {
    public static final String GEOFENCE_ID_PREFIX = "geo-";
    public static final String GEOFENCE_SNAPSHOT_ID_PREFIX = "geo-snap-";
    public static final String DEFAULT_WALLPAPER_UID = GEOFENCE_ID_PREFIX + "1337";
    private final static String LOGTAG = "GeofenceDB";
    private final static String COLUMNS[] = {
            Wallpaper.COLUMN_UID,
            Wallpaper.COLUMN_DELETED,
            GeofenceWallpaper.COLUMN_LATITUDE,
            GeofenceWallpaper.COLUMN_LONGITUDE,
            GeofenceWallpaper.COLUMN_RADIUS,
            GeofenceWallpaper.COLUMN_COLOR,
            GeofenceWallpaper.COLUMN_DISTANCE,
            GeofenceWallpaper.COLUMN_ZOOM_LEVEL
    };
    
    public GeofenceDB(Context context) {
        super(context);
    }

    public static String getSnapshotUid(String geofenceUid) {
        return geofenceUid.replace(GEOFENCE_ID_PREFIX, GEOFENCE_SNAPSHOT_ID_PREFIX);
    }

    public static boolean hasDefaultWallpaper() {
        return FileUtils.fileExistance(ImageManager.getInstance()
                .getPictureUri(DEFAULT_WALLPAPER_UID));
    }

    @Override
    public String getNewUid() {
        return GEOFENCE_ID_PREFIX + super.getNewUid();
    }

    /**
     * Reads wallpaper data from the database
     * @param uid The wallpaper to be read
     * @return Wallpaper class
     */
    public GeofenceWallpaper getWallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        GeofenceWallpaper out;

        Cursor data = queryWallpaper(database, GeofenceWallpaper.TABLE_NAME,
                GeofenceWallpaper.COLUMN_UID, COLUMNS, Wallpaper.COLUMN_UID + " = '" + uid + "'");

        data.moveToFirst();

        if (!data.isAfterLast()) {
            out = new GeofenceWallpaper(data);
            data.close();
            return out;
        } else {
            data.close();
            return null;
        }
    }

    /**
     * Creates a new geowallpaper and stores it into the database
     *
     * @param uid       Unique id
     * @param location  Center location
     * @param radius    Radius
     * @param color     Color
     * @param zoomLevel Zoom level on the map
     * @return GeowallpaperData
     */
    public GeofenceWallpaper addWallpaper(String uid, LatLng location, float radius,
                                           int color, float zoomLevel) {
        if(addUid(uid, false)){
            SQLiteDatabase database = openDatabase();

            ContentValues v = new ContentValues();
            v.put(GeofenceWallpaper.COLUMN_UID, uid);
            v.put(GeofenceWallpaper.COLUMN_LATITUDE, location.latitude);
            v.put(GeofenceWallpaper.COLUMN_LONGITUDE, location.longitude);
            v.put(GeofenceWallpaper.COLUMN_RADIUS, radius);
            v.put(GeofenceWallpaper.COLUMN_COLOR, color);
            v.put(GeofenceWallpaper.COLUMN_ZOOM_LEVEL, zoomLevel);

            LatLng lastUserLocation = getLastLocation();
            float distance = GeoMath.getDistance(lastUserLocation.latitude, lastUserLocation.longitude,
                    location.latitude, location.longitude) - radius;
            v.put(GeofenceWallpaper.COLUMN_DISTANCE, distance < 0 ? 0 : distance);

            if(database.insert(GeofenceWallpaper.TABLE_NAME, null, v) != -1){
                return getWallpaper(uid);
            }else{
                Log.e(LOGTAG, "Error inserting wallpaper data to database");
                return null;
            }
        }else{
            Log.e(LOGTAG, "Error adding new uid to wallpaper table");
            return null;
        }
    }

    /**
     * Returns the location associated with the distances values of the geofencepapers
     *
     * @return LatLng, or null if not last location
     */
    public LatLng getLastLocation() {
        boolean hasLocation = Preferences.getBoolean(getContext(), R.string.preference_geofence_last_location_stored, false);
        if (hasLocation) {
            float lat = Preferences.getFloat(getContext(), R.string.preference_geofence_last_location_lat, 45);
            float lon = Preferences.getFloat(getContext(), R.string.preference_geofence_last_location_lon, 8);
            return new LatLng(lat, lon);
        }
        return null;
    }

    /**
     * Sets the last known user location and updates the geofencepapers distances.
     *
     * @param location The user location
     */
    public void setLastLocation(Location location) {
        if (location == null) {
            Preferences.setBoolean(getContext(), R.string.preference_geofence_last_location_stored, false);
            updateDistances(null);
            return;
        }
        Preferences.setBoolean(getContext(), R.string.preference_geofence_last_location_stored, true);
        Preferences.setFloat(getContext(), R.string.preference_geofence_last_location_lat, (float) location.getLatitude());
        Preferences.setFloat(getContext(), R.string.preference_geofence_last_location_lon, (float) location.getLongitude());

        updateDistances(location);
    }

    /**
     * Updates the distance of all geowallpapers from the current location
     */
    private void updateDistances(Location location) {
        SQLiteDatabase database = openDatabase();
        boolean n = location == null;

        List<GeofenceWallpaper> wallpapers = getWallpaperList(null, true);

        for (GeofenceWallpaper w : wallpapers) {
            float distance = n ? -1 : GeoMath.getDistance(location.getLatitude(), location.getLongitude(),
                    w.getLatitude(), w.getLongitude()) - w.getRadius();
            if (!n && distance < 0) distance = 0;
            ContentValues v = new ContentValues();
            v.put(GeofenceWallpaper.COLUMN_DISTANCE, distance);
            database.update(GeofenceWallpaper.TABLE_NAME, v, GeofenceWallpaper.COLUMN_UID + " = ?", new String[]{w.getUid()});
        }

    }
    
    /**
     * Returns the number of geowallpapers currently stored in the database
     *
     * @return Number of geowallpapers
     */
    public int getWallpaperCount() {
        SQLiteDatabase database = openDatabase();
        final SQLiteStatement stmt = database.compileStatement("SELECT COUNT(" +
                GeofenceWallpaper.COLUMN_UID + ") FROM "
                + Wallpaper.TABLE_WALLPAPERS + ", " + GeofenceWallpaper.TABLE_NAME
                + " WHERE " + Wallpaper.COLUMN_UID + " = " + GeofenceWallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 0");
        return  (int)stmt.simpleQueryForLong();
    }

    /**
     * Returns the list of geowallpapers ordered by distance
     *
     * @return
     */
    public ArrayList<GeofenceWallpaper> getWallpapersByDistance() {
        return new ArrayList<>(getWallpaperList(GeofenceWallpaper.COLUMN_DISTANCE + " ASC", false));
    }

    /**
     * Returns the list of wallpapers currently stored in the database, ordered by the column
     * specified by "orderBy"
     * @return list of wallpapers ordered by "orderBy"
     */
    private List<GeofenceWallpaper> getWallpaperList(String orderBy, boolean includingDeleted){
        SQLiteDatabase database = openDatabase();
        Cursor data = queryWallpaper(database, GeofenceWallpaper.TABLE_NAME,
                GeofenceWallpaper.COLUMN_UID, COLUMNS, includingDeleted ? null : Wallpaper.COLUMN_DELETED + " = 0",
                orderBy);

        data.moveToFirst();
        return cursorToList(data);
    }

    public void deleteWallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(Wallpaper.COLUMN_DELETED, true);
        database.update(Wallpaper.TABLE_WALLPAPERS, values, Wallpaper.COLUMN_UID + " = '" + uid + "'", null);
    }

    public List<GeofenceWallpaper> getDeletedWallpapers(){
        SQLiteDatabase database = openDatabase();
        Cursor c = queryWallpaper(database, GeofenceWallpaper.TABLE_NAME, GeofenceWallpaper.COLUMN_UID,
                COLUMNS, Wallpaper.COLUMN_DELETED + " = 1", null);
        c.moveToFirst();
        return cursorToList(c);
    }

    public int getDeletedWallpapersCount(){
        SQLiteDatabase database = openDatabase();
        String query = "SELECT COUNT(" +
                Wallpaper.COLUMN_UID + ") FROM " + GeofenceWallpaper.TABLE_NAME
                +  ", " + Wallpaper.TABLE_WALLPAPERS
                + " WHERE " + GeofenceWallpaper.COLUMN_UID + " = " + Wallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 1";
        final SQLiteStatement stmt = database.compileStatement(query);
        int count = (int)stmt.simpleQueryForLong();
        return count;
    }

    public void undoDeletion(){
        SQLiteDatabase database = openDatabase();
        database.execSQL("UPDATE " + Wallpaper.TABLE_WALLPAPERS
                + " SET " + Wallpaper.COLUMN_DELETED + " = 0"
                + " WHERE " + Wallpaper.COLUMN_DELETED + " = 1"
                + " AND " + Wallpaper.COLUMN_UID + " IN ("
                + "SELECT " + GeofenceWallpaper.COLUMN_UID
                + " FROM " + GeofenceWallpaper.TABLE_NAME + ")");

    }

    /**
     * Permanently deletes all the logically deleted geowallpapers and all the files
     * associated with them.
     */
    public void confirmDeletion() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.rawQuery("SELECT ? FROM " + Wallpaper.TABLE_WALLPAPERS + ", "
                        + GeofenceWallpaper.TABLE_NAME
                        + " WHERE " + Wallpaper.COLUMN_DELETED + " = 1"
                        + " AND " + Wallpaper.COLUMN_UID + " = " + GeofenceWallpaper.COLUMN_UID,
                new String[]{Wallpaper.COLUMN_UID});

        c.moveToFirst();
        while (!c.isAfterLast()){
            String uid = c.getString(0);
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(getSnapshotUid(uid)));
            c.moveToNext();
        }

        c.close();
        database.delete(Wallpaper.TABLE_WALLPAPERS, Wallpaper.COLUMN_DELETED + " = 1", null);
    }

    public GeofenceWallpaper getActiveWallpaper() {
        SQLiteDatabase database = openDatabase();
        Cursor c = queryWallpaper(database, GeofenceWallpaper.TABLE_NAME, GeofenceWallpaper.COLUMN_UID,
                COLUMNS, Wallpaper.COLUMN_DELETED + " = 0 AND "
                        + GeofenceWallpaper.COLUMN_ACTIVE + "  = 1",
                GeofenceWallpaper.COLUMN_RADIUS + "ASC", 1);
        c.moveToFirst();
        if(!c.isAfterLast()){
            GeofenceWallpaper out = new GeofenceWallpaper(c);
            c.close();
            return out;
        }else {
            c.close();
            return null;
        }
    }

    public List<GeofenceWallpaper> getActiveWallpapers(){
        SQLiteDatabase database = openDatabase();
        Cursor c = queryWallpaper(database, GeofenceWallpaper.TABLE_NAME, GeofenceWallpaper.COLUMN_UID,
                COLUMNS, Wallpaper.COLUMN_DELETED + " = 0 AND "
                        + GeofenceWallpaper.COLUMN_ACTIVE + "  = 1",
                GeofenceWallpaper.COLUMN_RADIUS + "ASC");
        c.moveToFirst();
        return cursorToList(c);
    }

    public void activateWallpaper(String uid) {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(GeofenceWallpaper.COLUMN_ACTIVE, 1);
        database.update(GeofenceWallpaper.TABLE_NAME, values,
                GeofenceWallpaper.COLUMN_UID + " = ?", new String[]{uid});

        Logger.fileW(getContext(), LOGTAG, "Activating %s", uid);
        listActiveGeofences();
    }

    public void deactivateWallpaper(String uid) {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(GeofenceWallpaper.COLUMN_ACTIVE, 0);
        database.update(GeofenceWallpaper.TABLE_NAME, values,
                GeofenceWallpaper.COLUMN_UID + " = ?", new String[]{uid});
        Logger.fileW(getContext(), LOGTAG, "Deactivating %s", uid);
        listActiveGeofences();
    }

    public void deactivateAllWallpapers(){
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(GeofenceWallpaper.COLUMN_ACTIVE, 0);
        database.update(GeofenceWallpaper.TABLE_NAME, values,
                GeofenceWallpaper.COLUMN_ACTIVE + " = 1", null);
        Logger.fileW(getContext(), LOGTAG, "Deactivating all");
        listActiveGeofences();
    }

    private List<GeofenceWallpaper> cursorToList(Cursor c){
        return cursorToList(c, true);
    }

    private List<GeofenceWallpaper> cursorToList(Cursor c, boolean close){
        List<GeofenceWallpaper> out = new ArrayList<>();
        while (!c.isAfterLast()){
            out.add(new GeofenceWallpaper(c));
            c.moveToNext();
        }
        if(close){
            c.close();
        }
        return out;
    }

    private void listActiveGeofences() {
        List<GeofenceWallpaper> data = getActiveWallpapers();
        Logger.fileW(getContext(), LOGTAG, "Active geofences:");
        int i = 1;
        for (GeofenceWallpaper d : data) {
            Logger.fileW(getContext(), LOGTAG, "\t\t%d: %s", i++, d.getUid());
        }
    }
}
