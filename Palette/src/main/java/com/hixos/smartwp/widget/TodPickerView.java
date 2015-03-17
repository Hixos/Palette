package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Hour24;

import java.util.ArrayList;
import java.util.List;

public class TodPickerView extends View {
    private static final int NONE = 0;
    private static final int AM = 0, PM = 1;

    private static final int DIR_NONE = 0;
    private static final int DIR_CW = 1;
    private static final int DIR_CCW = -1;

    //Cursors
    private int mActiveCursor = NONE;
    private static final int HOUR = 1;
    private static final int MINUTE = 2;
    private static final double HOUR_STEP = Math.PI / 6;
    private static final double HOUR_CURSOR_STEP = Math.PI / 72;
    private static final double MINUTE_STEP = Math.PI / 6;

    //Measures
    private Point mCenter;
    private int mMinuteOuterRadius, mMinuteInnerRadius;
    private int mHourInnerRadius;
    private int mTouchableAreaWidth;
    private int mAreaSeparator;
    private int mCursorRadius;
    private RectF mArcRect;
    private int mAmPmRadius;
    //Numbers
    private List<PointF> mMinuteTextCoords;
    private List<PointF> mHourTextCoords;
    private Rect mMinuteCursorBounds;
    private Rect mHourCursorBounds;
    private PointF mMinutePointerCoords;
    private PointF mHourPointerCoords;

    private Drawable mMinuteCursor;
    private Drawable mHourCursor;

    private Hour24 mHour;
    private Hour24 mLastHour;

    private double mLastTouchRadians;
    private int mDirection = DIR_NONE;

    private int mQuadrant;
    private Interval mQuadrantInterval;

    //Paints
    private Paint mMinuteBgPaint;
    private Paint mHourBgPaint;

    private Paint mMinuteTextPaint;
    private Paint mHourTextPaint;
    private Paint mAmPmTextPaint;

    private Paint mMinutePointerPaint, mHourPointerPaint;

    private Paint mArcPaint;
    private Paint mArcBorderPaint;

    private Path mPath;
    private PathEffect mDashEffect;

    //Vibration
    private long mLastVibration;
    private int mVibrationStep;

    //Alterady used hours
    private List<Interval> mUsedIntervals;
    private List<Interval> mIntervalMasks;

    private int mZeroPeriod = AM;

    //Active area
    private Interval mActiveInterval;

    private Hour24 mMaxHour = new Hour24(23,55);


    private boolean mCursorsEnabled = true;
    private OnTimeChangedListener mListener;

    public TodPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public TodPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TodPickerView(Context context) {
        super(context);
        initView();
    }

    private static double minuteToRadians(int minute) {
        return MINUTE_STEP * minute;
    }

    private static float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public Hour24 getTime() {
        return new Hour24(mHour);
    }

    private void initView() {
        mHour = new Hour24(0,0);
        mLastHour = new Hour24(0,0);
        mQuadrant = AM;
        mQuadrantInterval = new Interval(Hour24.Hour0000(), Hour24.Hour1200());

        mVibrationStep = getResources().getInteger(R.integer.picker_vibration_step);

        mTouchableAreaWidth = getContext().getResources()
                .getDimensionPixelSize(R.dimen.picker_touchable_area_width);
        mCursorRadius = mTouchableAreaWidth / 2;

        mMinuteTextCoords = new ArrayList<>();
        mHourTextCoords = new ArrayList<>();
        mCenter = new Point();
        mMinuteCursorBounds = new Rect();
        mHourCursorBounds = new Rect();
        mHourPointerCoords = new PointF();
        mMinutePointerCoords = new PointF();

        mArcRect = new RectF();

        mUsedIntervals = new ArrayList<>();
        mIntervalMasks = new ArrayList<>();

        mMinuteCursor = getContext().getResources().getDrawable(R.drawable.interval_picker_cursor);
        mHourCursor = getContext().getResources().getDrawable(R.drawable.interval_picker_cursor);

        mAreaSeparator = getResources().getDimensionPixelSize(R.dimen.picker_outer_margin);
        initPaints();

        setTime(mHour);
    }

    public void setOnTimeChangedListener(OnTimeChangedListener listener){
        mListener = listener;
    }

