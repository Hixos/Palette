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

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;

import java.util.ArrayList;
import java.util.List;

public class TodPickerView extends View {

    public static final int AM = 0x01;
    public static final int PM = 0x10;

    private static final int NONE = 0;
    //Cursors
    private int mActiveCursor = NONE;
    private static final int HOUR = 1;
    private static final int MINUTE = 2;
    private static final float HOUR_STEP = 0.5236f; // Math.toRadians(360 / 12)
    private static final float HOUR_PREC_STEP = 0.00873f; // Math.toRadians(360 / (12 * 60))
    private static final float MINUTE_STEP = 0.10472f; // Math.toRadians(360 / 60)
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

    private Hour mHour;

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
    private List<ClockArea> mUsedAreas;
    private List<ClockArea> mUsedAreasMask;

    //Active area
    private ClockArea mActiveArea;
    private Hour mMaxHour = null;

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

    private static float hourToRadians(Hour hour) {
        return HOUR_PREC_STEP * (hour.getHour() * 60 + hour.getMinute());
    }

    private static float minuteToRadians(int minute) {
        return MINUTE_STEP * minute;
    }

    private static float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public Hour getHour() {
        return mHour;
    }

    private void initView() {
        mHour = new Hour();

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

        mUsedAreas = new ArrayList<>();
        mUsedAreasMask = new ArrayList<>();

        mMinuteCursor = getContext().getResources().getDrawable(R.drawable.interval_picker_cursor);
        mHourCursor = getContext().getResources().getDrawable(R.drawable.interval_picker_cursor);

        mAreaSeparator = getResources().getDimensionPixelSize(R.dimen.picker_outer_margin);
        initPaints();

        setTime(new Hour(0, 0, AM));
        invalidate();
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

    private void enableActiveArea(Hour startHour) {
        mActiveArea = new ClockArea(startHour, startHour, Color.GREEN);
        mHour.setPeriod(startHour.getPeriod());
        mMaxHour = new Hour(11, 59, PM);
        for (ClockArea a : mUsedAreas) {
            if (a.getStartHour().compare(mActiveArea.getStartHour()) == 1) {
                mMaxHour.set(a.getStartHour());
                break;
            }
        }
    }

    public void addUsedArea(ClockArea area) {
        boolean added = false;
        for (int i = 0; i <= mUsedAreas.size(); i++) {
            if (mUsedAreas.get(i).getStartHour().compare(area.getStartHour()) == 1) {
                added = true;
                mUsedAreas.add(i, area);
                break;
            }
        }
        if (!added) mUsedAreas.add(area);

        createUsedAreaMask();
    }

    private void createUsedAreaMask() {
        mUsedAreasMask.clear();

        ClockArea area = null;
        for (int i = 0; i < mUsedAreas.size(); i++) {
            if (i == 0 || !mUsedAreas.get(i - 1).isAdjacent(mUsedAreas.get(i))) {
                if (area != null) {
                    mUsedAreasMask.add(area);
                }
                area = new ClockArea(mUsedAreas.get(i).getStartHour(),
                        mUsedAreas.get(i).getEndHour(), mUsedAreas.get(i).getColor());
            } else {
                area.setEndHour(mUsedAreas.get(i).getEndHour());
            }
        }
        if (area != null) {
            mUsedAreasMask.add(area);
        }
    }

    public void setTime(Hour hour) {
        if (mActiveArea != null) {
            if (mMaxHour != null && hour.compare(mMaxHour) == 1) {
                hour = mMaxHour;
            }
            if (hour.compare(mActiveArea.getStartHour()) < 1) {
                int m = mActiveArea.getStartHour().getMinute();
                int h = mActiveArea.getStartHour().getHour() + (m == 59 ? 1 : 0);
                hour = new Hour(h, m + 1, mActiveArea.getStartHour().getPeriod());
            }
        } else {
            for (ClockArea a : mUsedAreasMask) {
                ClockArea section = a.getPeriodSection(mHour.getPeriod());
                if (section != null && section.contains(hour)) {
                    if (hour.compare(section.getMiddle()) == 1) {
                        hour = section.getEndHour();
                        hour.setPeriod(getHour().getPeriod()); //Prevent inadvertently changing period
                    } else {
                        hour = section.getStartHour();
                    }
                    break;
                }
            }
        }
        mHour.set(hour);

        mActiveArea.setEndHour(mHour);

        int totMinute = mHour.getHour() * 60 + mHour.getMinute();
        float hourRadians = totMinute * HOUR_PREC_STEP;
        float minuteRadians = mHour.getMinute() * MINUTE_STEP;
        moveHourCursor(hourRadians);
        moveMinuteCursor(minuteRadians);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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
                } else {
                    mActiveCursor = getTouchedArea(distance);
                    if (mActiveCursor != NONE) {
                        onTouch(x, y);
                        if (mActiveCursor == HOUR) {
                            mHourCursor.setState(new int[]{android.R.attr.state_pressed});
                        } else {
                            mMinuteCursor.setState(new int[]{android.R.attr.state_pressed});
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mActiveCursor != NONE) {
                    onTouch(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mActiveCursor != NONE) {
                    onTouch(x, y);
                    mActiveCursor = NONE;
                    mMinuteCursor.setState(new int[]{});
                    mHourCursor.setState(new int[]{});
                } else if (distance < mAmPmRadius) {
                    toggleAmPm();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                mMinuteCursor.setState(new int[]{});
                mHourCursor.setState(new int[]{});
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

    private void onTouch(float x, float y) {
        float radians = (float) Math.atan2(x - mCenter.x, mCenter.y - y); //Get angle on the clock

        switch (mActiveCursor) {
            case HOUR: {
                Hour hour = radiansToHour(radians);
                if (mHour.getHour() != hour.getHour() || mHour.getMinute() != hour.getMinute()) {
                    if (mHour.getHour() != hour.getHour()) {
                       /* if (((mHour == 1 || mHour == 12) && (hour == 10 || hour == 11)
                                && mQuadrant == PM)
                                || ((mHour == 10 || mHour == 11) && (hour == 1 || hour == 12)
                                && mQuadrant == AM)){
                            toggleAmPm();
                        }*/
                        vibrate();
                    }
                    setTime(hour);
                }
                break;
            }
            case MINUTE: {
                Hour hour = new Hour(mHour.getHour(), radiansToPrecMinute(radians),
                        mHour.getPeriod());
                if (mHour.getMinute() != hour.getMinute()) {
                    if (mHour.getMinute() < 15 && hour.getMinute() > 45) {
                       /* if (mHour - 1 == 11 && mQuadrant == PM) {
                            toggleAmPm();
                        }*/
                        hour.setHour(mHour.getHour() - 1);
                    } else if (mHour.getMinute() > 45 && hour.getMinute() < 15) {
                        /*if (mHour + 1 == 12 && mQuadrant == AM) {
                            toggleAmPm();
                        }*/
                        hour.setHour(mHour.getHour() + 1);
                    }
                    setTime(hour);
                    vibrate();
                }
                break;
            }
        }
        invalidate();
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

    private Hour radiansToHour(double radians) {
        radians = radians < 0 ? radians + 2 * Math.PI : radians;
        int hour = (int) Math.floor(radians / HOUR_STEP);
        return new Hour(hour, (int) (radians / HOUR_PREC_STEP) - hour * 60, mHour.getPeriod());
    }

    private int radiansToPrecMinute(float radians) { //For use with Minute pointer radians
        radians = radians < 0 ? (float) (radians + 2 * Math.PI) : radians;
        int min = Math.round(radians / MINUTE_STEP);
        return min == 60 ? 0 : min;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenter.x, mCenter.y, mMinuteOuterRadius, mMinuteBgPaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mMinuteInnerRadius, mHourBgPaint);
        drawArcs(canvas);

        if (mActiveArea != null) {
            drawClockArea(canvas, mActiveArea);
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
        canvas.drawLine(mCenter.x, mCenter.y, mMinutePointerCoords.x, mMinutePointerCoords.y,
                mMinutePointerPaint);
        canvas.drawLine(mCenter.x, mCenter.y, mHourPointerCoords.x, mHourPointerCoords.y,
                mHourPointerPaint);

        mMinuteCursor.setBounds(mMinuteCursorBounds);
        mMinuteCursor.draw(canvas);

        mHourCursor.setBounds(mHourCursorBounds);
        mHourCursor.draw(canvas);
    }

    private void drawArcs(Canvas canvas) {
        for (ClockArea area : mUsedAreas) {
            drawClockArea(canvas, area);
        }
    }

    private void drawClockArea(Canvas canvas, ClockArea area) {
        if ((area.getPeriod() & mHour.getPeriod()) == mHour.getPeriod()) {
            ClockArea section = area.getPeriodSection(mHour.getPeriod());

            mArcPaint.setColor(section.getColor());
            mArcBorderPaint.setColor(section.getColor());
            mArcPaint.setAlpha(85);
            mArcBorderPaint.setAlpha(180);

            canvas.drawArc(mArcRect, section.getStartHourAngle() - 90,
                    section.getStartEndAngle(), true, mArcPaint); //Fill the arc

            if (section.getCut() == ClockArea.CUT_START) {
                mArcBorderPaint.setPathEffect(mDashEffect);
            }

            mPath.reset();
            PointF p = getCirclePointDegrees(mMinuteInnerRadius, section.getStartHourAngle());
            mPath.moveTo(mCenter.x, mCenter.y);
            mPath.lineTo(p.x, p.y);
            canvas.drawPath(mPath, mArcBorderPaint);

            if (section.getCut() == ClockArea.CUT_END) {
                mArcBorderPaint.setPathEffect(mDashEffect);
            } else {
                mArcBorderPaint.setPathEffect(null);
            }

            mPath.reset();
            p = getCirclePointDegrees(mMinuteInnerRadius, section.getEndHourAngle());
            mPath.moveTo(mCenter.x, mCenter.y);
            mPath.lineTo(p.x, p.y);
            canvas.drawPath(mPath, mArcBorderPaint);

            mArcBorderPaint.setPathEffect(null);
        }
    }


    private void drawAmPmSwitch(Canvas canvas) {
        canvas.drawCircle(mCenter.x, mCenter.y, mAmPmRadius, mMinuteBgPaint);
        String text = mHour.getPeriod() == PM ? "PM" : "AM";
        int x = mCenter.x - (int) (mAmPmTextPaint.measureText(text) / 2f);
        int y = mCenter.y + (int) (mAmPmTextPaint.getTextSize() * 0.35f);
        canvas.drawText(text, x, y, mAmPmTextPaint);
    }

    private PointF getCirclePointDegrees(int radius, float degrees) {
        return getCirclePointRadians(radius, (float) Math.toRadians(degrees));
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

    private void toggleAmPm() {
        if (mActiveArea != null && (mActiveArea.getStartHour().getPeriod() == PM
                || mMaxHour.getPeriod() == AM)) {
            return;
        }
        if (mHour.getPeriod() == AM) {
            mHour.setPeriod(PM);
        } else {
            mHour.setPeriod(AM);
        }
        setTime(mHour);
        if (mActiveArea != null)
            mActiveArea.getEndHour().setPeriod(mHour.getPeriod());
        invalidate();
    }

    public class Hour {
        private int mHour;
        private int mMinute;
        private int mPeriod;

        public Hour() {
            mHour = 0;
            mMinute = 0;
            mPeriod = AM;
        }

        public Hour(int hour, int minute, int period) {
            mHour = hour % 12;
            mMinute = minute % 60;
            mPeriod = period;
        }

        public void log(String tag, String info) {
            Logger.w(tag, info + " %d:%d %s", getHour(),
                    getMinute(), getPeriod() == AM ? "AM" : "PM");
        }

        public int getHour() {
            return mHour;
        }

        public void setHour(int hour) {
            this.mHour = hour % 12;
        }

        public int getRealHour() {
            return mHour == 0 ? 12 : mHour;
        }

        public int getMinute() {
            return mMinute;
        }

        public void setMinute(int minute) {
            this.mMinute = minute % 60;
        }

        public int getPeriod() {
            return mPeriod;
        }

        public void setPeriod(int period) {
            this.mPeriod = period;
        }

        public int compare(Hour hour) {
            int t1 = (getHour() + (getPeriod() == PM ? 12 : 0)) * 60 + getMinute();
            int t2 = (hour.getHour() + (hour.getPeriod() == PM ? 12 : 0)) * 60 + hour.getMinute();

            if (t1 - t2 == 0) {
                return 0;
            } else if (t1 - t2 < 0) {
                return -1;
            } else {
                return 1;
            }
        }

        public boolean equals(Hour hour) {
            return compare(hour) == 0;
        }

        public void set(Hour hour) {
            mHour = hour.getHour();
            mMinute = hour.getMinute();
            mPeriod = hour.getPeriod();
        }
    }


    public class ClockArea {
        private static final int CUT_NONE = 0;
        private static final int CUT_START = 1;
        private static final int CUT_END = 2;
        private final int mCut;
        private Hour mStartHour;
        private Hour mEndHour;
        private int mColor;

        public ClockArea(int startHour, int startMinute, int startPeriod,
                         int endHour, int endMinute, int endPeriod, int color) {
            if (endPeriod < startPeriod) {
                throw new IllegalArgumentException("End hour period must be greater then start " +
                        "hour period. (AM < PM)");
            }
            mStartHour = new Hour(startHour, startMinute, startPeriod);
            mEndHour = new Hour(endHour, endMinute, endPeriod);
            mColor = color;
            this.mCut = CUT_NONE;
        }

        public ClockArea(Hour startHour, Hour endHour, int color) {
            if (endHour.getPeriod() < startHour.getPeriod()) {
                throw new IllegalArgumentException("End hour period must be greater then start " +
                        "hour period. (AM < PM)");
            }
            mStartHour = new Hour(startHour.getHour(), startHour.getMinute(), startHour.getPeriod());
            mEndHour = new Hour(endHour.getHour(), endHour.getMinute(), endHour.getPeriod());
            mColor = color;
            this.mCut = CUT_NONE;
        }

        private ClockArea(int startHour, int startMinute, int startPeriod,
                          int endHour, int endMinute, int endPeriod, int color, int cut) {
            if (endPeriod < startPeriod) {
                throw new IllegalArgumentException("End hour period must be greater then start " +
                        "hour period. (AM < PM)");
            }
            mStartHour = new Hour(startHour, startMinute, startPeriod);
            mEndHour = new Hour(endHour, endMinute, endPeriod);
            mColor = color;
            this.mCut = cut;
        }

        private ClockArea(Hour startHour, Hour endHour, int color, int cut) {
            if (endHour.getPeriod() < startHour.getPeriod()) {
                throw new IllegalArgumentException("End hour period must be greater then start " +
                        "hour period. (AM < PM)");
            }
            mStartHour = new Hour(startHour.getHour(), startHour.getMinute(), startHour.getPeriod());
            mEndHour = new Hour(endHour.getHour(), endHour.getMinute(), endHour.getPeriod());
            mColor = color;
            this.mCut = cut;
        }

        public Hour getStartHour() {
            return mStartHour;
        }

        public void setStartHour(Hour hour) {
            mStartHour.set(hour);
        }

        public Hour getEndHour() {
            return mEndHour;
        }

        public void setEndHour(Hour hour) {
            if (hour.getPeriod() < mStartHour.getPeriod()) {
                throw new IllegalArgumentException("End hour period must be greater then start " +
                        "hour period. (AM < PM)");
            }
            mEndHour.set(hour);
        }

        public float getStartHourAngle() {
            return (float) (Math.toDegrees(hourToRadians(mStartHour)) % 360);
        }

        public float getStartMinuteAngle() {
            return (float) Math.toDegrees(minuteToRadians(mStartHour.getMinute()));
        }

        public float getEndHourAngle() {
            return (float) (Math.toDegrees(hourToRadians(mEndHour)) % 360);
        }

        public float getEndMinuteAngle() {
            return (float) Math.toDegrees(minuteToRadians(mEndHour.getMinute()));
        }

        public float getStartEndAngle() {
            float start = getStartHourAngle();
            float end = getEndHourAngle();
            while (end < start) end += 360;
            return end - start;
        }

        public int getColor() {
            return mColor;
        }

        public ClockArea getPeriodSection(int period) {
            if (mStartHour.getPeriod() == period) {
                if (mEndHour.getPeriod() == period) {
                    return new ClockArea(mStartHour, mEndHour, mColor);
                } else {
                    return new ClockArea(mStartHour.getHour(), mStartHour.getMinute(),
                            mStartHour.getPeriod(),
                            0, 0, PM, mColor, CUT_END);
                }
            } else {
                if (mEndHour.getPeriod() == period) {
                    return new ClockArea(0, 0, PM,
                            mEndHour.getHour(), mEndHour.getMinute(), mEndHour.getPeriod(),
                            mColor, CUT_START);
                } else {
                    return null;//new ClockArea(mStartHour, mEndHour, mColor);
                }
            }
        }

        public Hour getMiddle() {
            int ts = (mStartHour.getHour() + (mStartHour.getPeriod() == PM ? 12 : 0)) * 60
                    + mStartHour.getMinute();
            int te = (mEndHour.getHour() + (mEndHour.getPeriod() == PM ? 12 : 0)) * 60
                    + mEndHour.getMinute();
            int mm = (te + ts) / 2;
            int mh = 0;
            while (mm > 59) {
                mm -= 60;
                mh++;
            }
            return new Hour(mh % 12, mm, mh > 11 ? PM : AM);
        }

        public int getPeriod() {
            return mStartHour.getPeriod() | mEndHour.getPeriod();
        }

        public int getCut() {
            return mCut;
        }

        public boolean contains(Hour hour) {
            return hour.compare(mStartHour) == 1 && hour.compare(mEndHour) == -1;
        }

        public boolean isAdjacent(ClockArea a) {
            return a.getStartHour().compare(getEndHour()) == 0
                    || getStartHour().compare(a.getEndHour()) == 0;
        }
    }
}
