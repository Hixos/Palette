package com.hixos.smartwp.wallpaper;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Luca on 21/03/2014.
 */
public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String LOGTAG = "GLRenderer";

    private LiveWallpaperService.Callbacks mCallbacks;

    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mVPMatrix = new float[16];

    private float mRatio;
    private float mXOffset;

    private GLWallpaper mCurrentWallpaper;
    private GLWallpaper mNextWallpaper;

    private TickingFloatAnimator mCrossfadeAnimator = TickingFloatAnimator.create().from(0);

    public GLRenderer(LiveWallpaperService.Callbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_ONE, GLES20.GL_ONE);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLWallpaper.initGL();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mRatio = (float)width / height;
        Matrix.orthoM(mProjectionMatrix, 0,
                0, mRatio * 2,
                -1, 1,
                1, 10);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 1, 0, 0, -1, 0, 1, 0);
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        boolean animate = mCrossfadeAnimator.tick();

        if(mCurrentWallpaper != null){
            mCurrentWallpaper.draw(mVPMatrix, mXOffset, mRatio, 1.0f);
        }

        if(mNextWallpaper != null){
            float alpha = mCrossfadeAnimator.currentValue();
            mNextWallpaper.draw(mVPMatrix, mXOffset, mRatio, alpha);
        }

        if(animate){
            mCallbacks.requestRender();
        }
    }

    public void onDestroy(){
        if(mNextWallpaper != null){
            mNextWallpaper.destroy();
            mNextWallpaper = null;
        }
        if(mCurrentWallpaper != null){
            mCurrentWallpaper.destroy();
            mCurrentWallpaper = null;
        }
    }

    public void setXOffset(float xOffset){
        mXOffset = xOffset;
        mCallbacks.requestRender();
    }

    public void setWallpaper(Bitmap wallpaper, int crossFadeDuration){
        loadWallpaper(wallpaper, crossFadeDuration);
    }

    private void loadWallpaper(Bitmap wallpaper, int crossFadeDuration){
        if(mNextWallpaper != null){
            mCrossfadeAnimator.cancel();
            swapWallpapers();
        }

        mNextWallpaper = new GLWallpaper(wallpaper);
        wallpaper.recycle();
        wallpaper = null;
        System.gc();
        startCrossfadeAnimation(mCurrentWallpaper != null ? crossFadeDuration : 250);
        mCallbacks.requestRender();
    }

    private void startCrossfadeAnimation(int duration){
        mCrossfadeAnimator.from(0).to(1)
                .withDuration(duration)
                .withEndListener(new Runnable() {
                    @Override
                    public void run() {
                        swapWallpapers();
                    }
                }).start();
    }

    private void swapWallpapers(){
        if (mNextWallpaper != null) {
            if(mCurrentWallpaper != null)
                mCurrentWallpaper.destroy();

            mCurrentWallpaper = mNextWallpaper;
            mNextWallpaper = null;
            mCallbacks.requestRender();
            System.gc();
        }
    }
}
