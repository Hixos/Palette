package com.hixos.smartwp.triggers.geofence;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.GeoMath;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.utils.UnitLocale;
import com.hixos.smartwp.widget.ErrorDialogFragment;
import com.hixos.smartwp.widget.ProgressDialogFragment;
import com.hixos.smartwp.widget.SearchBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import com.google.android.gms.location.LocationClient;

public class GeofencePickerActivity extends ActionBarActivity implements View.OnClickListener {
    public final static String EXTRA_GEOFENCES = "geofences";
    public final static String EXTRA_UID = "uid";
    public final static String EXTRA_TARGET_GEOFENCE = "target_geofence";
    public final static String EXTRA_COLOR = "color";
    public final static String RESULT_LATITUDE = "latitude";
    public final static String RESULT_LONGITUDE = "longitude";
    public final static String RESULT_RADIUS = "radius";
    public final static String RESULT_UID = "uid";
    public final static String RESULT_DISTANCE = "distance";
    public final static String RESULT_ZOOM = "zoom";
    private static final String LOGTAG = "GeofencePicker";

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private SearchBox mSearchBox;
    private GoogleMap mMap;
    private PlayLocationSource mLocationSource;

    private String uid;
    private Intent mReturnIntent;

    private Location mLocation;

    private String mTargetGeofenceUid;
    private Handler mSnapshotHandle;
    private ProgressDialogFragment mProgressDialog;

    private List<Circle> mOtherGeofences;
    private Circle mCurrentGeofence;
    private Marker mSearchMarker;

