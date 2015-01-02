package com.hixos.smartwp.wallpaper;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.services.ServiceUtils;
import com.hixos.smartwp.services.geofence.GeofenceService;
import com.hixos.smartwp.services.slideshow.SlideshowService;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;

import glwallpaperservice.GLWallpaperService;


/**
 * Created by Luca on 23/02/14.
 */
public class LiveWallpaperService extends GLWallpaperService{
    public static final String ACTION_SERVICE_ACTIVATED = "com.hixos.smartwp.ACTION_SERVICE_ACTIVATED";

    private final static String LOGTAG = "LiveWallpaperService";

    public interface Callbacks {
        public void requestRender();
    }

    private WallpaperEngine mEngine;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public WallpaperService.Engine onCreateEngine() {
        mEngine = new WallpaperEngine();
        return mEngine;
    }

    private class WallpaperEngine extends GLEngine implements OnWallpaperChangedCallback {

        private final static String LOGTAG = "WallpaperEngine";
        private GLRenderer mRenderer;

        private SetWallpaperTask mTask;

        private Uri mCurrentWallaper;
        private Uri mQueuedWallpaper;
        private boolean mVisible = false;

        private ServiceActivatedReceiver mReceiver;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);
            mRenderer = new GLRenderer(new Callbacks() {
                @Override
                public void requestRender() {
                    WallpaperEngine.this.requestRender();
                }
            });
            setEGLConfigChooser(8, 8, 8, 0, 0, 0);
            setRenderer(mRenderer);
            setRenderMode(RENDERMODE_WHEN_DIRTY);

            mReceiver = new ServiceActivatedReceiver();
            LiveWallpaperService.this.registerReceiver(mReceiver,
                    new IntentFilter(ACTION_SERVICE_ACTIVATED));
            initService();
        }

        public void initService(){
            int activeService = ServiceUtils.getActiveService(LiveWallpaperService.this);

            if(!isPreview()) {
                switch (activeService){
                    case ServiceUtils.SERVICE_SLIDESHOW:
                        SlideshowService.startListener(this, LiveWallpaperService.this);
                        GeofenceService.stopListener(LiveWallpaperService.this);
                        break;
                    case ServiceUtils.SERVICE_GEOFENCE:
                        GeofenceService.startListener(this, LiveWallpaperService.this);
                        SlideshowService.stopListener(LiveWallpaperService.this);
                        break;
                }
            }

            switch (activeService){
                case ServiceUtils.SERVICE_SLIDESHOW:
                    setWallpaper(SlideshowService.getBestWallpaperUri(LiveWallpaperService.this));
                    break;
                case ServiceUtils.SERVICE_GEOFENCE:
                    setWallpaper(GeofenceService.getBestWallpaperUri(LiveWallpaperService.this));
                    break;
            }
        }

        @Override
        public void onDestroy() {
            mRenderer.onDestroy();

            int activeService = ServiceUtils.getActiveService(LiveWallpaperService.this);
            if(!isPreview()){
                switch (activeService){
                    case ServiceUtils.SERVICE_SLIDESHOW:
                        SlideshowService.stopListener(LiveWallpaperService.this);
                        break;
                    case ServiceUtils.SERVICE_GEOFENCE:
                        GeofenceService.stopListener(LiveWallpaperService.this);
                        break;
                }
            }
            LiveWallpaperService.this.unregisterReceiver(mReceiver);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            mVisible = visible;
            if(mVisible && mQueuedWallpaper != null){
                setWallpaper(mQueuedWallpaper);
            }else if(!mVisible && mTask != null && !mTask.isCancelled()){
                mTask.cancel(true);
            }
        }

        @Override
        public void onOffsetsChanged(final float xOffset, float yOffset, float xOffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset,
                    yPixelOffset);
            mRenderer.setXOffset(xOffset);
            requestRender();
        }

        public void setWallpaper(Uri wallpaper){
            if(mVisible){
                if(mCurrentWallaper == null || wallpaper.compareTo(mCurrentWallaper) != 0){
                    if(mTask != null && !mTask.isCancelled()){
                        mTask.cancel(true);
                    }
                    mTask = new SetWallpaperTask();
                    mTask.execute(wallpaper);
                }
            }else{
                mQueuedWallpaper = wallpaper;
            }
        }

        @Override
        public void requestRender(){
            if(mVisible)
                super.requestRender();
        }

        @Override
        public void onWallpaperChanged(Uri wallpaper) {
            if(wallpaper != null){
                setWallpaper(wallpaper);
            }else {
                Logger.e(LOGTAG, "Wallpaper uri is null");
            }
        }

        private class SetWallpaperTask extends AsyncTask<Uri, Void, Bitmap> {
            private Uri wpUri;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                System.gc();
            }

            @Override
            protected Bitmap doInBackground(Uri... uris) {
                wpUri = uris[0];
                Rect size = BitmapIO.getImageSize(LiveWallpaperService.this, wpUri);
                Point displaySize = MiscUtils.UI.getDisplaySize(LiveWallpaperService.this);
                if(size == null || displaySize == null){
                    return  BitmapIO.loadBitmap(LiveWallpaperService.this, wpUri, false);
                }else {
                    int heigth = Math.max(displaySize.x, displaySize.y);
                    int width = Math.round(heigth * ((float) size.width() / size.height()));
                    return BitmapIO.loadBitmap(LiveWallpaperService.this, wpUri,
                            width, heigth, false);
                }
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if(bitmap != null && mVisible){
                    final int crossfadeDuration = Integer.valueOf(Preferences.getString(LiveWallpaperService.this,
                            R.string.preference_wallpaper_crossfade_duration,
                            getString(R.string.pref_crossfade_duration_default_value)));
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mRenderer.setWallpaper(bitmap, crossfadeDuration);
                        }
                    });
                    mQueuedWallpaper = null;
                    mCurrentWallaper = wpUri;
                    requestRender();
                }else if(bitmap != null){
                    bitmap.recycle();
                    System.gc();
                }
            }

            @Override
            protected void onCancelled(Bitmap bitmap) {
                if(bitmap != null){
                    bitmap.recycle();
                    bitmap = null;
                }
                System.gc();
            }
        }

        private class ServiceActivatedReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                initService();
            }
        }
    }
}