    private void initPaints() {
        mMinuteBgPaint = new Paint();
        mMinuteBgPaint.setColor(getContext().getResources().getColor(R.color.picker_background));
        mMinuteBgPaint.setAntiAlias(true);

        mHourBgPaint = new Paint();
        mHourBgPaint.setColor(getContext().getResources().getColor(R.color.picker_background_light));
        mHourBgPaint.setAntiAlias(true);

        mMinuteTextPaint = new Paint();
        if (!isInEditMode()) {
            mMinuteTextPaint.setTypeface(Fonts.getTypeface(getContext(), Fonts.STYLE_LIGHT | Fonts.STYLE_CONDENSED));
        }
        mMinuteTextPaint.setAntiAlias(true);
        mMinuteTextPaint.setTextSize(getContext().getResources()
                .getDimensionPixelSize(R.dimen.picker_text_size));
        mMinuteTextPaint.setColor(getContext().getResources().getColor(R.color.picker_text));

        mHourTextPaint = new Paint();
        if (!isInEditMode()) {
            mHourTextPaint.setTypeface(Fonts.getTypeface(getContext(), Fonts.STYLE_CONDENSED));
        }
        mHourTextPaint.setAntiAlias(true);
        mHourTextPaint.setTextSize(getContext().getResources()
                .getDimensionPixelSize(R.dimen.picker_text_size));
        mHourTextPaint.setColor(getContext().getResources().getColor(R.color.picker_text));

        mAmPmTextPaint = new Paint();
        if (!isInEditMode()) {
            mHourTextPaint.setTypeface(Fonts.getTypeface(getContext(), Fonts.STYLE_REGULAR));
        }
        mAmPmTextPaint.setAntiAlias(true);
        mAmPmTextPaint.setTextSize(getContext().getResources()
                .getDimensionPixelSize(R.dimen.picker_text_size));
        mAmPmTextPaint.setColor(getContext().getResources().getColor(R.color.picker_text));

        mHourPointerPaint = new Paint();
        mHourPointerPaint.setAntiAlias(true);
        mHourPointerPaint.setColor(getContext().getResources().getColor(R.color.picker_cursor_accent));
        mHourPointerPaint.setStyle(Paint.Style.STROKE);
        mHourPointerPaint.setStrokeWidth(4);

        mMinutePointerPaint = new Paint();
        mMinutePointerPaint.setAntiAlias(true);
        mMinutePointerPaint.setColor(getContext().getResources().getColor(R.color.picker_cursor_accent));
        mMinutePointerPaint.setStyle(Paint.Style.STROKE);
        mMinutePointerPaint.setStrokeWidth(4);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);

        mArcBorderPaint = new Paint();
        mArcBorderPaint.setAntiAlias(true);
        mArcBorderPaint.setStyle(Paint.Style.STROKE);
        mArcBorderPaint.setStrokeWidth(2);

