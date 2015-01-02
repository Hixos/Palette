package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.ImageLoadTask;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.RectUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Straight;
import com.hixos.smartwp.wallpaper.WallpaperUtils;

/**
 * Created by Luca on 08/03/14.
 */
public class CropView extends View {

    public static final int CROP_MODE_NONE = -1;
    public static final int CROP_MODE_DEFAULT = 0;
    public static final int CROP_MODE_KITKAT = 1;
    public static final int CROP_MODE_PORTRAIT = 2;
    public static final int CROP_MODE_LANDSCAPE = 3;

    private static final byte ACTION_NONE = -1;
    private static final byte ACTION_DRAG_TOP_LEFT = 0;
    private static final byte ACTION_DRAG_TOP_RIGHT = 1;
    private static final byte ACTION_DRAG_BOTTOM_LEFT = 2;
    private static final byte ACTION_DRAG_BOTTOM_RIGHT = 3;
    private static final byte ACTION_MOVE = 4;

    private int mTouchAction = ACTION_NONE;

    //Picture
    private Bitmap mImage;
    private float mImageRatio;
    private RectF mImageSize;

    private RectF mImageContainer;

    //Crop
    private float mPreviousX, mPreviousY;

    private int mCropMode = CROP_MODE_NONE;
    private RectF mCropArea;
    private RectF mKitkatCropArea;

    private float mCropRatio;
    private float mKitkatCropRatio;

    private float mCropAreaMinWidth;

    private Straight mTouchStraight, mCropStraight;

    //UX & UI
    private BitmapDrawable mHandleDrawable;
    private Rect mHandleBounds;
    private int mHandleSize;
    private float mTouchRadius;

    private RectF mDimArea;
    private RectF mScreenArea;

    private float mPortraitScreenRatio;

    private Paint mImagePaint;
    private Paint mCropPaint;
    private Paint mDimPaint;
    private Paint mScreenPaint;

    //OTHER
    private RectF mSavedCropArea;
    private boolean mTouchEnabled = true;

    private ImageManager.OnImageLoadedListener mListener;

    public CropView(Context context) {
        super(context);
        init();
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mImage = null;
        mImageRatio = 0;
        mImageSize = new RectF();

        mImageContainer = new RectF();

        mHandleDrawable = (BitmapDrawable)getResources().getDrawable(R.drawable.ic_cropper_handle);
        mHandleSize = mHandleDrawable.getBitmap().getWidth();
        mHandleBounds = new Rect();
        mDimArea = new RectF();
        mScreenArea = new RectF();
        mKitkatCropArea = new RectF();

        mTouchRadius = getResources().getDimensionPixelSize(R.dimen.cropper_handle_touch_radius);
        initPaints();
    }

    private void initPaints(){
        mImagePaint = new Paint();
        mCropPaint = new Paint();
        mCropPaint.setColor(getContext().getResources().getColor(R.color.cropper_primary_line));
        mCropPaint.setAntiAlias(true);
        mCropPaint.setStrokeWidth(getContext().getResources().getDimensionPixelSize(R.dimen.cropper_primary_stroke_width));
        mCropPaint.setStyle(Paint.Style.STROKE);

        mDimPaint = new Paint();
        mDimPaint.setColor(getContext().getResources().getColor(R.color.cropper_dim_area));

        mScreenPaint = new Paint();
        mScreenPaint.setColor(getContext().getResources().getColor(R.color.cropper_secondary_line));
        mScreenPaint.setAntiAlias(true);
        mScreenPaint.setStrokeWidth(getContext().getResources().getDimensionPixelSize(R.dimen.cropper_secondary_stroke_width));
        mScreenPaint.setStyle(Paint.Style.STROKE);
    }

    public void onSaveInstanceState(Bundle outState){
        outState.putInt("crop_mode", mCropMode);

        if(mCropArea == null || mImageContainer == null ){
            outState.putBoolean("restore_crop", false);
        }else {
            RectF relativeCrop = RectUtils.getRelativeRect(mCropArea, mImageContainer);
            outState.putFloat("crop_left", relativeCrop.left);
            outState.putFloat("crop_right", relativeCrop.right);
            outState.putFloat("crop_top", relativeCrop.top);
            outState.putFloat("crop_bottom", relativeCrop.bottom);
            outState.putBoolean("restore_crop", true);
        }
    }

