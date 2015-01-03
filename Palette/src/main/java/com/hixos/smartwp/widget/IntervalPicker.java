package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.drew.lang.annotations.NotNull;
import com.hixos.smartwp.R;

import java.util.ArrayList;
import java.util.List;

public class IntervalPicker extends View {

    private OnIntervalSelectedListener mListener;
    private int mWidth, mHeight;
    private Context mContext;
    //View values
    private int mRange = 60;
    private float mStep;
    private int mTextCount;
    private float mTextStep;
    private int mClockStep;
    private int mCurrentInterval = 1;
    private long mLastVibration;
    private int mVibrationStep;
    //Dimensions
    private float mTouchableAreaWidth;
    //View center
    private int mCenterX, mCenterY;
    private float mOuterRadius;
    private float mTouchableOuterRadius;
    private float mTouchableInnerRadius;
    private float mCursorRadius;
    //COORDS
    private float mCursorX, mCursorY;
    private float mNeedleX, mNeedleY;
    private boolean mPressed = false;
    private Drawable mCursor;
    //Paints
    private Paint mBgPaint;
    private Paint mNeedlePaint;
    private Paint mClockTextPaint;
    private List<Point> textPositions;


    public IntervalPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initView();
    }

    public IntervalPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
    }

    public IntervalPicker(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public int getRange() {
        return mRange;
    }

    public void setRange(int range) {
        mRange = range;

        mCurrentInterval = getCorrectInterval(mCurrentInterval);

        if (mListener != null)
            mListener.onIntervalSelected(mCurrentInterval);
        updateStep();

        updateNeedlePosition();
        updateTextPositions();

        invalidate();
    }

    public int getInterval() {
        return mCurrentInterval;
    }

    public void setInterval(int interval) {
        this.mCurrentInterval = getCorrectInterval(interval);
        updateNeedlePosition();

        if (mListener != null)
            mListener.onIntervalSelected(mCurrentInterval);


    }

    private int getCorrectInterval(int interval) {
        int out = interval;
        if (interval > mRange) out = mRange;
        if (interval < 1) out = 1;

        return out;
    }

    public void setOnIntervalSelectedListener(OnIntervalSelectedListener listener) {
        this.mListener = listener;
    }

    private void initView() {
        mVibrationStep = getResources().getInteger(R.integer.picker_vibration_step);
        mCursor = mContext.getResources().getDrawable(R.drawable.interval_picker_cursor);
        mTouchableAreaWidth = mContext.getResources().getDimensionPixelSize(R.dimen.picker_touchable_area_width);
        mCursorRadius = mTouchableAreaWidth / 2;
        textPositions = new ArrayList<>();
        initPaints();
        updateStep();
    }

    private void updateStep() {
        mStep = (2 * (float) Math.PI) / mRange;
        if (mRange <= 12) {
            mTextCount = mRange;
            mTextStep = mStep;
        } else {
            mTextCount = 12;
            mTextStep = (float) Math.PI / 6;
        }
        mClockStep = mRange / mTextCount;
    }

    private void initPaints() {
        mBgPaint = new Paint();
        mBgPaint.setColor(mContext.getResources().getColor(R.color.picker_background));
        mBgPaint.setAntiAlias(true);

        mNeedlePaint = new Paint();
        mNeedlePaint.setColor(mContext.getResources().getColor(R.color.picker_cursor_accent));
        mNeedlePaint.setAntiAlias(true);
        mNeedlePaint.setStrokeWidth(4);

        mClockTextPaint = new Paint();
        if (!isInEditMode()) {
            mClockTextPaint.setTypeface(Fonts.getTypeface(mContext, Fonts.STYLE_LIGHT | Fonts.STYLE_CONDENSED));
        }
        mClockTextPaint.setAntiAlias(true);
        mClockTextPaint.setTextSize(mContext.getResources().getDimensionPixelSize(R.dimen.picker_text_size));
        mClockTextPaint.setColor(mContext.getResources().getColor(R.color.picker_text));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        mWidth = widthSize;

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            mHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            mHeight = Math.min(mWidth, heightSize);
        } else {
            mHeight = mWidth;
        }

        initDimensions();

        updateNeedlePosition();
        updateTextPositions();
        //MUST CALL THIS
        setMeasuredDimension(mWidth, mHeight);

    }

    private void initDimensions() {
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;

        mOuterRadius = (mWidth < mHeight ? mWidth / 2 : mHeight / 2);

        mTouchableOuterRadius = mOuterRadius - getResources().getDimensionPixelSize(R.dimen.picker_outer_margin);
        mTouchableInnerRadius = mTouchableOuterRadius - mTouchableAreaWidth;
    }

    private void updateNeedlePosition() {
        double radians = (2 * Math.PI * mCurrentInterval) / mRange - Math.PI / 2;
        mCursorX = getX(radians, mTouchableInnerRadius + mTouchableAreaWidth / 2, mCenterX) - mCursorRadius;
        mCursorY = getY(radians, mTouchableInnerRadius + mTouchableAreaWidth / 2, mCenterY) - mCursorRadius;

        mNeedleX = getX(radians, mTouchableInnerRadius, mCenterX);
        mNeedleY = getY(radians, mTouchableInnerRadius, mCenterY);
    }

    private void updateTextPositions() {
        textPositions.clear();
        for (int i = 1; i <= mTextCount; i++) {
            double angle = mTextStep * i - Math.PI / 2;
            float x = getX(angle, (mTouchableInnerRadius + mTouchableOuterRadius) / 2, mCenterX);
            float y = getY(angle, (mTouchableInnerRadius + mTouchableOuterRadius) / 2, mCenterY);

            int n = mClockStep * i;
            String s = String.format("%02d", n);
            x = x - mClockTextPaint.measureText(s) / 2;
            y = y + (mClockTextPaint.getTextSize() * 0.7f) / 2;
            Point p = new Point();
            p.x = (int) x;
            p.y = (int) y;
            textPositions.add(p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (isInsideTouchableArea(x, y)) {
                    onTouch(x, y);
                    mPressed = true;
                    mCursor.setState(new int[]{android.R.attr.state_pressed});
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (mPressed) {
                    onTouch(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mPressed) {
                    onTouch(x, y);
                    mPressed = false;
                    mCursor.setState(new int[]{});
                }
                break;

        }
        return true;
    }

    private void setIntervalByRadians(double radians) {
        radians = radians + Math.PI / 2;
        if (radians < 0)
            radians = radians + 2 * Math.PI;

        int interval = (int) Math.round(radians * mRange / (Math.PI * 2));

        if (interval == 0) interval = mRange;

        if (interval != mCurrentInterval) {
            if (mListener != null)
                mListener.onIntervalSelected(interval);


            if (System.currentTimeMillis() - mLastVibration > mVibrationStep) {
                mLastVibration = System.currentTimeMillis();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }

            mCurrentInterval = interval;
        }
    }

    private void onTouch(float x, float y) {
        double radians = Math.atan2(x - mCenterX, mCenterY - y);
        radians = ((float) Math.round(radians / mStep)) * mStep - (Math.PI / 2);

        mCursorX = getX(radians, mTouchableInnerRadius + mTouchableAreaWidth / 2, mCenterX) - mCursorRadius;
        mCursorY = getY(radians, mTouchableInnerRadius + mTouchableAreaWidth / 2, mCenterY) - mCursorRadius;

        mNeedleX = getX(radians, mTouchableInnerRadius, mCenterX);
        mNeedleY = getY(radians, mTouchableInnerRadius, mCenterY);

        setIntervalByRadians(radians);
        invalidate();
    }

    private boolean isInsideTouchableArea(float x, float y) {
        double distance = getDistance(mCenterX, mCenterY, x, y);
        return distance > mTouchableInnerRadius && distance < mOuterRadius;
    }

    private double getDistance(float x1, float y1, float x2, float y2) {
        float distX = x1 - x2;
        float distY = y1 - y2;
        return Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mBgPaint);
        canvas.drawLine(mCenterX, mCenterY, mNeedleX, mNeedleY, mNeedlePaint);
        drawNumbers(canvas);
        drawCursor(canvas);

    }

    private void drawNumbers(Canvas canvas) {
        for (int i = 0; i < textPositions.size(); i++) {
            int n = mClockStep * (i + 1);
            String s = String.format("%02d", n);
            canvas.drawText(s, textPositions.get(i).x, textPositions.get(i).y, mClockTextPaint);
        }
    }

    private void drawCursor(Canvas canvas) {
        Rect rect = new Rect((int) mCursorX, (int) mCursorY, (int) (mCursorX + mCursorRadius * 2), (int) (mCursorY + mCursorRadius * 2));
        mCursor.setBounds(rect);
        mCursor.draw(canvas);
    }

    private float getX(double angle, float radius, float centerX) {
        return (float) Math.cos(angle) * radius + centerX;
    }

    private float getY(double angle, float radius, float centerY) {
        return (float) Math.sin(angle) * radius + centerY;
    }

    public interface OnIntervalSelectedListener {
        public void onIntervalSelected(int interval);
    }
}
