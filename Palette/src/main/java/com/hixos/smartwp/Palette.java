package com.hixos.smartwp;

import android.app.Application;

/**
 * Created by Luca on 10/10/2014.
 */
public class Palette extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        long maxMemory = Runtime.getRuntime().maxMemory();
        new com.hixos.smartwp.bitmaps.ImageManager.Builder(this)
                .hasCache(true).cacheSize(Math.round(maxMemory / 4f)).build();
    }
}
