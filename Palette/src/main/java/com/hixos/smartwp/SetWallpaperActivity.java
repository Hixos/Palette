package com.hixos.smartwp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnticipateInterpolator;

import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.widget.CircleView;


public class SetWallpaperActivity extends ActionBarActivity implements View.OnClickListener {
    private CircleView mCircle;
    private boolean mClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.acitvity_fade_out);
        setContentView(R.layout.activity_set_wallpaper);
        mCircle = (CircleView) findViewById(R.id.circleView);
        mCircle.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_fade_in, R.anim.acitvity_fade_out);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.set_wallpaper, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.circleView:
                if (!mClicked) {
                    mClicked = true;
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mCircle, "radiusPerc", mCircle.getRadiusPerc(), 3);
                    animator.setDuration(600);
                    animator.setInterpolator(new AnticipateInterpolator(0.9f));
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            MiscUtils.Activity.showLiveWallpaperActivity(SetWallpaperActivity.this);
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                    animator.start();
                }
                break;
        }
    }
}