    public void onRestoreInstanceState(Bundle inState){
        if(inState.getBoolean("restore_crop", false)){
            mSavedCropArea = new RectF();
            mSavedCropArea.left = inState.getFloat("crop_left");
            mSavedCropArea.right = inState.getFloat("crop_right");
            mSavedCropArea.top = inState.getFloat("crop_top");
            mSavedCropArea.bottom = inState.getFloat("crop_bottom");
        }else {
            mSavedCropArea = null;
        }

        mCropMode = inState.getInt("crop_mode");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth, measuredHeight;

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(mImageRatio == 0){
            measuredWidth = widthSize;
            measuredHeight = heightSize;
        }else{
            if(mImage != null){
                mSavedCropArea = RectUtils.getRelativeRect(mCropArea, mImageContainer);
            }
            int imageWidth, imageHeight;
            float viewRatio = ((float)widthSize) / heightSize;

            if(mImageRatio > viewRatio){
                measuredWidth = widthSize;
                measuredHeight = ((int) (widthSize / mImageRatio));
            }else{
                measuredHeight = heightSize;
                measuredWidth = ((int) (heightSize * mImageRatio));
            }

            if(mImageRatio > 1){
                imageHeight = measuredHeight - mHandleSize;
                imageWidth = ((int) (imageHeight * mImageRatio));
            }else{
                imageWidth = measuredWidth - mHandleSize;
                imageHeight = ((int) (imageWidth / mImageRatio));
            }

            int left = (measuredWidth - imageWidth) / 2;
            int top = (measuredHeight - imageHeight) / 2;

            mImageContainer.left = left;
            mImageContainer.top = top;
            mImageContainer.right = left + imageWidth;
            mImageContainer.bottom = top + imageHeight;

            mCropAreaMinWidth = mImageContainer.width() * 0.15f;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mImage != null){
            canvas.drawBitmap(mImage, null, mImageContainer, mImagePaint);
            drawDimArea(canvas);
            drawScreens(canvas);
            drawCropArea(canvas);
            drawHandles(canvas);
        }
    }

    private void drawCropArea(Canvas canvas){
        if(mCropMode == CROP_MODE_KITKAT){
            canvas.drawRect(mKitkatCropArea, mScreenPaint);
        }

        canvas.drawRect(mCropArea, mCropPaint);
    }

    private void drawHandles(Canvas canvas){
        int halfSize = mHandleSize / 2;
        mHandleBounds.left = (int)mCropArea.left - halfSize;
        mHandleBounds.right = (int)mCropArea.left + halfSize;
        mHandleBounds.top = (int)mCropArea.top - halfSize;
        mHandleBounds.bottom = (int)mCropArea.top + halfSize;

        mHandleDrawable.setBounds(mHandleBounds);
        mHandleDrawable.draw(canvas);

        mHandleBounds.left = (int)mCropArea.right - halfSize;
        mHandleBounds.right = (int)mCropArea.right + halfSize;

        mHandleDrawable.setBounds(mHandleBounds);
        mHandleDrawable.draw(canvas);

        mHandleBounds.top = (int)mCropArea.bottom - halfSize;
        mHandleBounds.bottom = (int)mCropArea.bottom + halfSize;

        mHandleDrawable.setBounds(mHandleBounds);
        mHandleDrawable.draw(canvas);

        mHandleBounds.left = (int)mCropArea.left - halfSize;
        mHandleBounds.right = (int)mCropArea.left + halfSize;

        mHandleDrawable.setBounds(mHandleBounds);
        mHandleDrawable.draw(canvas);
    }

    private void drawDimArea(Canvas canvas){
        boolean kk = mCropMode == CROP_MODE_KITKAT;

        mDimArea.left = mImageContainer.left;
        mDimArea.right = mCropArea.left;
        mDimArea.top = mImageContainer.top;
        mDimArea.bottom = mImageContainer.bottom;
        canvas.drawRect(mDimArea, mDimPaint);

        mDimArea.left = kk ? mKitkatCropArea.right : mCropArea.right;
        mDimArea.right = mImageContainer.right;
        canvas.drawRect(mDimArea, mDimPaint);

        mDimArea.left = mCropArea.left;
        mDimArea.right = kk ? mKitkatCropArea.right : mCropArea.right;
        mDimArea.bottom = mCropArea.top;
        canvas.drawRect(mDimArea, mDimPaint);

        mDimArea.bottom = mImageContainer.bottom;
        mDimArea.top = mCropArea.bottom;
        canvas.drawRect(mDimArea, mDimPaint);
    }

