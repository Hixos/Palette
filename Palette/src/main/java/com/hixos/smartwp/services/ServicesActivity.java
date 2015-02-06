package com.hixos.smartwp.services;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.hixos.smartwp.R;
import com.hixos.smartwp.SettingsActivity;
import com.hixos.smartwp.services.geofence.GeofenceFragment;
import com.hixos.smartwp.services.slideshow.SlideshowFragment;
import com.hixos.smartwp.utils.MiscUtils;

public class ServicesActivity extends ActionBarActivity {
    public final static String EXTRA_SERVIVE = "com.hixos.smartwp.EXTRA_SERVICE";
    public final static String EXTRA_DISABLE_LWP_CHECK = "com.hixos.smartp.DISABLE_LWP_CHECK";

    private static final String SLIDESHOW = "slideshow";
    private static final String GEOFENCE = "geofence";
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

        if (getResources().getBoolean(R.bool.has_translucent_statusbar)) {
            View statusBackground = findViewById(R.id.statusbar_background);
            statusBackground.setVisibility(View.VISIBLE);
            statusBackground.getLayoutParams().height = MiscUtils.UI.getStatusBarHeight(this);
        }


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String tag;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SERVIVE)) {
            tag = getIntent().getExtras().getInt(EXTRA_SERVIVE, 1)
                    == ServiceUtils.SERVICE_GEOFENCE
                    ? GEOFENCE
                    : SLIDESHOW;
        } else {
            tag = SLIDESHOW;
        }

        Fragment f = getFragmentManager().findFragmentByTag(tag);
        if (f == null) {
            if (tag.equals(SLIDESHOW)) {
                f = new SlideshowFragment();
                FragmentManager manager = getFragmentManager();
                manager.beginTransaction()
                        .replace(R.id.content_frame, f, SLIDESHOW)
                        .commit();
            } else if (tag.equals(GEOFENCE)) {
                f = new GeofenceFragment();
                FragmentManager manager = getFragmentManager();
                manager.beginTransaction()
                        .replace(R.id.content_frame, f, GEOFENCE)
                        .commit();
            }
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
