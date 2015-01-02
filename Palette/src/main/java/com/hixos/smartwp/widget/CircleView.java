package com.hixos.smartwp.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.hixos.smartwp.R;

/**
 * Created by Luca on 18/04/2014.
 */
public class CircleView extends View {

    private float mRadiusPerc = 0.6f;
    private int mMaxRadius;
    private int mRadius;

    private float mAlpha = 0.4f;
    private int mColor = Color.BLUE;

    private int mWidth, mHeight;
    private int mMaxWidth, mMaxHeight;

    private int mCenterX, mCenterY;

    private Paint mPaint;
    private Paint mMaskPaint;
    private Paint mWhitePaint;
    private Paint mBlackPaint;

    private BitmapDrawable mDrawable;
    private Rect mDrawableBounds;
    private float mDrawableFixedRadius = 0;

    private Drawable mForeground;
    private boolean mPressing = false;
    private OnClickListener mClickListener;

    public CircleView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CircleView,
                    0, 0);
            mAlpha = Math.max(Math.min(a.getFloat(R.styleable.CircleView_circleAlpha, 0.4f), 1), 0);
            mColor = a.getColor(R.styleable.CircleView_circleColor,
                    isInEditMode() ? getResources().getColor(R.color.accent_blue) : Color.BLUE);
            mDrawable = (BitmapDrawable)a.getDrawable(R.styleable.CircleView_innerDrawable);
            mForeground = a.getDrawable(R.styleable.CircleView_foreground);
            if(mForeground != null){
                mForeground.setCallback(this);
            }
            if(isInEditMode() && mDrawable == null){
                mDrawable = (BitmapDrawable)getResources().getDrawable(R.drawable.ic_slideshow);
            }
            mMaxWidth = a.getDimensionPixelSize(R.styleable.CircleView_maxWidth, 0);
            mMaxHeight = a.getDimensionPixelSize(R.styleable.CircleView_maxHeight, 0);
            mDrawableFixedRadius = a.getFloat(R.styleable.CircleView_innerDrawableFixedRadius, 0);
            mRadiusPerc = a.getFloat(R.styleable.CircleView_circleRadius, 0.6f);
        }
        mPaint = new Paint();
        mPaint.setColor(mColor);
        mPaint.setAntiAlias(true);
        mPaint.setAlpha(Math.round(mAlpha * 255));

        mMaskPaint = new Paint();
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mWhitePaint = new Paint();
        mWhitePaint.setColor(Color.WHITE);
        mWhitePaint.setAntiAlias(true);

        mBlackPaint = new Paint();
        mBlackPaint.setColor(Color.BLACK);

        mDrawableBounds = new Rect();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if(mMaxWidth > 0 && mMaxWidth < widthSize){
            widthSize = mMaxWidth;
            widthMode = MeasureSpec.AT_MOST;
        }

        if(mMaxHeight > 0 && mMaxHeight < heightSize){
            heightSize = mMaxHeight;
            heightMode = MeasureSpec.AT_MOST;
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize, heightMode));

        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();

        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        mMaxRadius = (int)Math.max(Math.min(mWidth - Math.max(getPaddingLeft(), getPaddingRight()) * 2,
                mHeight - Math.max(getPaddingTop(), getPaddingBottom()) * 2) / 2f, 0);
        updateRadius();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        int drawableRadius = mDrawableFixedRadius > 0
                ? (int)(mDrawableFixedRadius * mMaxRadius)
                : mRadius;
        if(mDrawable != null) {
            mDrawableBounds.left = Math.round(mCenterX - drawableRadius / 1.5f);
            mDrawableBounds.right = Math.round(mCenterX + drawableRadius / 1.5f);
            mDrawableBounds.top = Math.round(mCenterY - drawableRadius / 1.5f);
            mDrawableBounds.bottom = Math.round(mCenterY + drawableRadius / 1.5f);
            mDrawable.setBounds(mDrawableBounds);
            mDrawable.draw(canvas);
        }

    }

    @Override
    public void draw(Canvas canvas) {
        if(mForeground != null){
            canvas.saveLayer(null, new Paint(), Canvas.ALL_SAVE_FLAG);

            canvas.saveLayer(null, null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                    Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
            canvas.restore();
            super.draw(canvas);
            mForeground.draw(canvas);

            canvas.saveLayer(null, mMaskPaint, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG);
            canvas.drawCircle(mCenterX, mCenterY, mRadius, mWhitePaint);
            canvas.restore();
        }else{
            super.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mForeground);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if(mForeground != null)
            mForeground.jumpToCurrentState();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if(mForeground != null){
            mForeground.setState(getDrawableState());
            invalidate();
        }
    }

    @TargetApi(21)
    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mForeground != null) {
            mForeground.setHotspot(x, y);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState;
        if (mPressing) {
            int[] state_pressed = {android.R.attr.state_pressed};
            drawableState = super.onCreateDrawableState(extraSpace + 1);
            mergeDrawableStates(drawableState, state_pressed);
        } else {
            drawableState = super.onCreateDrawableState(extraSpace);
        }
        return drawableState;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int distance = (int)Math.sqrt(
                Math.pow(y - mCenterY, 2) + Math.pow(x - mCenterX, 2));
        boolean inside = distance <= mRadius;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(inside) {
                    mPressing = true;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        drawableHotspotChanged(x,y);
                    }
                    refreshDrawableState();
                    return true;
                }
            case MotionEvent.ACTION_UP:
                mPressing = false;
                refreshDrawableState();
                if(inside && mClickListener != null){
                    mClickListener.onClick(this);
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }
    protected void updateForegroundBounds(){
        if(mForeground != null){
            mForeground.setBounds(mCenterX - mRadius, mCenterY - mRadius,
                    mCenterX + mRadius, mCenterY + mRadius);
            getAlpha();
        }
    }

    public void setRadiusPerc(float perc){
        if(perc < 0){
            throw new IllegalArgumentException("Radius percentage must be >= 0");
        }
        mRadiusPerc = perc;
        updateRadius();
        invalidate();
    }

    public float getRadiusPerc(){
        return mRadiusPerc;
    }

    private void updateRadius(){
        mRadius = (int)(mRadiusPerc * mMaxRadius);
        updateForegroundBounds();
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
        mPaint.setColor(color);
        mPaint.setAlpha(Math.round(getAlpha() * 255));
        invalidate();
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = Math.max(Math.min(alpha, 1), 0);
        mPaint.setAlpha(Math.round(mAlpha * 255));
        invalidate();
    }

    public BitmapDrawable getInnerDrawable() {
        return mDrawable;
    }

    public void setInnerDrawable(BitmapDrawable innerDrawable) {
        this.mDrawable = innerDrawable;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
    }

    public void setForeground(Drawable foreground){
        if(mForeground != null){
            mForeground.setCallback(null);
        }
        mForeground = foreground;
        if(mForeground != null){
            mForeground.setCallback(this);
        }
    }
}
