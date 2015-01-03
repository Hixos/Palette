package com.hixos.smartwp.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;

public class AsyncImageView extends ImageView {

    private int mAnimationTime = 150;

    private boolean mMeasured = false;
    private float mRatio = 0;

    private ThumbnailLoadTask mLoadTask;
    private ObjectAnimator mAnimator;

    private ImageManager.OnImageLoadedListener mListener;

    public AsyncImageView(Context context) {
        super(context);
        init(context, null);
    }

    public AsyncImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.AsyncImageView,
                    0, 0);
            float r = a.getFloat(R.styleable.AsyncImageView_ratio, 0);
            setRatio(r);
        }

        ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mMeasured = true;
                    return true;
                }
            });
        }
    }

    public void setRatio(int width, int height) {
        setRatio((float) width / height);
    }

    public float getRatio() {
        return mRatio;
    }

    public void setRatio(float ratio) {
        mRatio = ratio;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!isInLayout())
                requestLayout();
        } else {
            requestLayout();
        }
    }

    public void setImageUID(String uid) {
        setImageUID(uid, null);
    }

    public void setImageUID(final String uid, ImageManager.OnImageLoadedListener listener) {
        mListener = listener;

        if (mMeasured) {
            setImageBitmap(null);
            loadImage(uid);
        } else {
            setImageBitmap(null);
            final ViewTreeObserver observer = getViewTreeObserver();
            if (observer != null) {
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(this);
                        loadImage(uid);
                        return true;
                    }
                });
            }
        }
    }

    private void loadImage(String uid) {
        if (mLoadTask != null && mLoadTask.isRunning()) {
            mLoadTask.cancel(true);
        }
        if (ImageManager.getInstance().isCached(uid)) {
            onImageLoaded(ImageManager.getInstance().getCachedThumbnail(uid), uid, false);
        } else {
            mLoadTask = new ThumbnailLoadTask(uid);
            mLoadTask.execute();
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        mMeasured = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float screenRatio = 1;
        if (heightSize > 0) {
            screenRatio = (float) widthSize / (float) heightSize;
        }

        int measuredWidth, measuredHeight;

        if (mRatio > 0) {
            if (screenRatio > mRatio) {
                measuredHeight = heightSize;
                measuredWidth = Math.round((float) heightSize * mRatio);
            } else {
                measuredWidth = widthSize;
                measuredHeight = Math.round((float) widthSize / mRatio);
            }
            setMeasuredDimension(measuredWidth, measuredHeight);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private void onImageLoaded(Bitmap bitmap, String uid, boolean animate) {
        setImageBitmap(bitmap);
        if (mListener != null) {
            mListener.onImageLoaded(bitmap, uid);
        }
        if (animate)
            fadeInAnimation();
    }

    private void fadeInAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            mAnimator = ObjectAnimator.ofInt(this, "ImageAlpha", 0, 255);
            mAnimator.setDuration(mAnimationTime).setInterpolator(new DecelerateInterpolator());
            mAnimator.start();
        }
    }

    class ThumbnailLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private String mUid;
        private boolean mRunning = false;

        public ThumbnailLoadTask(String uid) {
            mUid = uid;
        }

        public String getUid() {
            return mUid;
        }

        public boolean isRunning() {
            return mRunning;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return ImageManager.getInstance().getThumbnail(mUid, getWidth(), getHeight());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRunning = true;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mRunning = false;
            onImageLoaded(bitmap, mUid, true);
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            mRunning = false;
        }
    }
}
