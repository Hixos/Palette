package com.hixos.smartwp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hixos.smartwp.services.ServiceUtils;
import com.hixos.smartwp.services.ServicesActivity;
import com.hixos.smartwp.services.geofence.GeofenceDatabase;
import com.hixos.smartwp.services.slideshow.SlideshowDatabase;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.widget.CircleView;

public class MainActivity extends ActionBarActivity {

    private ServiceBubble mBubbleSlideshow;
    private ServiceBubble mBubbleGeofence;

    private int mInitialActiveService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(null);

        LinearLayout root = (LinearLayout)findViewById(R.id.root);

        int top, bottom = 0;
        top = MiscUtils.UI.getActionBarHeight(this);

        if(getResources().getBoolean(R.bool.has_translucent_navbar)){

            bottom += MiscUtils.UI.getNavBarHeight(this);
        }

        if(getResources().getBoolean(R.bool.has_translucent_statusbar)){
            top += MiscUtils.UI.getStatusBarHeight(this);
        }

        root.setPadding(root.getPaddingLeft(),
                root.getPaddingTop() + top,
                root.getPaddingRight(),
                root.getPaddingBottom() + bottom);

        mBubbleSlideshow = new ServiceBubble(findViewById(R.id.bubble_slideshow), ServiceUtils.SERVICE_SLIDESHOW);
        mBubbleGeofence = new ServiceBubble(findViewById(R.id.bubble_geofence), ServiceUtils.SERVICE_GEOFENCE);

        mBubbleSlideshow
                .setColor(getResources().getColor(R.color.accent_blue))
                .setText(getString(R.string.name_slideshow))
                .setInnerDrawable(getResources().getDrawable(R.drawable.ic_slideshow_dark))
                .setForeground(getResources().getDrawable(R.drawable.bubble_foreground_blue));

        mBubbleGeofence
                .setColor(getResources().getColor(R.color.accent_orange))
                .setText(getString(R.string.name_geofence))
                .setInnerDrawable(getResources().getDrawable(R.drawable.ic_geofence_dark))
                .setForeground(getResources().getDrawable(R.drawable.bubble_foreground_orange));

        mInitialActiveService = ServiceUtils.getActiveService(this);

        switch (mInitialActiveService){
            case ServiceUtils.SERVICE_SLIDESHOW:
                mBubbleSlideshow.activate(false);
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                mBubbleGeofence.activate(false);
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
        if(ServiceUtils.getActiveService(this) != mInitialActiveService){
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
        switch (item.getItemId()){
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void onBubbleClick(int id){
        switch (id){
            case ServiceUtils.SERVICE_SLIDESHOW:
                if(mBubbleSlideshow.isExpanded()){
                    onSettingsClick(id);
                }else{
                    mBubbleGeofence.deactivate();
                    ServiceUtils.setActiveService(this, ServiceUtils.SERVICE_SLIDESHOW);
                }
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                if(mBubbleGeofence.isExpanded()){
                    onSettingsClick(id);
                }else {
                    mBubbleSlideshow.deactivate();
                    ServiceUtils.setActiveService(this, ServiceUtils.SERVICE_GEOFENCE);
                }
                break;
        }
    }

    private void onSettingsClick(int id){
        Intent intent = new Intent(this, ServicesActivity.class);
        intent.putExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK,
                getIntent().getBooleanExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK, false));

        switch (id){
            case ServiceUtils.SERVICE_SLIDESHOW:
                intent.putExtra(ServicesActivity.EXTRA_SERVIVE,
                        ServiceUtils.SERVICE_SLIDESHOW);
                startActivity(intent);
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                mBubbleSlideshow.deactivate();
                intent.putExtra(ServicesActivity.EXTRA_SERVIVE,
                        ServiceUtils.SERVICE_GEOFENCE);
                startActivity(intent);
                break;
        }
    }

    private void onBubbleExpanded(int id){
        switch (id){
            case ServiceUtils.SERVICE_SLIDESHOW:
                SlideshowDatabase sdatabase = new SlideshowDatabase(this);
                if(sdatabase.getWallpaperCount() == 0){
                    onSettingsClick(ServiceUtils.SERVICE_SLIDESHOW);
                }
                break;
            case ServiceUtils.SERVICE_GEOFENCE:
                if(!GeofenceDatabase.hasDefaultWallpaper()){
                    onSettingsClick(ServiceUtils.SERVICE_GEOFENCE);
                }
                break;
        }
    }

    private class ServiceBubble implements View.OnClickListener {
        private final static int ANIMATION_DURATION = 300;
        private final static float MIN_RADIUS = 0.5f;
        private final static float MAX_RADIUS = 1.0f;

        private final static float MIN_ALPHA = 0.4f;

        private int mID;

        private CircleView mCircle;
        private TextView mTextName;

        public boolean isActive() {
            return mActive;
        }

        private boolean mActive = false;

        public boolean isExpanded() {
            return mExpanded;
        }

        private boolean mExpanded = false;

        private AnimatorSet mAnimatorSet;


        public ServiceBubble(View bubbleView, int id){
            mCircle = (CircleView)bubbleView.findViewById(R.id.circleView);
            mTextName = (TextView)bubbleView.findViewById(R.id.textview_name);
            mID = id;
            init();
        }

        private void init(){
            mCircle.setRadiusPerc(MIN_RADIUS);
            mCircle.setAlpha(MIN_ALPHA);
            mTextName.setAlpha(MIN_ALPHA);
            mCircle.setOnClickListener(this);
        }

        public ServiceBubble setColor(int color){
            mCircle.setColor(color);
            mTextName.setTextColor(color);
            return this;
        }

        public ServiceBubble setText(CharSequence text){
            mTextName.setText(text);
            return this;
        }

        public ServiceBubble setInnerDrawable(Drawable innerDrawable){
            mCircle.setInnerDrawable((BitmapDrawable)innerDrawable);
            return this;
        }

        public ServiceBubble setForeground(Drawable foreground){
            mCircle.setForeground(foreground);
            return this;
        }

        public void activate(){
            activate(true);
        }

        public void deactivate(){
            deactivate(true);
        }

        public void activate(boolean animate){
            if(mActive) return;
            mActive = true;
            if(mAnimatorSet != null){
                mAnimatorSet.cancel();
            }

            if(animate){
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
            }else {
                mTextName.setAlpha(1.0f);
                mCircle.setAlpha(1.0f);
                mCircle.setRadiusPerc(MAX_RADIUS);
                mExpanded = true;
            }

        }

        public void deactivate(boolean animate){
            if(!mActive) return;
            mActive = false;
            if(mAnimatorSet != null){
                mAnimatorSet.cancel();
            }

            if(animate){
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
            }else{
                mTextName.setAlpha(MIN_ALPHA);
                mCircle.setAlpha(MIN_ALPHA);
                mCircle.setRadiusPerc(MIN_RADIUS);
                mExpanded = false;
            }
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.circleView:
                    activate();
                    onBubbleClick(mID);
                    break;
            }
        }
    }
}