    private int mColor;
    private boolean mPositioned = false;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    checkPlayServices();
                } else {
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence_picker);

        if (!checkPlayServices()) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        mOtherGeofences = new ArrayList<>();
        mColor = getIntent().getIntExtra(EXTRA_COLOR, GeofencePickerFragment.getRandomAccentColor(this));

        createActionbar();
        initMap(savedInstanceState, (GeofenceWallpaper) getIntent().getParcelableExtra(EXTRA_TARGET_GEOFENCE));

        if (savedInstanceState != null) {
            mTargetGeofenceUid = savedInstanceState.getString("target_uid");
            mColor = savedInstanceState.getInt("color");
        }

        drawGeofences(getIntent().getParcelableArrayListExtra(EXTRA_GEOFENCES));
        uid = getIntent().getStringExtra(EXTRA_UID);

        mProgressDialog = (ProgressDialogFragment) getFragmentManager().findFragmentByTag("progress_dialog");
    }


    private boolean checkPlayServices() {
        int errorCode = GooglePlayServicesUtil.
                isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == errorCode) {
            Log.d("Location Updates",
                    "Google Play services is available.");
            return true;
        } else {
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if (errorDialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                errorFragment.show(getFragmentManager(), "services_error");
            }
            return false;
        }
    }

    private void createActionbar() {
        final ActionBar ab = getSupportActionBar();

        ab.setCustomView(R.layout.actionbar_geofence_picker);
        ab.setDisplayShowCustomEnabled(true);
        mSearchBox = (SearchBox) findViewById(R.id.search_box);
        if (ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey()) {
            mSearchBox.setPadding(mSearchBox.getPaddingLeft(), mSearchBox.getPaddingTop(), getResources().getDimensionPixelSize(R.dimen.search_box_padding_right), mSearchBox.getPaddingBottom());
        }
        if (!Geocoder.isPresent()) {
            mSearchBox.setVisibility(View.GONE);
        }
        mSearchBox.setOnSearchActionListener(new SearchBox.OnSearchActionListener() {
            @Override
            public void onSearch(String query) {
                if (mSearchMarker != null) {
                    mSearchMarker.remove();
                }
                GeocoderTask task = new GeocoderTask();
                task.execute(query);
            }

            @Override
            public void onDismiss() {
                if (mSearchMarker != null) {
                    mSearchMarker.remove();
                }
            }
        });
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        findViewById(R.id.button_done).setOnClickListener(this);
    }

    private void initMap(Bundle savedInstanceState, GeofenceWallpaper target) {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_frame)).getMap();
        }

        if (mMap == null) {
            Toast.makeText(this, getString(R.string.error_cant_load_map), Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        int top, bottom = 0;
        top = MiscUtils.UI.getActionBarHeight(this);

        if (MiscUtils.UI.hasTranslucentStatus(this) && Build.VERSION.SDK_INT < 21) {
            View statusBackground = findViewById(R.id.statusbar_background);
            statusBackground.setVisibility(View.VISIBLE);
            statusBackground.getLayoutParams().height = MiscUtils.UI.getStatusBarHeight(this);
        }

        if (MiscUtils.UI.addStatusBarPadding(this)) {
            top += MiscUtils.UI.getStatusBarHeight(this);
        }

        if (MiscUtils.UI.hasTranslucentNavigation(this)) {
            bottom += MiscUtils.UI.getNavBarHeight(this);
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_pointer);
        mMap.setPadding(0, top, 0, bottom);
        layout.setPadding(0, top, 0, bottom);
        mMap.setIndoorEnabled(false);
        if (savedInstanceState != null) {
            CameraPosition.Builder builder = new CameraPosition.Builder();
            builder.target(new LatLng(savedInstanceState.getDouble("current_latitude"), savedInstanceState.getDouble("current_longitude")))
                    .bearing(savedInstanceState.getFloat("current_bearing"))
                    .zoom(savedInstanceState.getFloat("current_zoom"));
            CameraUpdate cu = CameraUpdateFactory.newCameraPosition(builder.build());
            mMap.moveCamera(cu);
            mPositioned = true;
        } else if (target != null) {
            mColor = target.getColor();
            mTargetGeofenceUid = target.getUid();
            gotoGeofence(target);
            mPositioned = true;
        }

        if (MiscUtils.Location.networkLocationProviderEnabled(this)) {
            mMap.setMyLocationEnabled(true);

            if (mLocationSource == null) {
                mLocationSource = new PlayLocationSource(!mPositioned);
            }
            mMap.setLocationSource(mLocationSource);
        }

        mMap.setMapType(Preferences.getInt(this, R.string.preference_map_type, GoogleMap.MAP_TYPE_HYBRID));
        CircleOptions opt = new CircleOptions()
                .center(getCenter())
                .radius(getRadius())
                .zIndex(1)
                .fillColor(adjustAlpha(mColor, 0.4f))
                .strokeColor(adjustAlpha(mColor, 0.8f))
                .strokeWidth(getResources().getDimensionPixelSize(R.dimen.geofence_border_width));
        mCurrentGeofence = mMap.addCircle(opt);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mCurrentGeofence.setCenter(getCenter());
                mCurrentGeofence.setRadius(getRadius());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        CameraPosition ps = mMap.getCameraPosition();

        outState.putDouble("current_latitude", ps.target.latitude);
        outState.putDouble("current_longitude", ps.target.longitude);
        outState.putFloat("current_zoom", ps.zoom);
        outState.putFloat("current_bearing", ps.bearing);
        outState.putInt("color", mColor);
        outState.putString("target_uid", mTargetGeofenceUid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationSource != null) {
            mLocationSource.activateRequests();
        }
    }

    @Override
    protected void onPause() {
        if (mLocationSource != null) {
            mLocationSource.deactivateRequests();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mLocationSource != null)
            mLocationSource.deactivate();

        mLocationSource = null;
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.geofence_picker, menu);
        MenuItem sat = menu.findItem(R.id.action_sat_imagery);
        if (sat != null) {
            sat.setChecked(Preferences.getInt(this, R.string.preference_map_type,
                    GoogleMap.MAP_TYPE_SATELLITE) == GoogleMap.MAP_TYPE_SATELLITE);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sat_imagery:
                boolean checked = item.isChecked();
                item.setChecked(!checked);

                if (item.isChecked()) {
                    Preferences.setInt(this, R.string.preference_map_type, GoogleMap.MAP_TYPE_SATELLITE);
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    Preferences.setInt(this, R.string.preference_map_type, GoogleMap.MAP_TYPE_NORMAL);
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoGeofence(GeofenceWallpaper target) {
        //LatLngBounds bounds = new LatLngBounds(ne, sw);
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(target.getLatLng(), target.getZoomLevel());
        mMap.animateCamera(cu);
    }

    private void animateToLocation(double lat, double lng) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), mMap.getCameraPosition().zoom);
        mMap.animateCamera(cu);
    }

    private void animateToLocation(double lat, double lng, float zoom) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoom);
        mMap.animateCamera(cu);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
           /* case R.id.button_discard:
                setResult(RESULT_CANCELED);
                finish();
                break;*/
            case R.id.button_done:
                mReturnIntent = new Intent();
                LatLng center = getCenter();

                mReturnIntent.putExtra(RESULT_LATITUDE, center.latitude);
                mReturnIntent.putExtra(RESULT_LONGITUDE, center.longitude);
                float radius = getRadius();
                boolean imperial = Preferences.getBoolean(this, R.string.preference_imperial_system, false);
                if (imperial) {
                    if (UnitLocale.toYards(radius) < 50) {
                        Toast.makeText(this,
                                String.format(getString(R.string.error_geofence_too_small), getResources().getQuantityString(R.plurals.yard, 50).toLowerCase()),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    if (radius < 50) {
                        Toast.makeText(this,
                                String.format(getString(R.string.error_geofence_too_small), getResources().getQuantityString(R.plurals.meter, 50).toLowerCase()),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                mSearchBox.collapse();
                mReturnIntent.putExtra(RESULT_RADIUS, radius);
                mReturnIntent.putExtra(RESULT_UID, uid);
                mReturnIntent.putExtra(RESULT_ZOOM, mMap.getCameraPosition().zoom);

                if (mLocation != null) {
                    float distance = GeoMath.getDistance(center.latitude, center.longitude,
                            mLocation.getLatitude(), mLocation.getLongitude()) - radius;
                    if (distance < 0) {
                        distance = 0;
                    }
                    mReturnIntent.putExtra(RESULT_DISTANCE, distance);
                }


                for (Circle c : mOtherGeofences) {
                    c.setVisible(false);
                }
                mOtherGeofences.clear();
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(false);

                mSnapshotHandle = new Handler();
                mSnapshotHandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getSnapshot();
                    }
                }, 1000);
                mProgressDialog = new ProgressDialogFragment();
                mProgressDialog.show(getFragmentManager(), "progress_dialog");
                break;
        }
    }

    private void drawGeofences(ArrayList<Parcelable> geofences) {
        if (geofences == null) {
            Log.e(LOGTAG, "Geofence parcelable is null");
            return;
        }

        for (Parcelable p : geofences) {
            GeofenceWallpaper g = (GeofenceWallpaper) p;

            if (!g.getUid().equals(mTargetGeofenceUid)) {
                drawGeofence(g, g.getColor());
            }
        }
    }

    private void drawGeofence(GeofenceWallpaper data, int color) {
        CircleOptions opt = new CircleOptions()
                .center(data.getLatLng())
                .radius(data.getRadius())
                .fillColor(adjustAlpha(color, 0.25f))
                .strokeColor(adjustAlpha(color, 0.65f))
                .strokeWidth(getResources().getDimensionPixelSize(R.dimen.geofence_border_width));
        mOtherGeofences.add(mMap.addCircle(opt));
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float getRadius() {
        View gmap = findViewById(R.id.map_frame);

        int mw = gmap.getWidth();
        int mh = gmap.getHeight();
        Point pointer_pos;

        if (mw > mh) {
            pointer_pos = new Point(mw / 2, mh - mh / 8);
        } else {
            pointer_pos = new Point(mw - mw / 6, mh / 2);
        }


        LatLng center = getCenter();
        LatLng rad = mMap.getProjection().fromScreenLocation(pointer_pos);
        float radius[] = new float[1];
        Location.distanceBetween(center.latitude, center.longitude, rad.latitude, rad.longitude, radius);
        return radius[0];
    }

    private LatLng getCenter() {
        return mMap.getCameraPosition().target;
    }

    private void getSnapshot() {
        mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                SaveSnapshotTask task = new SaveSnapshotTask();
                task.execute(bitmap);
            }
        });
    }

    private String parseAddress(Address address) {
        String out = address.getFeatureName();
        boolean comma = !isNumeric(out);
        if (address.getThoroughfare() != null)
            out += (comma ? ", " : " ") + address.getThoroughfare();
        if (address.getSubLocality() != null) out += ", " + address.getSubLocality();
        if (address.getLocality() != null && !address.getLocality().equals(address.getFeatureName()))
            out += ", " + address.getLocality();
        if (address.getAdminArea() != null) out += ", " + address.getAdminArea();
        if (address.getCountryName() != null) out += ", " + address.getCountryName();
        return out;
    }

    private boolean isNumeric(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public class PlayLocationSource implements LocationSource, GoogleApiClient.OnConnectionFailedListener,
            GoogleApiClient.ConnectionCallbacks, LocationListener {

        private GoogleApiClient mGoogleClient;
        private OnLocationChangedListener mListener;
        private LocationRequest mLocationRequest;

        private boolean mGotoLocation;

        public PlayLocationSource(boolean gotoLocation) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval(5000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            mGotoLocation = gotoLocation;
        }

        @Override
        public void onConnected(Bundle bundle) {
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
            mListener.onLocationChanged(mLocation);
            activateRequests();
            if (mGotoLocation && mLocation != null) {
                LatLng coord = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coord, 18f));
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            mListener.onLocationChanged(location);
        }

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mGoogleClient = null;
            mGoogleClient = new GoogleApiClient.Builder(GeofencePickerActivity.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleClient.connect();
            mListener = onLocationChangedListener;
        }

        @Override
        public void deactivate() {
            deactivateRequests();
            mGoogleClient.disconnect();
        }

        public void activateRequests() {
            if (mGoogleClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleClient, mLocationRequest, this);
            }
        }

        public void deactivateRequests() {
            if (mGoogleClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, this);
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

    private class GeocoderTask extends AsyncTask<String, Void, Address> {
        @Override
        protected Address doInBackground(String... strings) {
            Address out = getAddress1(strings[0]);

            if (out == null) {
                out = getAddress2(strings[0]);
            }

            return out;
        }

        private Address getAddress1(String query) {
            Geocoder gc = new Geocoder(getApplicationContext());
            try {
                List<Address> address = gc.getFromLocationName(query, 1);
                if (address != null && address.size() > 0 && address.get(0).hasLatitude() && address.get(0).hasLongitude())
                    return address.get(0);
                else
                    return null;
            } catch (IOException ex) {
                Log.e(LOGTAG, "Error decoding address. (1) " + ex.getMessage());
                return null;
            }
        }

        private Address getAddress2(String query) {
            com.hixos.smartwp.utils.Geocoder gc = new com.hixos.smartwp.utils.Geocoder(getApplicationContext());
            try {
                List<Address> address = gc.getFromLocationName(query, 1);
                if (address != null && address.size() > 0 && address.get(0).hasLatitude() && address.get(0).hasLongitude())
                    return address.get(0);
                else
                    return null;
            } catch (IOException ex) {
                Log.e(LOGTAG, "Error decoding address. (2) " + ex.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Address address) {
            super.onPostExecute(address);
            mSearchBox.hideProgress();
            if (address != null) {
                MarkerOptions options = new MarkerOptions()
                        .title(parseAddress(address))
                        .position(new LatLng(address.getLatitude(), address.getLongitude()));
                mSearchMarker = mMap.addMarker(options);
                animateToLocation(address.getLatitude(), address.getLongitude());
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_search_location_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SaveSnapshotTask extends AsyncTask<Bitmap, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Bitmap... bitmaps) {
            boolean success = ImageManager.getInstance().saveBitmap(bitmaps[0],
                    GeofenceDB.getSnapshotUid(uid));
            ImageManager.RecycleBin recycleBin = ImageManager.getInstance().getRecycleBin();
            if (recycleBin != null) {
                recycleBin.put(bitmaps[0]);
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                setResult(RESULT_OK, mReturnIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            finish();
        }
    }
}
