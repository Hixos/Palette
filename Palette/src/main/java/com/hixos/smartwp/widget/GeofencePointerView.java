package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.hixos.smartwp.R;

public class GeofencePointerView extends View {
    private final static String LOGTAG = "GeofenceView";

    private Context mContext;

    private float mCenterX, mCenterY;
    private float mRadius;
    private float mBorderWidth;
    private Paint mBorderPaint;
    private Paint mInnerPaint;
    public GeofencePointerView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public GeofencePointerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public GeofencePointerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public float getRadius() {
        return mRadius;
    }

    private void init() {
        mBorderWidth = mContext.getResources()
                .getDimensionPixelSize(R.dimen.geofence_current_border_width);

        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);

        mInnerPaint = new Paint();
        mInnerPaint.setAntiAlias(true);

        setColor(Color.RED);
    }

    public void setColor(int color) {
        mBorderPaint.setColor(adjustAlpha(color, 0.75f));
        mInnerPaint.setColor(adjustAlpha(color, 0.35f));
        invalidate();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        float realWidth = getMeasuredWidth()
                - mBorderWidth
                - Math.max(getPaddingLeft(), getPaddingRight()) * 2;

        float realHeight = getMeasuredHeight()
                - mBorderWidth
                - Math.max(getPaddingTop(), getPaddingBottom()) * 2;

        mRadius = Math.min(realWidth / 2, realHeight / 2);

        mCenterX = (getPaddingLeft() + getMeasuredWidth() - getPaddingRight()) / 2;
        mCenterY = (getPaddingTop() + getMeasuredHeight() - getPaddingBottom()) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mInnerPaint);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mBorderPaint);
    }
}
