package com.hixos.smartwp.triggers.timeofday;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Hour24;
import com.hixos.smartwp.widget.CheckableFrameLayout;
import com.hixos.smartwp.widget.TimeDisplay;
import com.hixos.smartwp.widget.TodPickerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 22/03/2015.
 */
public class TodPickerDialog extends DialogFragment implements View.OnClickListener,
        TodPickerView.OnTimeChangedListener{
    private CheckableFrameLayout mAmButton, mPmButton;
    private TodPickerView mStartPicker, mEndPicker;
    private TimeDisplay mStartTimeDisplay, mEndTimeDisplay;
    private TextView mMainButtonText;
    private TextView mTitleText;
    private TodPickerDialogListener mListener;
    private boolean mPickingStart = true;
    private int mColor;
    private List<TimeOfDayWallpaper> mCurrentWallpapers;

    public static TodPickerDialog getInstance(List<TimeOfDayWallpaper> currentWallpapers,
                                              int color, TodPickerDialogListener listener){
        TodPickerDialog dialog = new TodPickerDialog();
        dialog.mColor = color;
        dialog.mCurrentWallpapers = currentWallpapers;
        dialog.mListener = listener;
        return dialog;
    }

    public void setListener(TodPickerDialogListener listener){
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_DarkPalette_Material_Dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(mListener != null)
            mListener.onCancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("startMinutes", mStartPicker.getTime().getMinutes());
        outState.putInt("endMinutes", mEndPicker.getTime().getMinutes());
        outState.putInt("color", mColor);
        outState.putBoolean("quadrantAM", mAmButton.isChecked());
        outState.putBoolean("pickingStart", mPickingStart);
        outState.putParcelableArrayList("wallpapers", new ArrayList<Parcelable>(mCurrentWallpapers));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_tod_picker, null);
        mStartTimeDisplay = (TimeDisplay)view.findViewById(R.id.timedisplay_start);
        mEndTimeDisplay = (TimeDisplay)view.findViewById(R.id.timedisplay_end);
        mMainButtonText = (TextView)view.findViewById(R.id.textview_main_button);
        mTitleText = (TextView)view.findViewById(R.id.textViewTitle);
        mStartPicker = (TodPickerView)view.findViewById(R.id.todpicker_start);
        mEndPicker = (TodPickerView)view.findViewById(R.id.todpicker_end);

        mAmButton = (CheckableFrameLayout)view.findViewById(R.id.toggle_am);
        mPmButton = (CheckableFrameLayout)view.findViewById(R.id.toggle_pm);

        view.findViewById(R.id.framelayout_main_button).setOnClickListener(this);
        mAmButton.setOnClickListener(this);
        mPmButton.setOnClickListener(this);
        mStartTimeDisplay.setOnClickListener(this);
        mEndTimeDisplay.setOnClickListener(this);

        mStartPicker.setOnTimeChangedListener(this);
        mEndPicker.setOnTimeChangedListener(this);

        mAmButton.setChecked(true);

        if(savedInstanceState != null) {
            mPickingStart = savedInstanceState.getBoolean("pickingStart");
            if(!mPickingStart){
                showEndPicker(false);
            }

            mCurrentWallpapers = savedInstanceState.getParcelableArrayList("wallpapers");
            addCurrentWallpapers();

            Hour24 start = new Hour24(savedInstanceState.getInt("startMinutes"));
            Hour24 end = new Hour24(savedInstanceState.getInt("endMinutes"));
            mColor = savedInstanceState.getInt("color");

            mStartPicker.forceSetTime(start);
            mEndPicker.enableActiveInterval(start, mColor);
            mEndPicker.forceSetTime(end);

            if(savedInstanceState.getBoolean("quadrantAM")){
                mAmButton.setChecked(true);
                mPmButton.setChecked(false);
                if(mPickingStart){
                    mStartPicker.setQuadrant(TodPickerView.AM);
                }else{
                    mEndPicker.setQuadrant(TodPickerView.AM);
                }
            }else{
                mPmButton.setChecked(true);
                mAmButton.setChecked(false);
                if(mPickingStart){
                    mStartPicker.setQuadrant(TodPickerView.PM);
                }else{
                    mEndPicker.setQuadrant(TodPickerView.PM);
                }
            }
        }else{
            addCurrentWallpapers();
        }

        mAmButton.setChecked(mStartPicker.getQuadrant() == TodPickerView.AM);
        mPmButton.setChecked(mStartPicker.getQuadrant() == TodPickerView.PM);

        return view;
    }

    private void addCurrentWallpapers(){
        for (TimeOfDayWallpaper wp : mCurrentWallpapers) {
            TodPickerView.Interval interval = new TodPickerView.Interval(wp.getStartHour(),
                    wp.getEndHour(), wp.getVibrantColor());
            mStartPicker.addUsedInterval(interval);
            mEndPicker.addUsedInterval(interval);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.framelayout_main_button:
                if(mPickingStart){
                    showEndPicker(true);
                }else {
                    if(mListener != null)
                        mListener.onTimePicked(mStartPicker.getTime(), mEndPicker.getTime());
                    dismiss();
                }
                break;
            case R.id.timedisplay_start:
                showStartPicker(true);
                break;
            case R.id.timedisplay_end:
                showEndPicker(true);
                break;
            case R.id.toggle_am:
                if(mPickingStart){
                    mStartPicker.setQuadrant(TodPickerView.AM);
                }else{
                    mEndPicker.setQuadrant(TodPickerView.AM);
                }
                mAmButton.setChecked(true);
                mPmButton.setChecked(false);
                break;
            case R.id.toggle_pm:
                if(mPickingStart){
                    mStartPicker.setQuadrant(TodPickerView.PM);
                }else{
                    mEndPicker.setQuadrant(TodPickerView.PM);
                }
                mPmButton.setChecked(true);
                mAmButton.setChecked(false);
                break;
        }
    }

    private void showStartPicker(boolean animate){
        mPickingStart = true;
        mTitleText.setText(getString(R.string.dialog_tod_picker_title_start));
        if(mStartPicker.getQuadrant() == TodPickerView.AM){
            mAmButton.setChecked(true);
            mPmButton.setChecked(false);
        }else{
            mPmButton.setChecked(true);
            mAmButton.setChecked(false);
        }
        if(animate) {
            ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1, 1.3f);
            ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1, 0);

            scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();

                    mEndPicker.setScaleX(animatedValue);
                    mEndPicker.setScaleY(animatedValue);

                    mStartPicker.setScaleX(animatedValue - 0.3f);
                    mStartPicker.setScaleY(animatedValue - 0.3f);
                }
            });
            scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    mEndPicker.setAlpha(animatedValue);
                    mStartPicker.setAlpha(1 - animatedValue);
                }
            });
            alphaAnimator.setInterpolator(new AccelerateInterpolator());

            ValueAnimator textAnimator = ValueAnimator.ofFloat(1, 0.5f);

            textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    mEndTimeDisplay.setScaleX(animatedValue);
                    mEndTimeDisplay.setScaleY(animatedValue);
                    mEndTimeDisplay.setAlpha(animatedValue);

                    mStartTimeDisplay.setScaleX(1.5f - animatedValue);
                    mStartTimeDisplay.setScaleY(1.5f - animatedValue);
                    mStartTimeDisplay.setAlpha(1.5f - animatedValue);
                }
            });

            textAnimator.setInterpolator(new AnticipateOvershootInterpolator(2));

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleAnimator, alphaAnimator, textAnimator);
            animatorSet.setDuration(250);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mStartPicker.setVisibility(View.VISIBLE);
                    mStartPicker.setAlpha(0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mEndPicker.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
        }else {
            mEndPicker.setVisibility(View.GONE);
            mStartPicker.setVisibility(View.VISIBLE);
            mStartTimeDisplay.setScaleX(1);
            mStartTimeDisplay.setScaleY(1);
            mStartTimeDisplay.setAlpha(1);

            mEndTimeDisplay.setScaleX(0.5f);
            mEndTimeDisplay.setScaleY(0.5f);
            mEndTimeDisplay.setAlpha(0.5f);

        }
        mMainButtonText.setText(getString(R.string.next));
        mMainButtonText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_action_navigation_arrow_forward,0);
    }

    private void showEndPicker(boolean animate){
        mTitleText.setText(getString(R.string.dialog_tod_picker_title_end));
        mPickingStart = false;
        mEndPicker.setQuadrant(mAmButton.isChecked() ? TodPickerView.AM : TodPickerView.PM);
        mEndPicker.enableActiveInterval(mStartPicker.getTime(), mColor);

        if(animate) {
            ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1, 0.7f);
            ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1, 0);

            scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();

                    mStartPicker.setScaleX(animatedValue);
                    mStartPicker.setScaleY(animatedValue);

                    mEndPicker.setScaleX(animatedValue + 0.3f);
                    mEndPicker.setScaleY(animatedValue + 0.3f);
                }
            });
            scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();

                    mStartPicker.setAlpha(animatedValue);
                    mEndPicker.setAlpha(1 - animatedValue);
                }
            });
            alphaAnimator.setInterpolator(new AccelerateInterpolator());

            ValueAnimator textAnimator = ValueAnimator.ofFloat(1, 0.5f);

            textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    mStartTimeDisplay.setScaleX(animatedValue);
                    mStartTimeDisplay.setScaleY(animatedValue);
                    mStartTimeDisplay.setAlpha(animatedValue);

                    mEndTimeDisplay.setScaleX(1.5f - animatedValue);
                    mEndTimeDisplay.setScaleY(1.5f - animatedValue);
                    mEndTimeDisplay.setAlpha(1.5f - animatedValue);
                }
            });

            textAnimator.setInterpolator(new AnticipateOvershootInterpolator(2));

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleAnimator, alphaAnimator, textAnimator);
            animatorSet.setDuration(250);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mEndPicker.setVisibility(View.VISIBLE);
                    mEndPicker.setAlpha(0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mStartPicker.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
        }else{
            mEndPicker.setVisibility(View.VISIBLE);
            mStartPicker.setVisibility(View.GONE);
            mEndTimeDisplay.setScaleX(1);
            mEndTimeDisplay.setScaleY(1);
            mEndTimeDisplay.setAlpha(1);

            mStartTimeDisplay.setScaleX(0.5f);
            mStartTimeDisplay.setScaleY(0.5f);
            mStartTimeDisplay.setAlpha(0.5f);
        }

        mMainButtonText.setText(getString(R.string.done));
        mMainButtonText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_action_navigation_check, 0,0,0);
    }

    @Override
    public void onTimeChanged(TodPickerView picker, Hour24 time) {
        if(picker == mStartPicker){
            mStartTimeDisplay.setTime(time);
        }else{
            mEndTimeDisplay.setTime(time);
        }
    }


    public interface TodPickerDialogListener {
        void onTimePicked(Hour24 startHour, Hour24 endHour);
        void onCancel();
    }
}