    private void drawScreens(Canvas canvas){
        if(mCropMode == CROP_MODE_DEFAULT){
            float halfHeight = mCropArea.height() / 2;
            float halfWidth = mCropArea.height() * mPortraitScreenRatio / 2;

            float centerX = (mCropArea.right + mCropArea.left) / 2f;
            float centerY = (mCropArea.top + mCropArea.bottom) / 2f;

            mScreenArea.left = centerX - halfWidth;
            mScreenArea.right = centerX + halfWidth;
            mScreenArea.top = centerY - halfHeight;
            mScreenArea.bottom = centerY + halfHeight;

            canvas.drawRect(mScreenArea, mScreenPaint);

            mScreenArea.left = centerX - halfHeight;
            mScreenArea.right = centerX + halfHeight;
            mScreenArea.top = centerY - halfWidth;
            mScreenArea.bottom = centerY + halfWidth;

            canvas.drawRect(mScreenArea, mScreenPaint);


        }else if(mCropMode == CROP_MODE_KITKAT){
            mScreenArea.left = mCropArea.left;
            mScreenArea.right = Math.min(mCropArea.left + mCropArea.height(), mImageContainer.right);

            float halfHeight = mScreenArea.width() / scRatio / 2;
            float centerY = (mCropArea.top + mCropArea.bottom) / 2f;

            mScreenArea.top = centerY - halfHeight;
            mScreenArea.bottom = centerY + halfHeight;


            canvas.drawRect(mScreenArea, mScreenPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mCropMode != CROP_MODE_NONE && mTouchEnabled){
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    return startAction(x, y);
                case MotionEvent.ACTION_MOVE:
                    execAction(x, y);
                    invalidate();
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private boolean startAction(float x, float y){
        PointF handles[] = new PointF[4];
        handles[0] = new PointF(mCropArea.left, mCropArea.top);
        handles[1] = new PointF(mCropArea.right, mCropArea.top);
        handles[2] = new PointF(mCropArea.left, mCropArea.bottom);
        handles[3] = new PointF(mCropArea.right, mCropArea.bottom);

        mTouchAction = mCropArea.contains(x,y) ? ACTION_MOVE : ACTION_NONE;

        for(int i = 0; i < 4; i++){
            float dX = x - handles[i].x;
            float dY = y - handles[i].y;
            float distance = (float)Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
            if(distance < mTouchRadius){
                mTouchAction = i;
                break;
            }
        }
        switch (mTouchAction){
            case ACTION_MOVE:
                mPreviousX = x;
                mPreviousY = y;
                break;
            case ACTION_DRAG_BOTTOM_LEFT:
            case ACTION_DRAG_TOP_RIGHT:
                mCropStraight = new Straight(- 1 / mCropRatio, 0);
                mCropStraight.passTrough(mCropArea.left, mCropArea.bottom);
                mTouchStraight = new Straight(1, 0);
                break;
            case ACTION_DRAG_TOP_LEFT:
            case ACTION_DRAG_BOTTOM_RIGHT:
                mCropStraight = new Straight(1 / mCropRatio, 0);
                mCropStraight.passTrough(mCropArea.left, mCropArea.top);
                mTouchStraight = new Straight(-1, 0);
                break;
        }

        return mTouchAction != ACTION_NONE;
    }

    private void execAction(float x, float y){
        switch (mTouchAction){
            case ACTION_NONE:
                break;
            case ACTION_MOVE:
                float dX = x - mPreviousX;
                float dY = y - mPreviousY;

                float offsetX = Math.max(Math.min(dX,
                                mImageContainer.right - mCropArea.width() - mCropArea.left),
                        mImageContainer.left - mCropArea.left);

                float offsetY = Math.max(Math.min(dY,
                                mImageContainer.bottom - mCropArea.height() - mCropArea.top),
                        mImageContainer.top - mCropArea.top);

                mCropArea.offset(offsetX, offsetY);

                if(mCropMode == CROP_MODE_KITKAT) updateKitkatCropArea();
                mPreviousX = x;
                mPreviousY = y;
                break;
            default:
                mTouchStraight.passTrough(x, y);
                PointF intersection = fixIntersection(mTouchStraight.intersect(mCropStraight));

                if(mTouchAction == ACTION_DRAG_TOP_LEFT || mTouchAction == ACTION_DRAG_TOP_RIGHT){
                    mCropArea.top = intersection.y;
                }else{
                    mCropArea.bottom = intersection.y;
                }

                if(mTouchAction == ACTION_DRAG_TOP_LEFT || mTouchAction == ACTION_DRAG_BOTTOM_LEFT){
                    mCropArea.left = intersection.x;
                }else{
                    mCropArea.right = intersection.x;
                }

                if(mCropMode == CROP_MODE_KITKAT) updateKitkatCropArea();
                break;
        }
    }

    private PointF fixIntersection(PointF intersection){
        boolean left = mTouchAction == ACTION_DRAG_TOP_LEFT || mTouchAction == ACTION_DRAG_BOTTOM_LEFT;

        if(mImageContainer.contains(intersection.x, intersection.y)
                || (left && intersection.x > mCropArea.right)
                || (!left &&  intersection.x < mCropArea.left)){
            if(left){
                float x = Math.min(intersection.x, mCropArea.right - mCropAreaMinWidth);
                return new PointF(x, mCropStraight.y(x));
            }else{
                float x = Math.max(intersection.x, mCropArea.left + mCropAreaMinWidth);
                return new PointF(x, mCropStraight.y(x));
            }
        }else{
            Straight h;
            h = mTouchAction == ACTION_DRAG_TOP_LEFT || mTouchAction == ACTION_DRAG_TOP_RIGHT
                    ? new Straight(0, mImageContainer.top)
                    : new Straight(0, mImageContainer.bottom);

            PointF newIntersection = h.intersect(mCropStraight);
            if(newIntersection != null
                    && mImageContainer.left <= newIntersection.x
                    && newIntersection.x <= mImageContainer.right){
                return newIntersection;
            }else if(left){
                float x = mImageContainer.left;
                return new PointF(x, mCropStraight.y(x));
            }else{
                float x = mImageContainer.right;
                return new PointF(x, mCropStraight.y(x));
            }
        }
    }

    private void updateKitkatCropArea(){
        float width = mCropArea.height() * mKitkatCropRatio;

        mKitkatCropArea.top = mCropArea.top;
        mKitkatCropArea.bottom = mCropArea.bottom;

        mKitkatCropArea.left = mCropArea.left;
        mKitkatCropArea.right = Math.min(mCropArea.left + width, mImageContainer.right);

    }

    public void setImageURI(final Uri uri, ImageManager.OnImageLoadedListener listener){
        init();

        mListener = listener;
        AsyncTask<Void, Void, Void> dimensionsTask = new AsyncTask<Void, Void, Void>() {
            private RectF mDimensions = new RectF();

            @Override
            protected Void doInBackground(Void... voids) {
                Log.w("Rot", "BG");
                mDimensions.set(BitmapIO.getImageSize(getContext(), uri));
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                updateLayoutForImage(uri, mDimensions);
            }
        };

        dimensionsTask.execute();
    }

    private void updateLayoutForImage(final Uri image,  RectF dimensions){
        mImageSize.right = dimensions.right;
        mImageSize.bottom = dimensions.bottom;

        mImageRatio = mImageSize.width() / mImageSize.height();

        final ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    loadImage(image);
                    return true;
                }
            });
        }

