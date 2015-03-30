package com.hixos.smartwp.triggers;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.hixos.smartwp.R;
import com.hixos.smartwp.SettingsActivity;
import com.hixos.smartwp.triggers.geofence.GeofenceFragment;
import com.hixos.smartwp.triggers.slideshow.SlideshowFragment;
import com.hixos.smartwp.triggers.timeofday.TodFragment;
import com.hixos.smartwp.utils.MiscUtils;

public class ServicesActivity extends ActionBarActivity {
    public final static String EXTRA_SERVIVE = "com.hixos.smartwp.EXTRA_SERVICE";
    public final static String EXTRA_DISABLE_LWP_CHECK = "com.hixos.smartp.DISABLE_LWP_CHECK";

    private static final String SLIDESHOW = "slideshow";
    private static final String GEOFENCE = "geofence";
    private static final String TIMEOFDAY = "timeofday";

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                Fragment f = getFragmentManager().findFragmentByTag(GEOFENCE);
                if (f != null) {
                    f.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);
        getWindow().setBackgroundDrawable(null);

        if (MiscUtils.UI.hasTranslucentStatus(this) && Build.VERSION.SDK_INT < 21) {
            View statusBackground = findViewById(R.id.statusbar_background);
            statusBackground.setVisibility(View.VISIBLE);
            statusBackground.getLayoutParams().height = MiscUtils.UI.getStatusBarHeight(this);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String tag;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SERVIVE)) {
            switch (getIntent().getExtras().getInt(EXTRA_SERVIVE, 1)) {
                default:
                case ServiceUtils.SERVICE_SLIDESHOW:
                    tag = SLIDESHOW;
                    break;
                case ServiceUtils.SERVICE_TIMEOFDAY:
                    tag = TIMEOFDAY;
                    break;
                case ServiceUtils.SERVICE_GEOFENCE:
                    tag = GEOFENCE;
                    break;
            }
        } else {
            tag = SLIDESHOW;
        }

        Fragment f = getFragmentManager().findFragmentByTag(tag);
        if (f == null) {
            if (tag.equals(SLIDESHOW)) {
                f = new SlideshowFragment();
            } else if (tag.equals(GEOFENCE)) {
                f = new GeofenceFragment();
            } else if (tag.equals(TIMEOFDAY)) {
                f = new TodFragment();
            }
            FragmentManager manager = getFragmentManager();
            manager.beginTransaction()
                    .replace(R.id.content_frame, f, tag)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
