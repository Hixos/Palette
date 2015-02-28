package com.hixos.smartwp.triggers.timeofday;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.Hour;
import com.hixos.smartwp.widget.TodPickerView;

import java.util.ArrayList;

public class TimePickerActivity extends ActionBarActivity implements View.OnClickListener{
    public final static String EXTRA_WALLPAPERS = "used_intervals";
    public final static String EXTRA_COLOR = "color";
    public final static String RESULT_START_HOUR = "result_start_hour";
    public final static String RESULT_START_MINUTE = "result_start_minute";
    public final static String RESULT_START_PERIOD = "result_start_period";
    public final static String RESULT_END_HOUR = "result_end_hour";
    public final static String RESULT_END_MINUTE = "result_end_minute";
    public final static String RESULT_END_PERIOD = "result_end_period";

    private TodPickerView mStartPicker;
    private TodPickerView mEndPicker;

    private boolean mPickingStartTime = true;
    private Button mDoneButton;
    private Button mBackButton;

    private Hour mPickedStartTime;
    private Hour mPickedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker);

        mStartPicker = (TodPickerView)findViewById(R.id.todStartPickerView);
        mEndPicker = (TodPickerView)findViewById(R.id.todEndPickerView);

        mDoneButton = (Button)findViewById(R.id.buttonDone);
        mDoneButton.setOnClickListener(this);
        mBackButton = (Button)findViewById(R.id.buttonBack);
        mBackButton.setOnClickListener(this);

        ArrayList<Parcelable> wallpapersParc
                = getIntent().getParcelableArrayListExtra(EXTRA_WALLPAPERS);
          
        for(Parcelable p : wallpapersParc){
            TimeOfDayWallpaper wp = (TimeOfDayWallpaper)p;
            TodPickerView.ClockArea area
                    = new TodPickerView.ClockArea(wp.getStartHour(), wp.getEndHour(),
                    wp.getVibrantColor());
            mStartPicker.addUsedArea(area);
            mEndPicker.addUsedArea(area);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_time_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonDone:
                if(mPickingStartTime){
                    mPickingStartTime = false;
                    mBackButton.setVisibility(View.VISIBLE);
                    mDoneButton.setText("V Done"); //Todo: create string resource
                    mPickedStartTime = mStartPicker.getTime();
                    mEndPicker.enableActiveArea(mStartPicker.getTime(),
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

                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(scaleAnimator, alphaAnimator);
                    animatorSet.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
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
                    mPickedEndTime = mEndPicker.getTime();
                    Intent result = new Intent();
                    result.putExtra(RESULT_START_HOUR, mPickedStartTime.getHour());
                    result.putExtra(RESULT_START_MINUTE, mPickedStartTime.getMinute());
                    result.putExtra(RESULT_START_PERIOD, mPickedStartTime.getPeriod());
                    result.putExtra(RESULT_END_HOUR, mPickedEndTime.getHour());
                    result.putExtra(RESULT_END_MINUTE, mPickedEndTime.getMinute());
                    result.putExtra(RESULT_END_PERIOD, mPickedEndTime.getPeriod());
                    setResult(RESULT_OK, result);
                    finish();
                }
                break;
            case R.id.buttonBack:
                mPickingStartTime = true;
                mBackButton.setVisibility(View.GONE);
                mDoneButton.setText("Next ->"); //Todo: create string resource
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

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleAnimator, alphaAnimator);
                animatorSet.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
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
        }
    }
}
