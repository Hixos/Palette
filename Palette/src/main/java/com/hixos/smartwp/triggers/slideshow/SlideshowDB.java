package com.hixos.smartwp.triggers.slideshow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.Wallpaper;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Hixos on 24/07/2015.
 */
public class SlideshowDB extends DatabaseManager {
    public static final String SLIDESHOW_ID_PREFIX = "ss-";
    private final static String LOGTAG = "SlideshowDB";
    private final static String COLUMNS[] = {
            Wallpaper.COLUMN_UID,
            Wallpaper.COLUMN_DELETED,
            SlideshowWallpaper.COLUMN_ORDER,
            SlideshowWallpaper.COLUMN_SHUFFLE_ORDER};

    public SlideshowDB(Context context) {
        super(context);
    }

    @Override
    public String getNewUid() {
        return SLIDESHOW_ID_PREFIX + super.getNewUid();
    }

    /**
     * Return the slideshow interval
     *
     * @return interval in milliseconds
     */
    public long getIntervalMs() {
        return Preferences.getLong(getContext(), R.string.preference_slideshow_interval,
                10 * 1000 * 60);
    }

    /**
     * Sets the slideshow interval
     *
     * @param intervalMS interval in milliseconds
     */
    public void setIntervalMs(long intervalMS) {
        Preferences.setLong(getContext(), R.string.preference_slideshow_interval, intervalMS);
    }


    public void setShuffle(boolean enabled){
        Preferences.setBoolean(getContext(), R.string.preference_slideshow_shuffle, enabled);
        if(enabled){
            shuffle();
        }
    }

    public boolean isShuffleEnabled() {
        return Preferences.getBoolean(getContext(), R.string.preference_slideshow_shuffle, false);
    }

