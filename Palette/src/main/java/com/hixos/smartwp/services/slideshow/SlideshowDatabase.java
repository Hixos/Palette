package com.hixos.smartwp.services.slideshow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;

import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Luca on 30/01/14
 * Slideshow database, service and file manager
 */
public class SlideshowDatabase extends DatabaseManager {
    public final static String TABLE_DATA = "[slideshow_data]";
    public final static String COLUMN_DATA_ID = "[_id]";
    public final static String COLUMN_DATA_ORDER = "[order]";
    public final static String COLUMN_DATA_SHUFFLE_ORDER = "[shuffle_order]";
    public final static String COLUMN_DATA_DELETED = "[deleted]";
    public static final String SLIDESHOW_ID_PREFIX = "ss-";
    private static final String TAG = "slideshow";
    private static final String LOGTAG = "SsDatabase";
    private DatabaseObserver mObserver;
    private OnElementRemovedListener mOnElementRemovedListener;

    public SlideshowDatabase(Context context) {
        super(context);
    }

    @Override
    public String getTag() {
        return TAG;
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

    public boolean isShuffleEnabled() {
        return Preferences.getBoolean(getContext(), R.string.preference_slideshow_shuffle, false);
    }

    public void setShuffleEnabled(final boolean enabled) {
        Preferences.setBoolean(getContext(), R.string.preference_slideshow_shuffle, enabled);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (enabled) {
                    shuffleWallpapers();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (mObserver != null)
                    mObserver.onDataSetChanged();
            }
        };
        task.execute();
    }

    /**
     * Create a new slideshow wallpaper
     *
     * @param uid the uid for the wallpaper
     * @return slideshow wallpaper data
     */
    public SlideshowData createWallpaper(String uid) {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        int maxorder = getMaxOrder() + 1;

        values.put(COLUMN_DATA_ID, uid);
        values.put(COLUMN_DATA_ORDER, maxorder);
        values.put(COLUMN_DATA_DELETED, false);
        if (isShuffleEnabled()) {
            Random random = new Random();
            int count = getWallpaperCount();
            int shufflePos = count == 0 ? 0 : random.nextInt(count);
            values.put(COLUMN_DATA_SHUFFLE_ORDER, shufflePos);
            String updateQuery =
                    " UPDATE " + TABLE_DATA +
                            " SET " + COLUMN_DATA_SHUFFLE_ORDER + " = " +
                            COLUMN_DATA_SHUFFLE_ORDER + " + 1 WHERE " + COLUMN_DATA_SHUFFLE_ORDER +
                            " >= " + shufflePos;
            database.rawQuery(updateQuery, null);
        }

        database.insert(TABLE_DATA, null, values);

        Cursor cursor = database.query(TABLE_DATA,
                SlideshowData.DATA_COLUMNS, COLUMN_DATA_ID + " = ?", new String[]{uid},
                null, null, null);
        cursor.moveToFirst();

        SlideshowData d = SlideshowData.fromCursor(cursor);

        cursor.close();

        closeDatabase();

        if (mObserver != null)
            mObserver.onElementCreated(d);
        return d;
    }

    /**
     * Returns the slideshow wallpaper of the given uid
     *
     * @param uid the uid of the wallpaper
     * @return Slideshow data
     */
    public SlideshowData getWallpaper(String uid) {
        SQLiteDatabase database = openDatabase();
        SlideshowData out;
        Cursor data = database.query(TABLE_DATA, SlideshowData.DATA_COLUMNS,
                COLUMN_DATA_ID + " = ?", new String[]{uid}, null, null, null);
        data.moveToFirst();

        if (!data.isAfterLast()) {
            out = SlideshowData.fromCursor(data);
        } else {
            data.close();
            return null;
        }

        data.close();
        closeDatabase();
        return out;
    }

    /**
     * Logically deletes the wallpaper of the given id. The wallpaper is still
     * recoverable using restoreWallpaper(uid)
     *
     * @param uid
     */
    public void deleteWallpaper(String uid) {
        deleteWallpaper(uid, true);
    }

    public void deleteWallpaper(String uid, boolean notifyObserver) {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA_DELETED, true);
        database.update(TABLE_DATA, values, COLUMN_DATA_ID + " = ?", new String[]{uid});
        closeDatabase();

