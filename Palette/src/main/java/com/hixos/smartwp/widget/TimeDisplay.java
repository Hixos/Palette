package com.hixos.smartwp.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Hour24;

/**
 * Created by Luca on 25/03/2015.
 */
public class TimeDisplay extends LinearLayout {
    private TextView mTimeText, mPeriodText;
    private boolean m24HourMode = false;

    public TimeDisplay(Context context) {
        super(context);
        initialize(context, null);
    }

    public TimeDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public TimeDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs){
        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.layout_time_display, this);
        mTimeText = (TextView)findViewById(R.id.textViewTime);
        mPeriodText = (TextView)findViewById(R.id.textViewPeriod);

        if(attrs != null){
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.TimeDisplay,
                    0, 0);
            setColor(a.getColor(R.styleable.TimeDisplay_textColor, Color.WHITE));
            mTimeText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    a.getDimensionPixelSize(R.styleable.TimeDisplay_textTimeSize,
                    getResources().getDimensionPixelSize(R.dimen.text_large)));
            mPeriodText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    a.getDimensionPixelSize(R.styleable.TimeDisplay_textPeriodSize,
                            getResources().getDimensionPixelSize(R.dimen.text_small)));
            a.recycle();
        }

        if(DateFormat.is24HourFormat(getContext())){
            mPeriodText.setVisibility(GONE);
        }
        set24HourMode(DateFormat.is24HourFormat(context));
        setTime(Hour24.Hour1200());
    }

    public void setColor(int color){
        mTimeText.setTextColor(color);
        mPeriodText.setTextColor(color);
    }

    public void setTime(Hour24 hour24){
        if(m24HourMode){
            mTimeText.setText(String.format("%02d",hour24.getHour()) + ":" + String.format("%02d", hour24.getMinute()));
        }else{
            int hour = hour24.getHour();
            String period;
            if(hour == 24){
                period = "am";
                hour = 0;
            }else if(hour > 12){
                hour -= 12;
                period = "pm";
            }else{
                period = "am";
            }
            mTimeText.setText(String.format("%02d",hour) + ":" + String.format("%02d", hour24.getMinute()));
            mPeriodText.setText(period);
        }
    }

    public void set24HourMode(boolean is24HourMode){
        m24HourMode = is24HourMode;
        if(is24HourMode){
            mPeriodText.setVisibility(GONE);
        }else{
            mPeriodText.setVisibility(VISIBLE);
        }
    }
}