        requestLayout();
    }

    private void loadImage(final Uri uri){
        ImageLoadTask loader = new ImageLoadTask(getContext(), ((int)mImageContainer.width()), ((int) mImageContainer.height()),
                "", new ImageManager.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(Bitmap image, String id) {
                mImage = image;
                if(mListener != null)
                    mListener.onImageLoaded(image, id);
                updateCropMode();
                invalidate();
            }
        });
        loader.execute(uri);
    }

    public void setCropMode(int cropMode){
        if(mCropMode != CROP_MODE_NONE && cropMode != mCropMode){
            mSavedCropArea = null;
        }
        mCropMode = cropMode;
        updateCropMode();
    }

    float scRatio;
    private void updateCropMode(){
        Point screen = MiscUtils.UI.getDisplaySize(getContext());
        mPortraitScreenRatio = WallpaperUtils.getPortraitCropRatio(screen.x, screen.y);

        switch (mCropMode){
            case CROP_MODE_NONE:
                return;
            case CROP_MODE_DEFAULT:
                mCropRatio = WallpaperUtils.getDefaultCropRatio(screen.x, screen.y, getContext());
                break;
            case CROP_MODE_KITKAT:
                mKitkatCropRatio = WallpaperUtils.getDefaultCropRatio(screen.x, screen.y, getContext());
                mCropRatio =  WallpaperUtils.getKitkatMinCropRatio(screen.x, screen.y);
                scRatio = (float)Math.max(screen.x, screen.y) / (float)Math.min(screen.x, screen.y);
               // Logger.w("Test", "Screen", screen.x, screen.y, scRatio);
                break;
            case CROP_MODE_PORTRAIT:
                mCropRatio = WallpaperUtils.getPortraitCropRatio(screen.x, screen.y);
                break;
            case CROP_MODE_LANDSCAPE:
                mCropRatio = WallpaperUtils.getLandscapeCropRatio(screen.x, screen.y);
                break;
        }

        mCropArea = mSavedCropArea != null
                ? RectUtils.getFullRect(mSavedCropArea, mImageContainer)
                : RectUtils.getCropArea(mCropRatio, mImageContainer);

        if(mCropMode == CROP_MODE_KITKAT) updateKitkatCropArea();

        invalidate();
    }

    public RectF getRelativeCropRect(){
        return mCropMode == CROP_MODE_KITKAT
                ? RectUtils.getRelativeRect(mKitkatCropArea, mImageContainer)
                : RectUtils.getRelativeRect(mCropArea, mImageContainer);
    }

    public int getCropMode(){
        return mCropMode;
    }

    public void enableTouch(boolean enable){
        mTouchEnabled = enable;
    }
}