        mPath = new Path();
    }

    public void enableActiveInterval(Hour24 startHour, int color) {
        Hour24 newHour = new Hour24(startHour);
        newHour.add(5);

        mActiveInterval = new Interval(startHour, newHour, color);
        mMaxHour.set(Hour24.Hour2400());
        for(Interval i : mUsedIntervals){
            if(i.getStartHour().compare(startHour) > 0){
                mMaxHour.set(i.getStartHour());
                break;
            }
        }
        setTime(newHour);
    }

    public void addUsedInterval(Interval interval){
        boolean added = false;
        for (int i = 0; i < mUsedIntervals.size(); i++) {
            if (mUsedIntervals.get(i).getStartHour().compare(interval.getStartHour()) > 0) {
                added = true;
                mUsedIntervals.add(i, interval);
                break;
            }
        }
        if (!added) mUsedIntervals.add(interval);

        updateIntervalMasks();
        setTime(getTime()); //Reset the time to avoid overlap with the newly added interval
        if(isFull(mQuadrant)){
            toggleQuadrant();
        }
    }

    private void updateIntervalMasks() {
        mIntervalMasks.clear();
        if(mUsedIntervals.size() > 0){
            List<Interval> masks = new ArrayList<>();
            Hour24 startHour = mUsedIntervals.get(0).getStartHour();
            for(int i = 0; i < mUsedIntervals.size(); i++) {
                Interval current = mUsedIntervals.get(i);
                Interval next = i == mUsedIntervals.size() - 1 ? null : mUsedIntervals.get(i + 1);
                if(startHour == null){
                    startHour = current.getStartHour();
                }
                if(next == null || !current.getEndHour().equals(next.getStartHour())){
                    masks.add(new Interval(startHour, current.getEndHour()));
                    startHour = null;
                }
            }

            for(Interval mask : masks){
                  if(mask.getStartHour().compare(mQuadrantInterval.getEndHour()) > 0){
                      break;
                  } else if(mask.getEndHour().compare(mQuadrantInterval.getStartHour()) >= 0){
                      Hour24 maskStart = new Hour24(mask.getStartHour()
                              .compare(mQuadrantInterval.getStartHour()) <= 0
                              ? mQuadrantInterval.getStartHour() : mask.getStartHour());
                      Hour24 maskEnd = new Hour24(mask.getEndHour()
                              .compare(mQuadrantInterval.getEndHour()) >= 0
                              ? mQuadrantInterval.getEndHour() : mask.getEndHour());
                      mIntervalMasks.add(new Interval(maskStart, maskEnd));
                  }
            }
        }
    }

    public boolean setTime(Hour24 hour){
        if(!mCursorsEnabled){
            return false;
        }
        if(!mQuadrantInterval.contains(hour)){
            toggleQuadrant();
        }

        if(hour.compare(mMaxHour) > 0){
            hour.set(mMaxHour);
        }
        if(mActiveInterval == null){
            for(Interval interval : mIntervalMasks){
                if(interval.contains(hour)){
                    if(interval.getStartHour().equals(mQuadrantInterval.getStartHour())){
                        hour.set(interval.getEndHour());
                    }else if(interval.getEndHour().equals(mQuadrantInterval.getEndHour())) {
                        hour.set(interval.getStartHour());
                        hour.subtract(5);
                    }else {
                        Hour24 end = interval.getNearestEnd(hour);
                        hour.set(end);
                        if(end.equals(interval.getStartHour())){
                            hour.subtract(5);
                            if(mDirection == DIR_CCW){
                                mDirection = DIR_CW;
                            }
                        }else if(end.equals(interval.getEndHour()) && mDirection == DIR_CW){
                            mDirection = DIR_CCW;
                        }
                    }
                    break;
                }
            }
        }else{
            if(hour.compare(mActiveInterval.getStartHour()) <= 0){
                hour.set(mActiveInterval.getStartHour());
                hour.add(5);
            }
        }

        mLastHour.set(mHour);
        mHour.set(hour);
        if(mActiveInterval != null){
            mActiveInterval.getEndHour().set(mHour);
        }

        if(mListener != null){
            mListener.onTimeChanged(getTime());
        }

        moveHourCursor((float)(HOUR_CURSOR_STEP * (mHour.getMinutes() / 5)));
        moveMinuteCursor((float)(MINUTE_STEP * (mHour.getMinute() / 5)));

        invalidate();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        MeasureSpec.getMode(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mCenter.set(getMeasuredWidth() / 2, getMeasuredHeight() / 2);

        mMinuteOuterRadius = (getMeasuredWidth() < getMeasuredHeight()
                ? getMeasuredWidth() / 2 : getMeasuredHeight() / 2);
        mMinuteInnerRadius = mMinuteOuterRadius - mTouchableAreaWidth
                - (mAreaSeparator * 2);
        mHourInnerRadius = mMinuteInnerRadius - mTouchableAreaWidth
                - (mAreaSeparator * 2);

        mArcRect.set(mCenter.x - mMinuteInnerRadius, mCenter.y - mMinuteInnerRadius,
                mCenter.x + mMinuteInnerRadius, mCenter.y + mMinuteInnerRadius);

        mAmPmRadius = Math.max(mHourInnerRadius - (mTouchableAreaWidth / 2),
                mTouchableAreaWidth / 2);

        int dashSize = mMinuteInnerRadius / 30;
        mDashEffect = new DashPathEffect(new float[]{dashSize, dashSize}, 0);
        calculateNumberPositions();

        setTime(mHour);
        invalidate();
    }

    private void calculateNumberPositions() {
        //Minutes
        mMinuteTextCoords.clear();
        float stepDegrees = 360f * 5 / 60;
        for (int i = 0; i < 12; i++) {
            PointF point = getCirclePointDegrees((int) ((mMinuteInnerRadius + mMinuteOuterRadius) / 2f),
                    stepDegrees * i);
            point.x = point.x - (int) (mMinuteTextPaint.measureText(Integer.toString(i * 5)) / 2f);
            point.y = point.y + (int) (mMinuteTextPaint.getTextSize() * 0.35f);
            mMinuteTextCoords.add(point);
        }
        //Hours
        mHourTextCoords.clear();
        stepDegrees = 360f / 12;
        for (int i = 1; i <= 12; i++) {
            PointF point = getCirclePointDegrees((int) ((mHourInnerRadius + mMinuteInnerRadius) / 2f),
                    stepDegrees * i);
            point.x = point.x - (int) (mHourTextPaint.measureText(Integer.toString(i)) / 2);
            point.y = point.y + (int) (mHourTextPaint.getTextSize() * 0.35f);
            mHourTextCoords.add(point);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float distance = getDistance(mCenter.x, mCenter.y, x, y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (distance < mAmPmRadius) {
                    //TODO: Ripple!
                } else if(mCursorsEnabled){
                    mActiveCursor = getTouchedArea(distance);
                    if (mActiveCursor != NONE) {
                        onTouch(x, y, event.getAction());
                        if (mActiveCursor == HOUR) {
                            mHourCursor.setState(new int[]{android.R.attr.state_pressed});
                        } else {
                            mMinuteCursor.setState(new int[]{android.R.attr.state_pressed});
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mActiveCursor != NONE && mCursorsEnabled) {
                    onTouch(x, y, event.getAction());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (distance < mAmPmRadius) {
                    toggleQuadrant();
                }else if (mActiveCursor != NONE && mCursorsEnabled) {
                    onTouch(x, y, event.getAction());
                    mActiveCursor = NONE;
                    mMinuteCursor.setState(new int[]{});
                    mHourCursor.setState(new int[]{});
                }
                mDirection = DIR_NONE;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mMinuteCursor.setState(new int[]{});
                mHourCursor.setState(new int[]{});
                mDirection = DIR_NONE;
                return true;
        }
        return false;
    }

    private int getTouchedArea(float distance) {
        if (distance > mMinuteInnerRadius && distance <= mMinuteOuterRadius) {
            return MINUTE;
        } else if (distance >= mHourInnerRadius && distance <= mMinuteInnerRadius) {
            return HOUR;
        } else {
            return NONE;
        }
    }

    private float getRadians(float x, float y){
        float r = (float)Math.atan2(x - mCenter.x, mCenter.y - y);
        if(r < 0){
            r += Math.PI * 2;
        }
        return r;
    }

    private double getRadians(Hour24 hour) {
        int minutes = hour.getMinutes() % 720;
        if((mQuadrant == AM && hour.equals(Hour24.Hour1200()))
                || (mQuadrant == PM && hour.equals(Hour24.Hour2400()))){
            minutes = 720;
        }
        return HOUR_CURSOR_STEP * Math.round(minutes / 5f);
    }

    private double getRadians(int minute) {
        return MINUTE_STEP * ((minute % 60) / 5);
    }

    private double getDegrees(Hour24 hour) {
        return (float)Math.toDegrees(getRadians(hour));
    }

    private void onTouch(float x, float y, int action) {
        double hourRadians;
        double touchRadians = getRadians(x, y);
        if(mActiveCursor == MINUTE){
            touchRadians = Math.round(touchRadians / MINUTE_STEP) * MINUTE_STEP;
        }else{
            touchRadians = Math.round(touchRadians / HOUR_CURSOR_STEP) * HOUR_CURSOR_STEP;
        }

        touchRadians = touchRadians % (Math.PI * 2);

        int touchDirection;
        switch (action){
            case MotionEvent.ACTION_MOVE:
                if(touchRadians < mLastTouchRadians){
                    if(touchRadians < Math.PI / 20 && mLastTouchRadians > 39/20 * Math.PI){
                        touchDirection = DIR_CW;
                    }else {
                        touchDirection = DIR_CCW;
                    }
                }else if(touchRadians > mLastTouchRadians){
                    if(touchRadians > 39/20 * Math.PI && mLastTouchRadians < Math.PI / 20){
                        touchDirection = DIR_CCW;
                    }else {
                        touchDirection = DIR_CW;
                    }
                }else {
                    touchDirection = DIR_NONE;
                }
                break;
            default:
                touchDirection = DIR_NONE;
        }

        if(mActiveCursor == MINUTE){
            int lastMinute = getTime().getMinute();
            int newMinute = getMinute(touchRadians);
            int diff = 0;
            if(touchDirection == DIR_CW){
                if(newMinute < lastMinute){
                    diff = 60 - lastMinute + newMinute;
                }else {
                    diff = newMinute - lastMinute;
                }
            }else if(touchDirection == DIR_CCW){
                if(newMinute > lastMinute){
                    diff = newMinute - lastMinute - 60;
                }else {
                    diff = newMinute - lastMinute;
                }
            }
            diff = diff / 5;
            hourRadians = getRadians(getTime()) + HOUR_CURSOR_STEP * diff;
            hourRadians = (Math.round(hourRadians / HOUR_CURSOR_STEP) * HOUR_CURSOR_STEP) % (Math.PI * 2);
            if(hourRadians < 0){
                hourRadians += Math.PI * 2;
            }
        }else {
            hourRadians = touchRadians;
        }

        mLastTouchRadians = touchRadians;

        double lastHourRadians = getRadians(getTime());

        if(mDirection == DIR_CW){
            if(touchDirection == DIR_CCW && hourRadians < lastHourRadians
                    && hourRadians > (lastHourRadians - 12 * HOUR_CURSOR_STEP)){
                mDirection = DIR_CCW;
            }
        }else if(mDirection == DIR_CCW){
            if(touchDirection == DIR_CW && hourRadians > lastHourRadians
                    && hourRadians < (lastHourRadians + 12 * HOUR_CURSOR_STEP)){
                mDirection = DIR_CW;
            }
        }else {
            mDirection = touchDirection;
        }

        if(mDirection == DIR_CW && hourRadians < lastHourRadians){
            hourRadians = Math.PI * 2;
        }else if(mDirection == DIR_CCW && hourRadians > lastHourRadians){
            hourRadians = 0;
        }

        Hour24 hour = getTime(hourRadians);
        if(!hour.equals(getTime())){
            //vibrate();
            setTime(hour);
        }
    }

    private void moveHourCursor(float radians) {
        mHourPointerCoords.set(
                getCirclePointRadians(mHourInnerRadius + mAreaSeparator, radians));
        PointF ch = getCirclePointRadians((mHourInnerRadius + mMinuteInnerRadius) / 2,
                radians);
        mHourCursorBounds.set((int) (ch.x - mCursorRadius), (int) (ch.y - mCursorRadius),
                (int) (ch.x + mCursorRadius), (int) (ch.y + mCursorRadius));
    }

    private void moveMinuteCursor(float radians) {
        mMinutePointerCoords.set(
                getCirclePointRadians(mMinuteInnerRadius + mAreaSeparator, radians));
        PointF cm = getCirclePointRadians((mMinuteInnerRadius + mMinuteOuterRadius) / 2,
                radians);
        mMinuteCursorBounds.set((int) (cm.x - mCursorRadius), (int) (cm.y - mCursorRadius),
                (int) (cm.x + mCursorRadius), (int) (cm.y + mCursorRadius));
    }

    private Hour24 getTime(double radians) {
        int minutes = Math.round(Math.round(radians / HOUR_CURSOR_STEP) * 5);
        if(minutes != 720){
            minutes = minutes % 720;
        }
        minutes += (mQuadrant == PM ? 12 * 60 : 0);
        return new Hour24(minutes);
    }

    private int getMinute(double radians) {
        radians = radians < 0 ? radians + 2 * Math.PI : radians;
        int min = (int)(Math.round(radians / MINUTE_STEP) * 5) % 60;
        if(min < 0){
            min += 60;
        }
        return min;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenter.x, mCenter.y, mMinuteOuterRadius, mMinuteBgPaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mMinuteInnerRadius, mHourBgPaint);
        drawUsedIntervals(canvas);

        if (mActiveInterval != null) {
            drawInterval(canvas, mActiveInterval);
        }

        drawNumbers(canvas);
        drawCursors(canvas);

        drawAmPmSwitch(canvas);
    }

    private void drawNumbers(Canvas canvas) {
        for (int i = 0; i < 12; i++) {
            canvas.drawText(Integer.toString(i * 5), mMinuteTextCoords.get(i).x,
                    mMinuteTextCoords.get(i).y, mMinuteTextPaint);
            canvas.drawText(Integer.toString(i + 1), mHourTextCoords.get(i).x,
                    mHourTextCoords.get(i).y, mHourTextPaint);
        }
    }

    private void drawCursors(Canvas canvas) {
        if(mCursorsEnabled){
            canvas.drawLine(mCenter.x, mCenter.y, mMinutePointerCoords.x, mMinutePointerCoords.y,
                    mMinutePointerPaint);
            canvas.drawLine(mCenter.x, mCenter.y, mHourPointerCoords.x, mHourPointerCoords.y,
                    mHourPointerPaint);

            mMinuteCursor.setBounds(mMinuteCursorBounds);
            mMinuteCursor.draw(canvas);

            mHourCursor.setBounds(mHourCursorBounds);
            mHourCursor.draw(canvas);
        }
    }

    private void drawUsedIntervals(Canvas canvas) {
       for(Interval i : mUsedIntervals){
           drawInterval(canvas, i);
       }
    }

    private void drawInterval(Canvas canvas, Interval interval) {
        int cut = 0; //0: no cut 1: cut start 2: cut end
        Hour24 min = mQuadrant == AM ? Hour24.Hour0000() : Hour24.Hour1200();
        Hour24 max = mQuadrant == AM ? Hour24.Hour1200() : Hour24.Hour2400();
        Hour24 start, end;
        if(interval.getStartHour().compare(min) >= 0){
            if(interval.getStartHour().compare(max) > 0){
                return;
            }
            start = new Hour24(interval.getStartHour());
            if(interval.getEndHour().compare(max) > 0){
                end = new Hour24(max);
                cut = 2;
            }else{
                end = new Hour24(interval.getEndHour());
            }
        }else{
            if(interval.getEndHour().compare(min) < 0){
                return;
            }else{
                start = new Hour24(min);
                end = new Hour24(interval.getEndHour());
                cut = 1;
            }
        }

        mArcPaint.setColor(interval.getColor());
        mArcBorderPaint.setColor(interval.getColor());
        mArcPaint.setAlpha(150);
        mArcBorderPaint.setAlpha(220);

        double startAngle = getDegrees(start);
        canvas.drawArc(mArcRect, (float)(startAngle - 90), (float)(getDegrees(end) - startAngle), true, mArcPaint); //Fill the arc

        PointF c;
        c = getCirclePointDegrees(mMinuteInnerRadius, getDegrees(start));
        drawLine(canvas, mCenter.x, mCenter.y, c.x, c.y, cut == 1, mArcBorderPaint);
        c = getCirclePointDegrees(mMinuteInnerRadius, getDegrees(end));
        drawLine(canvas, mCenter.x, mCenter.y, c.x, c.y, cut == 2, mArcBorderPaint);
    }

    private void drawLine(Canvas canvas, float startX, float startY, float endX, float endY,
                          boolean dash, Paint paint){
        if(dash){
            paint.setPathEffect(mDashEffect);
        }else{
            paint.setPathEffect(null);
        }
        mPath.reset();
        mPath.moveTo(startX, startY);
        mPath.lineTo(endX, endY);
        canvas.drawPath(mPath, paint);
    }


    private void drawAmPmSwitch(Canvas canvas) {
        canvas.drawCircle(mCenter.x, mCenter.y, mAmPmRadius, mMinuteBgPaint);
        String text;
       /* if(mActiveArea != null){
            
        }else{*/
            text = mQuadrant == PM ? "PM" : "AM";
        //}
        int x = mCenter.x - (int) (mAmPmTextPaint.measureText(text) / 2f);
        int y = mCenter.y + (int) (mAmPmTextPaint.getTextSize() * 0.35f);
        canvas.drawText(text, x, y, mAmPmTextPaint);
    }

    private PointF getCirclePointDegrees(int radius, double degrees) {
        return getCirclePointRadians(radius, (float)Math.toRadians(degrees));
    }

    private PointF getCirclePointRadians(int radius, float radians) {
        radians = radians - (float) Math.PI / 2;
        return new PointF((float) (mCenter.x + radius * Math.cos(radians)),
                (float) (mCenter.y + radius * Math.sin(radians)));
    }

    private void vibrate() {
        if (System.currentTimeMillis() - mLastVibration > mVibrationStep) {
            mLastVibration = System.currentTimeMillis();
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void setQuadrant(int quadrant){
        if(mQuadrant == quadrant) return;

        mQuadrant = quadrant;
        if(mQuadrant == AM){
            mQuadrantInterval = new Interval(Hour24.Hour0000(), Hour24.Hour1200());
        } else {
            mQuadrantInterval = new Interval(Hour24.Hour1200(), Hour24.Hour2400());
        }
        mCursorsEnabled = !isFull(mQuadrant);
        if(mCursorsEnabled && !mQuadrantInterval.contains(getTime())) {
            Hour24 hour;
            switch (getTime().getMinutes()) {
                case 0:
                case 12 * 60:
                    hour = Hour24.Hour1200();
                    break;
                case 24 * 60:
                    hour = Hour24.Hour0000();
                    break;
                default:
                    hour = new Hour24(mHour.getMinutes() + 12 * 60);
                    break;
            }
            updateIntervalMasks();
            setTime(hour);
        }else{
            invalidate();
        }
    }
    private void toggleQuadrant() {
        if (mQuadrant == AM) {
            setQuadrant(PM);
        } else {
            setQuadrant(AM);
        }
    }

    private boolean isFull(int quadrant){
        if(mUsedIntervals.size() == 0){
            return false;
        }
        if(quadrant == AM){
            if(mUsedIntervals.get(0).getStartHour().equals(Hour24.Hour0000())){
                if(mUsedIntervals.size() == 1) {
                    return mUsedIntervals.get(0).getEndHour().compare(Hour24.Hour1200()) >= 0;
                }
                for(int i = 1; i < mUsedIntervals.size(); i++){
                    if(!mUsedIntervals.get(i).getStartHour()
                            .equals(mUsedIntervals.get(i-1).getEndHour())){
                        return false;
                    }
                    if(mUsedIntervals.get(i).getEndHour().compare(Hour24.Hour1200()) >= 0){
                        break;
                    }
                }
                return true;
            }
        }else{
            if(mUsedIntervals.get(mUsedIntervals.size() - 1)
                    .getEndHour().equals(Hour24.Hour2400())){
                for(int i = 0; i < mUsedIntervals.size(); i++){
                    if(mUsedIntervals.get(i).getStartHour().compare(Hour24.Hour1200()) > 0){
                        if(i > 0 && !mUsedIntervals.get(i - 1).getEndHour()
                                .equals(mUsedIntervals.get(i).getStartHour())){
                            return false;
                        }else if(i == 0){
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }


    public static class Interval {
        private Hour24 mStartHour, mEndHour;
        private int mColor = Color.BLACK;

        public Interval(Hour24 startHour, Hour24 endHour){
            mStartHour = new Hour24(startHour);
            mEndHour = new Hour24(endHour);
        }

        public Interval(Hour24 startHour, Hour24 endHour, int color){
            mStartHour = new Hour24(startHour);
            mEndHour = new Hour24(endHour);
            mColor = color;
        }

        public Hour24 getStartHour() {
            return mStartHour;
        }

        public void setStartHour(Hour24 startHour) {
            this.mStartHour = startHour;
        }

        public Hour24 getEndHour() {
            return mEndHour;
        }

        public void setEndHour(Hour24 endHour) {
            this.mEndHour = endHour;
        }

        public int getColor() {
            return mColor;
        }

        public void setColor(int color) {
            this.mColor = color;
        }

        public boolean contains(Hour24 hour){
            return hour.compare(mStartHour) >= 0 && hour.compare(mEndHour) <= 0;
        }

        public Hour24 getNearestEnd(Hour24 hour){
            return Math.abs(getStartHour().compare(hour)) < Math.abs(getEndHour().compare(hour))
                    ? getStartHour() : getEndHour();
        }
    }

    public interface OnTimeChangedListener{
        public void onTimeChanged(Hour24 time);
    }
}
