package com.hixos.smartwp.triggers.timeofday;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Hour24;
import com.hixos.smartwp.widget.TodPickerView;

import java.util.ArrayList;

public class TimePickerActivity extends ActionBarActivity implements View.OnClickListener, TodPickerView.OnTimeChangedListener{
    public final static String EXTRA_WALLPAPERS = "used_intervals";
    public final static String EXTRA_COLOR = "color";
    public final static String RESULT_START_HOUR = "result_start_hour";
    public final static String RESULT_END_HOUR = "result_end_hour";

    private TodPickerView mStartPicker;
    private TodPickerView mEndPicker;

    private Hour24 mStartTime;
    private Hour24 mEndTime;

    private boolean mPickingStartTime = true;
    private Button mNextButton;
    private Button mDoneButton;
    private Button mBackButton;

    private TextView mTimeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker);

        mStartPicker = (TodPickerView)findViewById(R.id.todStartPickerView);
        mEndPicker = (TodPickerView)findViewById(R.id.todEndPickerView);

        mStartPicker.setOnTimeChangedListener(this);
        mEndPicker.setOnTimeChangedListener(this);

        mTimeText = (TextView)findViewById(R.id.textTime);

        mNextButton = (Button)findViewById(R.id.buttonNext);
        mNextButton.setOnClickListener(this);
        mDoneButton = (Button)findViewById(R.id.buttonDone);
        mDoneButton.setOnClickListener(this);
        mBackButton = (Button)findViewById(R.id.buttonBack);
        mBackButton.setOnClickListener(this);

        ArrayList<Parcelable> wallpapersParc
                = getIntent().getParcelableArrayListExtra(EXTRA_WALLPAPERS);

        if(wallpapersParc != null){
            for(Parcelable p : wallpapersParc){
                TimeOfDayWallpaper wp = (TimeOfDayWallpaper)p;
                TodPickerView.Interval interval = new TodPickerView.Interval(wp.getStartHour(),
                        wp.getEndHour(), wp.getVibrantColor());
                mStartPicker.addUsedInterval(interval);
                mEndPicker.addUsedInterval(interval);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonNext:{
                mStartTime = mStartPicker.getTime();
                mEndPicker.enableActiveInterval(mStartPicker.getTime(),
                        getIntent().getIntExtra(EXTRA_COLOR, getResources()
                                .getColor(R.color.accent_blue)));
                ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1, 0.5f);
                ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1, 0);
                scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float animatedValue = (float) animation.getAnimatedValue();

                        mStartPicker.setScaleX(animatedValue);
                        mStartPicker.setScaleY(animatedValue);

                        mEndPicker.setScaleX(animatedValue + 0.5f);
                        mEndPicker.setScaleY(animatedValue + 0.5f);
                    }
                });
                alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float animatedValue = (float) animation.getAnimatedValue();

                        mStartPicker.setAlpha(animatedValue);
                        mEndPicker.setAlpha(1 - animatedValue);
                    }
                });

                Animator backReveal;
                Animator doneReveal;
                Animator nextConceal;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int cx = mBackButton.getWidth();
                    int cy = mBackButton.getHeight() / 2;

                    int finalRadius = Math.max(mBackButton.getWidth(), mBackButton.getHeight());
                    backReveal = ViewAnimationUtils
                            .createCircularReveal(mBackButton, cx, cy, 0, finalRadius);

                    cx = mNextButton.getWidth() / 2;
                    cy = mNextButton.getHeight() / 2;
                    finalRadius = Math.max(mNextButton.getWidth(), mNextButton.getHeight()) / 2;

                    doneReveal = ViewAnimationUtils
                            .createCircularReveal(mDoneButton, cx, cy, 0, finalRadius);
                    nextConceal = ViewAnimationUtils
                            .createCircularReveal(mNextButton, cx, cy, finalRadius, 0);

                } else {
                    backReveal = ObjectAnimator.ofFloat(mBackButton, "alpha", 0, 1);
                    doneReveal = ObjectAnimator.ofFloat(mDoneButton, "alpha", 0, 1);
                    nextConceal = ObjectAnimator.ofFloat(mNextButton, "alpha", 1, 0);
                }

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleAnimator, alphaAnimator,
                        backReveal, doneReveal, nextConceal);
                animatorSet.setDuration(250).setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mEndPicker.setVisibility(View.VISIBLE);
                        mEndPicker.setAlpha(0);
                        mBackButton.setVisibility(View.VISIBLE);
                        mDoneButton.setVisibility(View.VISIBLE);
                        mNextButton.setClickable(false);

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mStartPicker.setVisibility(View.GONE);
                        mBackButton.setClickable(true);
                        mNextButton.setVisibility(View.INVISIBLE);
                        mDoneButton.setClickable(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animatorSet.start();
                break;
            }
            case R.id.buttonDone:
                mEndTime = mEndPicker.getTime();
                Intent result = new Intent();
                result.putExtra(RESULT_START_HOUR, mStartTime.getMinutes());
                result.putExtra(RESULT_END_HOUR, mEndTime.getMinutes());
                setResult(RESULT_OK, result);
                finish();
                break;
            case R.id.buttonBack:
                ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.5f, 1f);
                ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0, 1);

                scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float animatedValue = (float) animation.getAnimatedValue();

                        mStartPicker.setScaleX(animatedValue);
                        mStartPicker.setScaleY(animatedValue);

                        mEndPicker.setScaleX(animatedValue + 0.5f);
                        mEndPicker.setScaleY(animatedValue + 0.5f);
                    }
                });
                alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float animatedValue = (float) animation.getAnimatedValue();

                        mStartPicker.setAlpha(animatedValue);
                        mEndPicker.setAlpha(1 - animatedValue);
                    }
                });

                Animator backConceal;
                Animator doneConceal;
                Animator nextReveal;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int cx = mBackButton.getWidth();
                    int cy = mBackButton.getHeight() / 2;

                    int startRadius = Math.max(mBackButton.getWidth(), mBackButton.getHeight());

                    backConceal = ViewAnimationUtils
                            .createCircularReveal(mBackButton, cx, cy, startRadius, 0);
                    cx = mNextButton.getWidth() / 2;
                    cy = mNextButton.getHeight() / 2;
                    startRadius = Math.max(mNextButton.getWidth(), mNextButton.getHeight()) / 2;

                    doneConceal = ViewAnimationUtils
                            .createCircularReveal(mDoneButton, cx, cy, startRadius, 0);
                    nextReveal = ViewAnimationUtils
                            .createCircularReveal(mNextButton, cx, cy, 0, startRadius);

                } else {
                    backConceal = ObjectAnimator.ofFloat(mBackButton, "alpha", 1, 0);
                    doneConceal = ObjectAnimator.ofFloat(mDoneButton, "alpha", 1, 0);
                    nextReveal = ObjectAnimator.ofFloat(mNextButton, "alpha", 0, 1);
                }

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleAnimator, alphaAnimator, backConceal, nextReveal, doneConceal);
                animatorSet.setDuration(250).setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mStartPicker.setVisibility(View.VISIBLE);
                        mStartPicker.setAlpha(0);
                        mBackButton.setClickable(false);
                        mDoneButton.setClickable(false);
                        mNextButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mEndPicker.setVisibility(View.GONE);
                        mBackButton.setVisibility(View.INVISIBLE);
                        mDoneButton.setVisibility(View.INVISIBLE);
                        mNextButton.setClickable(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animatorSet.start();
        }
    }

    @Override
    public void onTimeChanged(Hour24 time) {
        mTimeText.setText(time.toString());
    }
}