    /**
     * Adds a new wallpaper to the database
     * @param uid the uid of the new wallpaper
     * @return Wallpaper class if successfull
     */
    public SlideshowWallpaper addWallpaper(String uid){
        if(addUid(uid, false)){
            int order = getMaxOrderValue() + 1;
            ContentValues values = new ContentValues();
            values.put(SlideshowWallpaper.COLUMN_UID, uid);
            values.put(SlideshowWallpaper.COLUMN_ORDER, order);

            int shuffleOrder;
            if(isShuffleEnabled()){
                shuffleOrder = new Random().nextInt(getWallpaperCount());
                shiftRight(SlideshowWallpaper.COLUMN_SHUFFLE_ORDER, shuffleOrder, 1);
            }else {
                shuffleOrder = -1;
            }

            values.put(SlideshowWallpaper.COLUMN_SHUFFLE_ORDER, shuffleOrder);
            SQLiteDatabase database = openDatabase();
            if(database.insert(SlideshowWallpaper.TABLE_NAME, null, values) != -1){
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
     * Reads wallpaper data from the database
     * @param uid The wallpaper to be read
     * @return Wallpaper class
     */
    public SlideshowWallpaper getWallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        SlideshowWallpaper out;

        Cursor data = queryWallpaper(database, SlideshowWallpaper.TABLE_NAME,
                SlideshowWallpaper.COLUMN_UID, COLUMNS, Wallpaper.COLUMN_UID + " = '" + uid + "'");

        data.moveToFirst();

        if (!data.isAfterLast()) {
            out = new SlideshowWallpaper(data);
            data.close();
            
            return out;
        } else {
            data.close();
            
            return null;
        }
    }

    /**
     * Helper method
     * @return
     */
    public List<SlideshowWallpaper> getWallpapersByOrder(){
        return getWallpaperList(SlideshowWallpaper.COLUMN_ORDER);
    }
    /**
     * Helper method
     * @return
     */
    public List<SlideshowWallpaper> getWallpapersByShuffleOrder(){
        return getWallpaperList(SlideshowWallpaper.COLUMN_SHUFFLE_ORDER);
    }

    /**
     * Helper method
     * @return
     */
    public List<SlideshowWallpaper> getOrderedWallpaperList(){
        if(isShuffleEnabled()){
            return getWallpapersByShuffleOrder();
        }else {
            return getWallpapersByOrder();
        }
    }

    /**
     * Returns the list of wallpapers currently stored in the database, ordered by the column
     * specified by "orderBy"
     * @return list of wallpapers ordered by "orderBy"
     */
    private List<SlideshowWallpaper> getWallpaperList(String orderBy){
        SQLiteDatabase database = openDatabase();
        Cursor data = queryWallpaper(database, SlideshowWallpaper.TABLE_NAME,
                SlideshowWallpaper.COLUMN_UID, COLUMNS, Wallpaper.COLUMN_DELETED + " = 0",
                orderBy + " ASC");
        data.moveToFirst();

        List<SlideshowWallpaper> out = new ArrayList<>();

        while (!data.isAfterLast()){
            SlideshowWallpaper wp = new SlideshowWallpaper(data);
            out.add(wp);
            data.moveToNext();
        }

        data.close();
        
        return out;
    }

    public void deleteWallpaper(String uid){
        if(uid.equals(getCurrentWallpaperUid())){
            nextWallpaper();
        }
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(Wallpaper.COLUMN_DELETED, true);
        database.update(Wallpaper.TABLE_WALLPAPERS, values, Wallpaper.COLUMN_UID + " = '" + uid + "'", null);
        
    }

    public void undoDeletion(){
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(Wallpaper.COLUMN_DELETED, false);
        database.update(Wallpaper.TABLE_WALLPAPERS, values, Wallpaper.COLUMN_DELETED + " = 1", null);
        
    }

    public void confirmDeletion(){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.rawQuery("SELECT ?, ? FROM " + Wallpaper.TABLE_WALLPAPERS + ", "
                + SlideshowWallpaper.TABLE_NAME + " WHERE " + Wallpaper.COLUMN_DELETED + " = 1"
                + " AND " + Wallpaper.COLUMN_UID + " = " + SlideshowWallpaper.COLUMN_UID
                + " ORDER BY " + SlideshowWallpaper.COLUMN_ORDER + " ASC",
                new String[]{Wallpaper.COLUMN_UID, SlideshowWallpaper.COLUMN_ORDER});

        c.moveToFirst();
        while (!c.isAfterLast()){
            String uid = c.getString(0);
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
            int order = c.getInt(1);
            database.execSQL("UPDATE " + SlideshowWallpaper.TABLE_NAME + " SET "
                    + SlideshowWallpaper.COLUMN_ORDER + " = " + SlideshowWallpaper.COLUMN_ORDER
                    + " - 1 WHERE " + SlideshowWallpaper.COLUMN_ORDER + " > " + order);
            c.moveToNext();
        }

        c.close();
        database.delete(Wallpaper.TABLE_WALLPAPERS, Wallpaper.COLUMN_DELETED + " = 1", null);
        
    }


    public void shuffle(){
        SQLiteDatabase database = openDatabase();
        List<String> ids = new ArrayList<>();

        Cursor c = database.query(Wallpaper.TABLE_WALLPAPERS, new String[]{Wallpaper.COLUMN_UID},
                Wallpaper.COLUMN_DELETED + " = 0", null, null, null, null);

        c.moveToFirst();
        while (!c.isAfterLast()){
            ids.add(c.getString(0));
            c.moveToNext();
        }
        c.close();

        int inc = 0;
        while (ids.size() != 0){
            int index = new Random().nextInt(ids.size());
            ContentValues values = new ContentValues();
            values.put(SlideshowWallpaper.COLUMN_SHUFFLE_ORDER, inc);
            database.update(SlideshowWallpaper.TABLE_NAME, values, SlideshowWallpaper.COLUMN_UID
                            + " = '" + ids.get(index) + "'", null);
            inc++;
            ids.remove(index);
        }
        getWallpapersByShuffleOrder();
    }

    public void moveBefore(String move, String before){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(SlideshowWallpaper.TABLE_NAME,
                new String[]{SlideshowWallpaper.COLUMN_UID, SlideshowWallpaper.COLUMN_ORDER},
                SlideshowWallpaper.COLUMN_UID + " = '" + before + "' OR "
                + SlideshowWallpaper.COLUMN_UID + " = '" + move + "'", null, null, null, null);
        c.moveToFirst();
        int moveOrder = -1, beforeOrder = -1;

        while(!c.isAfterLast()){
            String uid = c.getString(0);
            if(uid.equals(move)){
                moveOrder = c.getInt(1);
            }else{
                beforeOrder = c.getInt(1);
            }
            c.moveToNext();
        }
        c.close();

        if(moveOrder == -1 || beforeOrder == -1){
            Log.e(LOGTAG, "Cannot move wallpaper. Not found.");
            return;
        }

        if(moveOrder > beforeOrder){
            shiftRight(SlideshowWallpaper.COLUMN_ORDER, beforeOrder, 1);
            ContentValues values = new ContentValues();
            values.put(SlideshowWallpaper.COLUMN_ORDER, beforeOrder);
            database.update(SlideshowWallpaper.TABLE_NAME, values,
                    SlideshowWallpaper.COLUMN_UID + " = '" + move + "'", null);
        }else{
            shiftLeft(SlideshowWallpaper.COLUMN_ORDER, moveOrder, beforeOrder, 1);
            ContentValues values = new ContentValues();
            values.put(SlideshowWallpaper.COLUMN_ORDER, beforeOrder);
            database.update(SlideshowWallpaper.TABLE_NAME, values,
                    SlideshowWallpaper.COLUMN_UID + " = '" + move + "'", null);
        }
    }

    @Deprecated
    public void fixOrder(){
        SQLiteDatabase database = openDatabase();
        List<String> ids = new ArrayList<>();

        Cursor c = database.rawQuery("SELECT ? FROM ?, ? WHERE ? = ? ORDER BY ? ASC", //ERROR
                new String[]{Wallpaper.COLUMN_UID,
                        Wallpaper.TABLE_WALLPAPERS, SlideshowWallpaper.TABLE_NAME,
                        Wallpaper.COLUMN_UID, SlideshowWallpaper.COLUMN_UID,
                        SlideshowWallpaper.COLUMN_ORDER});

        c.moveToFirst();
        while (!c.isAfterLast()){
            ids.add(c.getString(0));
            c.moveToNext();
        }
        c.close();

        for(int i = 0; i < ids.size(); i++){
            ContentValues values = new ContentValues();
            values.put(SlideshowWallpaper.COLUMN_ORDER, i);
            database.update(SlideshowWallpaper.TABLE_NAME, values, "? = ?",
                    new String[]{SlideshowWallpaper.COLUMN_UID, ids.get(i)});
        }
    }


    /**
     * Gets the number of active wallpapers
     * @return count of wallpapers
     */
    public int getWallpaperCount(){
        SQLiteDatabase database = openDatabase();
        final SQLiteStatement stmt = database.compileStatement("SELECT COUNT(" +
                SlideshowWallpaper.COLUMN_UID + ") FROM "
                + Wallpaper.TABLE_WALLPAPERS + ", " + SlideshowWallpaper.TABLE_NAME
                + " WHERE " + Wallpaper.COLUMN_UID + " = " + SlideshowWallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 0");
        return  (int)stmt.simpleQueryForLong();
    }

    public String getCurrentWallpaperUid(){
        String uid = Preferences.getString(getContext(),
                R.string.preference_slideshow_current_wallpaper, null);
        Logger.d("getCurrentWUid - Uid: %s", uid);
        if(uid != null){
            return exists(uid) ? uid : null;
        }else {
            return null;
        }
    }

    public SlideshowWallpaper getCurrentWallpaper(){
        String uid = getCurrentWallpaperUid();
        if(uid != null){
            return getWallpaper(uid);
        }else {
            return null;
        }
    }

    public void setCurrentWallpaper(String uid){
        Preferences.setString(getContext(), R.string.preference_slideshow_current_wallpaper, uid);
    }

    public String nextWallpaper(){
        String orderCol = isShuffleEnabled()
                ? SlideshowWallpaper.COLUMN_SHUFFLE_ORDER : SlideshowWallpaper.COLUMN_ORDER;
        SQLiteDatabase database = openDatabase();
        String currentUid = getCurrentWallpaperUid();
        if(currentUid == null) currentUid = "lolwut";
        Cursor c = database.rawQuery("SELECT " + SlideshowWallpaper.COLUMN_UID + " FROM "
                + SlideshowWallpaper.TABLE_NAME + " WHERE " + orderCol + " > "
                + "(SELECT " + orderCol + " FROM " + SlideshowWallpaper.TABLE_NAME + " WHERE "
                + SlideshowWallpaper.COLUMN_UID + " = '" + currentUid + "')"
                + " ORDER BY " + orderCol + " ASC LIMIT 1", null);
        c.moveToFirst();

        String uid;
        if(!c.isAfterLast()){
            uid = c.getString(0);
        }else{
            c.close();
            if(isShuffleEnabled()) shuffle();
            c = database.rawQuery("SELECT " + SlideshowWallpaper.COLUMN_UID + " FROM "
                    + SlideshowWallpaper.TABLE_NAME + " ORDER BY " + orderCol
                    + " ASC LIMIT 1", null);
            c.moveToFirst();
            if(!c.isAfterLast()) {
                uid = c.getString(0);
            }else{
                uid = null;
            }
        }
        
        c.close();

        setCurrentWallpaper(uid);
        return uid;
    }

    /**
     * Gets the number of deleted wallpapers
     * @return count of wallpapers
     */
    public int getDeletedWallpapersCount(){
        SQLiteDatabase database = openDatabase();
        String query = "SELECT COUNT(" +
                Wallpaper.COLUMN_UID + ") FROM " + SlideshowWallpaper.TABLE_NAME
                +  ", " + Wallpaper.TABLE_WALLPAPERS
                + " WHERE " + SlideshowWallpaper.COLUMN_UID + " = " + Wallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 1";
        final SQLiteStatement stmt = database.compileStatement(query);
        int count = (int)stmt.simpleQueryForLong();
        return count;
    }

    /**
     * Subtracts 'count' to the values in 'column' in the interval (from, to]
     * @param column The column to shift
     * @param from the minimum value to be increased
     * @param count shift how much?
     */
    private void shiftLeft(String column, int from, int to, int count){
        if(count <= 0){
            throw new IllegalArgumentException("'count' must be greater than 0");
        }
        SQLiteDatabase database = openDatabase();
        String update = "UPDATE " + SlideshowWallpaper.TABLE_NAME + " SET " + column + " = "
                + column + " - " + count + " WHERE " + column + " > " + from + " AND " + column
                + " <= " + to;
        database.execSQL(update);
    }


    /**
     * Sums 'count' to the values in 'column' higher or equal than 'from'
     * @param column The column to shift
     * @param from the minimum value to be increased
     * @param count shift how much?
     */
    private void shiftRight(String column, int from, int count){
        if(count <= 0){
            throw new IllegalArgumentException("'count' must be greater than 0");
        }
        SQLiteDatabase database = openDatabase();
        String update = "UPDATE " + SlideshowWallpaper.TABLE_NAME + " SET " + column + " = "
                + column + " + " + count + " WHERE " + column + " >= " + from;
        database.execSQL(update);
    }

    /**
     * Returns the maximum value of the column 'order'
     * @return max order
     */
    private int getMaxOrderValue(){
        SQLiteDatabase database = openDatabase();
        final SQLiteStatement stmt = database.compileStatement("SELECT MAX(" +
                SlideshowWallpaper.COLUMN_ORDER + ") FROM " + SlideshowWallpaper.TABLE_NAME);
        int out = (int)stmt.simpleQueryForLong();
        
        return out;
    }

    private boolean exists(String uid){
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(Wallpaper.TABLE_WALLPAPERS, new String[]{Wallpaper.COLUMN_DELETED},
                Wallpaper.COLUMN_UID + " = '" + uid + "'", null, null, null, null);
        c.moveToFirst();
        if(!c.isAfterLast()){
            boolean out = c.getInt(0) == 0;
            c.close();
            
            return out;
        }else {
            c.close();
            
            return false;
        }
    }
}
