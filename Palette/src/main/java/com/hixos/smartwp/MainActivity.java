package com.hixos.smartwp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hixos.smartwp.services.ServiceUtils;
import com.hixos.smartwp.services.ServicesActivity;
import com.hixos.smartwp.services.geofence.GeofenceDatabase;
import com.hixos.smartwp.services.slideshow.SlideshowDatabase;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.widget.CircleView;

public class MainActivity extends ActionBarActivity {

    private ServiceBubble mSlideshowBubble;
    private ServiceBubble mLocationBubble;
    private ServiceBubble mTimeOfDayBubble;

    private int mInitialActiveService;

    private ScrollView mScrollView;
    private HorizontalScrollView mHorizScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(null);

        if (MiscUtils.UI.hasTranslucentStatus(this) && Build.VERSION.SDK_INT < 21) {
            View statusBackground = findViewById(R.id.statusbar_background);
            statusBackground.setVisibility(View.VISIBLE);
            statusBackground.getLayoutParams().height = MiscUtils.UI.getStatusBarHeight(this);
        }

        mSlideshowBubble = new ServiceBubble(findViewById(R.id.bubble_slideshow), ServiceUtils.SERVICE_SLIDESHOW);
        mLocationBubble = new ServiceBubble(findViewById(R.id.bubble_geofence), ServiceUtils.SERVICE_GEOFENCE);
        mTimeOfDayBubble = new ServiceBubble(findViewById(R.id.bubble_timeofday), ServiceUtils.SERVICE_TIMEOFDAY);

        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mHorizScrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);

        mSlideshowBubble
                .setColor(getResources().getColor(R.color.accent_blue))
                .setText(getString(R.string.name_slideshow))
                .setInnerDrawable(getResources().getDrawable(R.drawable.ic_slideshow_dark))
                .setForeground(getResources().getDrawable(R.drawable.bubble_foreground_blue));

        mLocationBubble
                .setColor(getResources().getColor(R.color.accent_orange))
                .setText(getString(R.string.name_geofence))
                .setInnerDrawable(getResources().getDrawable(R.drawable.ic_geofence_dark))
                .setForeground(getResources().getDrawable(R.drawable.bubble_foreground_orange));

        mTimeOfDayBubble
                .setColor(getResources().getColor(R.color.accent_green))
                .setText(getString(R.string.name_geofence))
                .setInnerDrawable(getResources().getDrawable(R.drawable.ic_geofence_dark))
                .setForeground(getResources().getDrawable(R.drawable.bubble_foreground_orange));

        mInitialActiveService = ServiceUtils.getActiveService(this);

        switch (mInitialActiveService) {
            case ServiceUtils.SERVICE_SLIDESHOW:
                mSlideshowBubble.activate(false);
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                mLocationBubble.activate(false);
                break;
            case ServiceUtils.SERVICE_TIMEOFDAY:
                mTimeOfDayBubble.activate(false);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mInitialActiveService = ServiceUtils.getActiveService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ServiceUtils.getActiveService(this) != mInitialActiveService) {
            ServiceUtils.broadcastServiceActivated(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void onBubbleClick(int id) {
        switch (id) {
            case ServiceUtils.SERVICE_SLIDESHOW:
                if (mSlideshowBubble.isExpanded()) {
                    onSettingsClick(id);
                } else {
                    mLocationBubble.deactivate();
                    mTimeOfDayBubble.deactivate();
                    ServiceUtils.setActiveService(this, ServiceUtils.SERVICE_SLIDESHOW);
                }
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                if (mLocationBubble.isExpanded()) {
                    onSettingsClick(id);
                } else {
                    mSlideshowBubble.deactivate();
                    mTimeOfDayBubble.deactivate();
                    ServiceUtils.setActiveService(this, ServiceUtils.SERVICE_GEOFENCE);
                }
                break;
            case ServiceUtils.SERVICE_TIMEOFDAY:
                if (mTimeOfDayBubble.isExpanded()) {
                    onSettingsClick(id);
                } else {
                    mSlideshowBubble.deactivate();
                    mLocationBubble.deactivate();
                    //ServiceUtils.setActiveService(this, ServiceUtils.SERVICE_TIMEOFDAY);
                }
                break;
        }
    }

    private void onSettingsClick(int id) {
        Intent intent = new Intent(this, ServicesActivity.class);
        intent.putExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK,
                getIntent().getBooleanExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK, false));

        switch (id) {
            case ServiceUtils.SERVICE_SLIDESHOW:
                intent.putExtra(ServicesActivity.EXTRA_SERVIVE,
                        ServiceUtils.SERVICE_SLIDESHOW);
                startActivity(intent);
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                mSlideshowBubble.deactivate();
                intent.putExtra(ServicesActivity.EXTRA_SERVIVE,
                        ServiceUtils.SERVICE_GEOFENCE);
                startActivity(intent);
                break;
        }
    }

    private void onBubbleExpanded(int id) {
        switch (id) {
            case ServiceUtils.SERVICE_SLIDESHOW:
                SlideshowDatabase sdatabase = new SlideshowDatabase(this);
                if (sdatabase.getWallpaperCount() == 0) {
                    onSettingsClick(ServiceUtils.SERVICE_SLIDESHOW);
                }
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                if (!GeofenceDatabase.hasDefaultWallpaper()) {
                    onSettingsClick(ServiceUtils.SERVICE_GEOFENCE);
                }
                break;
            case ServiceUtils.SERVICE_TIMEOFDAY:
                /*if (!GeofenceDatabase.hasDefaultWallpaper()) {
                    onSettingsClick(ServiceUtils.SERVICE_GEOFENCE);
                }*/
                break;
        }
    }

    private class ServiceBubble implements View.OnClickListener {
        private final static int ANIMATION_DURATION = 300;
        private final static float MIN_RADIUS = 0.5f;
        private final static float MAX_RADIUS = 1.0f;

        private final static float MIN_ALPHA = 0.4f;

        private int mID;

        private View mView;
        private CircleView mCircle;
        private TextView mTextName;
        private boolean mActive = false;
        private boolean mExpanded = false;
        private AnimatorSet mAnimatorSet;

        public ServiceBubble(View bubbleView, int id) {
            mView = bubbleView;
            mCircle = (CircleView) bubbleView.findViewById(R.id.circleView);
            mTextName = (TextView) bubbleView.findViewById(R.id.textview_name);
            setDimensions(bubbleView);
            mID = id;
            init();
        }

        private void setDimensions(View bubbleView) {
            int orientation = getResources().getConfiguration().orientation;
            Point displaySize = MiscUtils.UI.getDisplaySize(MainActivity.this);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                bubbleView.getLayoutParams().height = (int) Math.round(displaySize.y / 3f);
            } else {
                bubbleView.getLayoutParams().width = (int) Math.round(displaySize.x / 2.6f);
            }
        }

        public boolean isActive() {
            return mActive;
        }

        public boolean isExpanded() {
            return mExpanded;
        }

        private void init() {
            mCircle.setRadiusPerc(MIN_RADIUS);
            mCircle.setAlpha(MIN_ALPHA);
            mTextName.setAlpha(MIN_ALPHA);
            mCircle.setOnClickListener(this);
        }

        public ServiceBubble setColor(int color) {
            mCircle.setColor(color);
            mTextName.setTextColor(color);
            return this;
        }

        public ServiceBubble setText(CharSequence text) {
            mTextName.setText(text);
            return this;
        }

        public ServiceBubble setInnerDrawable(Drawable innerDrawable) {
            mCircle.setInnerDrawable((BitmapDrawable) innerDrawable);
            return this;
        }

        public ServiceBubble setForeground(Drawable foreground) {
            mCircle.setForeground(foreground);
            return this;
        }

        public void activate() {
            activate(true);
        }

        public void deactivate() {
            deactivate(true);
        }

        public void activate(boolean animate) {
            if (mActive) return;
            mActive = true;
            if (mAnimatorSet != null) {
                mAnimatorSet.cancel();
            }

            if (animate) {
                ObjectAnimator circleRadiusAnimator = ObjectAnimator.ofFloat(mCircle,
                        "radiusPerc", mCircle.getRadiusPerc(), MAX_RADIUS);
                ObjectAnimator circleAlphaAnimator = ObjectAnimator.ofFloat(mCircle,
                        "alpha", mCircle.getAlpha(), 1.0f);
                circleAlphaAnimator.setInterpolator(new AnticipateOvershootInterpolator());
                circleRadiusAnimator.setInterpolator(new AnticipateOvershootInterpolator());

                ObjectAnimator textAlphaAnimator = ObjectAnimator.ofFloat(mTextName,
                        "alpha", mTextName.getAlpha(), 1.0f);
                textAlphaAnimator.setInterpolator(new LinearInterpolator());

                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.play(circleRadiusAnimator)
                        .with(circleAlphaAnimator)
                        .with(textAlphaAnimator);
                mAnimatorSet.setDuration(ANIMATION_DURATION);
                mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        onBubbleExpanded(mID);
                        mExpanded = true;
                    }
                });

                mAnimatorSet.start();
                Rect hitRect = new Rect();
                mView.getHitRect(hitRect);
                Rect scrollRect = new Rect();
                if (mScrollView != null) {
                    mScrollView.getDrawingRect(scrollRect);
                    Logger.logRectSides("IXV", "hitRect", hitRect);
                    Logger.logRectSides("IXV", "scrollRect", scrollRect);
                    if (!scrollRect.contains(hitRect)) {
                        mScrollView.smoothScrollTo(mView.getLeft(), mView.getTop());
                    }
                } else if (mHorizScrollView != null) {
                    mHorizScrollView.getDrawingRect(scrollRect);
                    if (scrollRect.contains(hitRect)) {
                        mHorizScrollView.smoothScrollTo(mView.getLeft(), mView.getTop());
                    }
                }
            } else {
                mTextName.setAlpha(1.0f);
                mCircle.setAlpha(1.0f);
                mCircle.setRadiusPerc(MAX_RADIUS);
                mExpanded = true;
                if (mScrollView != null) {
                    mScrollView.scrollTo(mView.getLeft(), mView.getTop() - mView.getPaddingTop());
                } else if (mHorizScrollView != null) {
                    mHorizScrollView.scrollTo(mView.getLeft(), mView.getTop());
                }
            }

        }

        public void deactivate(boolean animate) {
            if (!mActive) return;
            mActive = false;
            if (mAnimatorSet != null) {
                mAnimatorSet.cancel();
            }

            if (animate) {
                ObjectAnimator circleRadiusAnimator = ObjectAnimator.ofFloat(mCircle,
                        "radiusPerc", mCircle.getRadiusPerc(), MIN_RADIUS);
                ObjectAnimator circleAlphaAnimator = ObjectAnimator.ofFloat(mCircle,
                        "alpha", mCircle.getAlpha(), MIN_ALPHA);
                circleAlphaAnimator.setInterpolator(new AnticipateOvershootInterpolator());
                circleRadiusAnimator.setInterpolator(new AnticipateOvershootInterpolator());

                ObjectAnimator textAlphaAnimator = ObjectAnimator.ofFloat(mTextName,
                        "alpha", mTextName.getAlpha(), MIN_ALPHA);
                textAlphaAnimator.setInterpolator(new LinearInterpolator());
                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.play(circleRadiusAnimator)
                        .with(circleAlphaAnimator)
                        .with(textAlphaAnimator);
                mAnimatorSet.setDuration(ANIMATION_DURATION);
                mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mExpanded = false;
                    }
                });
                mAnimatorSet.start();
            } else {
                mTextName.setAlpha(MIN_ALPHA);
                mCircle.setAlpha(MIN_ALPHA);
                mCircle.setRadiusPerc(MIN_RADIUS);
                mExpanded = false;
            }
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.circleView:
                    activate();
                    onBubbleClick(mID);
                    break;
            }
        }
    }
}