        if (notifyObserver && mObserver != null) {
            mObserver.onElementRemoved(uid);
        }

        if (mOnElementRemovedListener != null)
            mOnElementRemovedListener.onElementRemoved(uid);
    }

    public int getDeletedWallpaperCount() {
        SQLiteDatabase database = openDatabase();
        String query = "SELECT count(" + COLUMN_DATA_ID + ") FROM " + TABLE_DATA + " WHERE "
                + COLUMN_DATA_DELETED + " = 1";
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();
        int count = 0;
        if (!cursor.isAfterLast()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        closeDatabase();
        return count;
    }

    public void adapterDeleteWallpaper(String uid) {
        AsyncTask<String, Void, Void> deleteTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                deleteWallpaper(strings[0], false);
                return null;
            }
        };
        deleteTask.execute(uid);
    }

    public void restoreWallpapers() {
        restoreWallpapers(true);
    }

    private void restoreWallpapers(boolean notifyObserver) {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA_DELETED, false);
        database.update(TABLE_DATA, values, COLUMN_DATA_DELETED + " = 1", null);
        closeDatabase();
        if (notifyObserver && mObserver != null) {
            mObserver.onDataSetChanged();
        }
    }

    public void restoreWallpapersAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                restoreWallpapers(false);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (mObserver != null) {
                    mObserver.onDataSetChanged();
                }
            }
        };
        task.execute();
    }

    /**
     * Permanently deletes all *logically deleted* wallpapers and removes all the files
     * associated with them
     */
    public void clearDeletedWallpapers() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{COLUMN_DATA_ID, COLUMN_DATA_DELETED},
                COLUMN_DATA_DELETED + " = " + 1, null, null, null, null);
        c.moveToFirst();
        List<String> deleted = new ArrayList<>();
        while (!c.isAfterLast()) {
            deleted.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        database.delete(TABLE_DATA, COLUMN_DATA_DELETED + " = 1", null);
        closeDatabase();

        for (String uid : deleted) {
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));

        }
    }

    public void clearDeletedWallpapersAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                clearDeletedWallpapers();
                return null;
            }
        };
        task.execute();
    }

    /**
     * Returns the list of wallpapers ordered by distance in time
     * starting from the current wallpaper
     *
     * @return List of SlideshowData
     */
    public List<SlideshowData> getOrderedWallpapers() {
        return getOrderedWallpapers(false);
    }

    /**
     * Returns the list of wallpapers ordered by distance in time
     *
     * @param fromCurrent if the wallpapers should be ordered starting from the current
     * @return List of SlideshowData
     */
    private List<SlideshowData> getOrderedWallpapers(boolean fromCurrent) {
        String orderColumn = isShuffleEnabled() ? COLUMN_DATA_SHUFFLE_ORDER : COLUMN_DATA_ORDER;
        SQLiteDatabase database = openDatabase();
        List<SlideshowData> out = new ArrayList<>();
        Cursor data = database.query(TABLE_DATA, SlideshowData.DATA_COLUMNS,
                COLUMN_DATA_DELETED + " = " + 0, null, null, null, orderColumn + " ASC");
        data.moveToFirst();

        while (!data.isAfterLast()) {
            out.add(SlideshowData.fromCursor(data));
            data.moveToNext();
        }

        data.close();
        closeDatabase();

        if (out.size() == 0) return out;

        if (fromCurrent) {
            int current = getCurrentWallpaperIndex();
            if (current >= out.size()) current = 0;

            List<SlideshowData> ordered = new ArrayList<>();

            int i = current;
            do {
                ordered.add(out.get(i));
                i++;
                if (i >= out.size()) {
                    i = 0;
                }
            } while (i != current);

            return ordered;
        }

        return out;
    }

    public void getOrderedWallpapersAsync(final OnWallpapersLoadedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener must be defined!");

        AsyncTask<Void, Void, List<SlideshowData>> task = new AsyncTask<Void, Void, List<SlideshowData>>() {
            @Override
            protected List<SlideshowData> doInBackground(Void... voids) {
                return getOrderedWallpapers();
            }

            @Override
            protected void onPostExecute(List<SlideshowData> slideshowDatas) {
                super.onPostExecute(slideshowDatas);
                listener.onWallpapersLoaded(slideshowDatas);
            }
        };
        task.execute();
    }

    public int getPosition(String uid) {
        return getPosition(uid, isShuffleEnabled());
    }

    private int getPosition(String uid, boolean shuffle) {
        String column = shuffle ? COLUMN_DATA_SHUFFLE_ORDER : COLUMN_DATA_ORDER;
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{column},
                COLUMN_DATA_ID + " = ?", new String[]{uid}, null, null, null);
        c.moveToFirst();
        int pos;

        if (!c.isAfterLast()) {
            pos = c.getInt(0);
        } else {
            pos = -1;
        }

        c.close();
        closeDatabase();
        return pos;
    }

    public void updateOrderAsync(final List<SlideshowData> items, final boolean notifyObserver) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                updateOrder(items, isShuffleEnabled(), notifyObserver);
                return null;
            }
        };
        task.execute();
    }

    private void updateOrder(List<SlideshowData> items, boolean shuffle, boolean notifyObserver) {
        String column = shuffle ? COLUMN_DATA_SHUFFLE_ORDER : COLUMN_DATA_ORDER;
        SQLiteDatabase database = openDatabase();

        int order = 1;
        for (SlideshowData item : items) {
            ContentValues values = new ContentValues();
            values.put(column, order);
            database.update(TABLE_DATA, values, COLUMN_DATA_ID + " = ?", new String[]{item.getUid()});
            order++;
        }

        closeDatabase();

        if (notifyObserver && mObserver != null) {
            mObserver.onDataSetChanged();
        }
    }

    @Deprecated
    public void adapterSetPosition(final String ID, final String atID) {

    }

    @Deprecated
    private void setPosition(String ID, String atID, boolean shuffle, boolean notifyObserver) {
        String column = shuffle ? COLUMN_DATA_SHUFFLE_ORDER : COLUMN_DATA_ORDER;
        int newPosition = getPosition(atID);
        int oldPosition = getPosition(ID);

        if (newPosition != -1 && oldPosition != -1) {
            String moveBy1SQL, swapSQL;

            swapSQL = "UPDATE " + TABLE_DATA + " SET " + column +
                    " = " + newPosition + " WHERE " + COLUMN_DATA_ID + " = '" + ID + "'";

            if (oldPosition < newPosition) {
                moveBy1SQL = "UPDATE " + TABLE_DATA + " SET " + column +
                        " = " + column + " - 1 WHERE " + column + " > " + oldPosition +
                        " AND " + column + " <= " + newPosition;
            } else if (oldPosition > newPosition) {
                moveBy1SQL = "UPDATE " + TABLE_DATA + " SET " + column +
                        " = " + column + " + 1 WHERE " + column + " >= " + newPosition +
                        " AND " + column + " < " + oldPosition;
            } else {
                return;
            }

            SQLiteDatabase database = openDatabase();
            database.execSQL(moveBy1SQL);

            database.execSQL(swapSQL);

            closeDatabase();

            if (notifyObserver && mObserver != null) {
                mObserver.onDataSetChanged();
            }
        }
    }

    public void logOrder() {
        String column = isShuffleEnabled() ? COLUMN_DATA_SHUFFLE_ORDER : COLUMN_DATA_ORDER;
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{COLUMN_DATA_ID, column}, null, null, null, null, column + " ASC");
        c.moveToFirst();
        while (!c.isAfterLast()) {
            c.moveToNext();
        }
        c.close();
        closeDatabase();
    }

    /**
     * Shuffles the wallpapers
     */
    public void shuffleWallpapers() {
        SQLiteDatabase database = openDatabase();
        List<String> uids = new ArrayList<>();
        Cursor cursor = database.query(TABLE_DATA, new String[]{COLUMN_DATA_ID},
                null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            uids.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();

        ContentValues v = new ContentValues();
        Random r = new Random();
        int order = 0;
        while (uids.size() > 0) {
            int index = r.nextInt(uids.size());
            v.put(COLUMN_DATA_SHUFFLE_ORDER, order);
            database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?", new String[]{uids.get(index)});
            uids.remove(index);
            order++;
        }
        closeDatabase();
    }

    /**
     * Return the number of slideshow wallpaper stored in the database
     *
     * @return int
     */
    public int getWallpaperCount() {
        SQLiteDatabase database = openDatabase();
        Cursor c = null;

        int count = 0;
        try {
            c = database.rawQuery("SELECT count(*) FROM " + TABLE_DATA + " WHERE " + COLUMN_DATA_DELETED + " = 0", null);
            c.moveToFirst();
            if (!c.isAfterLast()) {
                count = c.getInt(0);
            }
        } finally {
            if (c != null) c.close();
            closeDatabase();
        }

        return count;
    }

    /**
     * Returns the current wallpaper index
     *
     * @return index
     */
    public int getCurrentWallpaperIndex() {
        int current = Preferences.getInt(getContext(),
                R.string.preference_slideshow_current_wallpaper, 0);

        int size = getWallpaperCount();
        if (size == 0) {
            if (current != -1) {
                current = -1;
                setCurrentWallpaper(current);
            }
        } else if (current < 0 || current >= size) {
            current = 0;
            setCurrentWallpaper(0);
        }
        return current;
    }

    /**
     * Returns the current wallpaper
     *
     * @return data
     */
    public SlideshowData getCurrentWallpaper() {
        List<SlideshowData> data = getOrderedWallpapers(true);
        int first = getCurrentWallpaperIndex();
        int i = 0;
        int index = first + i;
        for (SlideshowData d : data) {
            if (FileUtils.fileExistance(ImageManager.getInstance().getPictureUri(d.getUid()))) {
                if (index >= data.size()) {
                    index -= data.size();
                }
                setCurrentWallpaper(index);
                return d;
            }
            i++;
        }
        return null;
    }

    /**
     * Sets the current wallpaper index
     *
     * @param value
     */
    public void setCurrentWallpaper(int value) {
        Preferences.setInt(getContext(),
                R.string.preference_slideshow_current_wallpaper, value);
    }

    /**
     * Returns the next wallpaper, this becomes the current one.
     *
     * @return next wallpaper data
     */
    public SlideshowData nextWallpaper() {
        int current = getCurrentWallpaperIndex() + 1;
        if (current == 0) return null;
        int size = getWallpaperCount();
        if (size == 0) {
            setCurrentWallpaper(-1);
            return null;
        } else if (current >= size) {
            if (isShuffleEnabled()) {
                shuffleWallpapers();
            }
            current = 0;
        }

        List<SlideshowData> data = getOrderedWallpapers(true);
        int first = current;
        int i = 0;
        for (SlideshowData d : data) {
            int index = first + i;
            if (FileUtils.fileExistance(ImageManager.getInstance().getPictureUri(d.getUid()))) {
                if (index >= data.size()) {
                    index -= data.size();
                }
                setCurrentWallpaper(index);
                return d;
            }
            i++;
        }
        setCurrentWallpaper(-1);
        return null;
    }

    /**
     * Returns the maximum order value
     *
     * @return
     */
    private int getMaxOrder() {
        SQLiteDatabase database = openDatabase();
        final SQLiteStatement stmt = database.compileStatement("SELECT MAX(" +
                COLUMN_DATA_ORDER + ") FROM " + TABLE_DATA);
        int out = (int) stmt.simpleQueryForLong();
        closeDatabase();
        return out;
    }

    /**
     * Sets the wallpaper
     *
     * @param uid uid of the wallpaper to set
     */
    public void setWallpaper(String uid) {
        if (!uid.equals(Preferences.getString(getContext(), R.string.preference_slideshow_last_set, ""))) {
            // ServiceManager.setWallpaper(getFileManager().getWallpaperUri(uid), getContext());
            Preferences.setString(getContext(), R.string.preference_slideshow_last_set, uid);
            Preferences.setLong(getContext(), R.string.preference_slideshow_last_set_time,
                    System.currentTimeMillis());
        }
    }

    public void setDatabaseObserver(DatabaseObserver observer) {
        mObserver = observer;
    }

    public void setOnElementRemovedListener(OnElementRemovedListener listener) {
        mOnElementRemovedListener = listener;
    }

    public interface DatabaseObserver {
        public void onElementRemoved(String uid);

        public void onElementCreated(SlideshowData element);

        public void onDataSetChanged();
    }


    public interface OnElementRemovedListener {
        public void onElementRemoved(String uid);
    }

    public interface OnWallpapersLoadedListener {
        public void onWallpapersLoaded(List<SlideshowData> wallpapers);
    }
}
