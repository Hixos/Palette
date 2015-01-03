package com.hixos.smartwp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DatabaseManager {
    private static AtomicInteger sOpenCounter;
    private static DatabaseHelper sDbHelper;
    private static SQLiteDatabase sDatabase;

    private Context mContext;

    static {
        sOpenCounter = new AtomicInteger();
    }

    public DatabaseManager(Context context) {
        mContext = context.getApplicationContext();
        initializeDatabase(mContext);
    }

    public static synchronized void initializeDatabase(Context context) {
        if (sDbHelper == null) {
            sDbHelper = new DatabaseHelper(context.getApplicationContext());
        }
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Return the app's database
     *
     * @return App's database
     */
    public synchronized SQLiteDatabase openDatabase() {
        int c = sOpenCounter.incrementAndGet();
        if (c < 1) {
            Logger.e("Database", "Invalid counter value");
            sOpenCounter.set(1);
        }
        if (c <= 1) {
            sDatabase = sDbHelper.getWritableDatabase();
        }
        return sDatabase;
    }

    /**
     * Closes the database and releases all the resources
     */
    public synchronized void closeDatabase() {
        if (sOpenCounter.decrementAndGet() == 0) {
            sDatabase.close();
        }
    }

    /**
     * String tag for the database
     *
     * @return tag
     */
    public abstract String getTag();

    /**
     * Default method to get a unique id
     *
     * @return uid
     */
    public String getNewUid() {
        return UUID.randomUUID().toString();
    }
}
