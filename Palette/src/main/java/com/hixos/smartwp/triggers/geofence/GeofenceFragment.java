package com.hixos.smartwp.triggers.geofence;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.CropperActivity;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.R;
import com.hixos.smartwp.SetWallpaperActivity;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.WallpaperCropper;
import com.hixos.smartwp.triggers.ServicesActivity;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.triggers.slideshow.SlideshowPickerFragment;
import com.hixos.smartwp.utils.DefaultWallpaperTile;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.widget.AnimatedGridView;
import com.hixos.smartwp.widget.ArrowView;
import com.hixos.smartwp.widget.ErrorDialogFragment;
import com.hixos.smartwp.widget.ProgressDialogFragment;
import com.hixos.smartwp.widget.UndoBarController;

import java.util.ArrayList;
import java.util.List;

public class GeofenceFragment extends Fragment implements UndoBarController.UndoListener,
        GeofenceDatabase.OnElementRemovedListener, DefaultWallpaperTile.DefaultWallpaperTileListener,
        View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static int REQUEST_SET_LIVE_WALLPAPER = 33;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    Handler handler = new Handler();
    private GeofenceDatabase mDatabase;
    private UndoBarController mUndoBarController;
    private AnimatedGridView mGridView;
    private long mUndobarShownTimestamp = -1;
    private GeofencePickerFragment mPickerFragment;
    private View mEmptyState;
    private ArrowView mArrowView;
    private boolean mEmptyStateAnimated = false;
    private View mErrorsLayout;
    private View mErrorFrameWifi;
    private View mErrorFrameNetwork;
    private int mDefaultPaddingTop;
    private GoogleApiClient mGoogleClient;
    private boolean mSetWallpaperActivityVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mDatabase = new GeofenceDatabase(getActivity());
        mDatabase.clearDeletedGeofences();
        mDatabase.setOnElementRemovedListener(this);
        mDatabase.clearDeletedGeofencesAsync();

        mPickerFragment = new GeofencePickerFragment();
        mPickerFragment.setDatabase(mDatabase);

        FragmentManager fragmentManager = getActivity().getFragmentManager();
        fragmentManager.beginTransaction().remove(fragmentManager.findFragmentByTag("wallpaper_picker"));
        fragmentManager.beginTransaction().add(mPickerFragment, "wallpaper_picker").commit();

        if (!getActivity().getIntent().getBooleanExtra(
                ServicesActivity.EXTRA_DISABLE_LWP_CHECK, false)) {
            checkLiveWallpaper();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_geofence, container, false);

        mGridView = (AnimatedGridView) view.findViewById(R.id.gridView);
        setAdapter();

        mErrorsLayout = view.findViewById(R.id.errorsLayout);
        mErrorFrameWifi = view.findViewById(R.id.error_frame_wifi);
        mErrorFrameNetwork = view.findViewById(R.id.error_frame_network_location);

        view.findViewById(R.id.button_error_wifi).setOnClickListener(this);
        view.findViewById(R.id.button_error_network_location).setOnClickListener(this);

        int paddingTop = MiscUtils.UI.getActionBarHeight(getActivity());
        int paddingBottom = 0;

        if (MiscUtils.UI.addStatusBarPadding(getActivity())) {
            paddingTop += MiscUtils.UI.getStatusBarHeight(getActivity());
        }
        if (MiscUtils.UI.hasTranslucentNavigation(getActivity())) {
            paddingBottom += MiscUtils.UI.getNavBarHeight(getActivity());
        }

        mDefaultPaddingTop = mGridView.getPaddingTop() + paddingTop;

        mErrorsLayout.setPadding(mErrorsLayout.getPaddingLeft(),
                mErrorsLayout.getPaddingTop() + paddingTop,
                mErrorsLayout.getPaddingRight(),
                mErrorsLayout.getPaddingBottom());

        mGridView.setPadding(mGridView.getPaddingLeft(),
                mGridView.getPaddingTop() + paddingTop,
                mGridView.getPaddingRight(),
                mGridView.getPaddingBottom() + paddingBottom);

        LinearLayout undoBar = (LinearLayout) view.findViewById(R.id.undobar);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) undoBar.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
                params.bottomMargin + paddingBottom);

        undoBar.setLayoutParams(params);

        mUndoBarController = null;
        mUndoBarController = new UndoBarController(undoBar, this);
        restoreUndobar();

        initEmptyState(view);
        return view;
    }

    private void initEmptyState(View view) {
        if (mDatabase.getGeofenceCount() != 0 || GeofenceDatabase.hasDefaultWallpaper()) {
            mArrowView = null;
            mEmptyState = null;
            return;
        }

        mEmptyState = view.findViewById(R.id.emptyState);
        mEmptyState.setVisibility(View.VISIBLE);

        mArrowView = (ArrowView) view.findViewById(R.id.emptyStateArrow);
        final TextView textView = (TextView) view.findViewById(R.id.emptyStateText);

        final ViewTreeObserver viewTreeObserver = getActivity().getWindow().getDecorView().getViewTreeObserver();
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View menuButton = getActivity().findViewById(R.id.action_new_wallpaper);

                    if (menuButton != null) {
                        if (viewTreeObserver.isAlive()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                viewTreeObserver.removeOnGlobalLayoutListener(this);
                            } else {
                                viewTreeObserver.removeGlobalOnLayoutListener(this);
                            }
                        }

                        int[] actionLocation = new int[2];
                        menuButton.getLocationInWindow(actionLocation);

                        int[] textLocation = new int[2];
                        textView.getLocationInWindow(textLocation);
                        final Point start = new Point(textLocation[0] + textView.getWidth() / 2,
                                textLocation[1]);
                        final Point end = new Point(actionLocation[0] + menuButton.getWidth() / 2,
                                actionLocation[1] + (int) (menuButton.getHeight() * 1.5f));

                        if (mEmptyStateAnimated) {
                            mArrowView.setPoints(start, end, false);
                        } else {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mEmptyStateAnimated = true;
                                    mArrowView.setPoints(start, end, true);
                                }
                            }, 250);
                        }
                    }
                }
            });
        }
    }

    private void hideEmptyState() {
        if (mEmptyState != null) {
            mEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.name_geofence);
        showErrorFrames();
        mGoogleClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
        if (lastLocation != null) {
            mDatabase.setLastLocation(lastLocation);
        }
        ((GeofenceDatabaseAdapter) mGridView.getAdapter()).reloadData();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        getActivity(),
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleClient != null) {
            mGoogleClient.disconnect();
        }
        mGoogleClient = null;
    }

    private void showErrorFrames() {
        if (mDatabase.getGeofenceCount() > 0 || GeofenceDatabase.hasDefaultWallpaper()) {
            int paddingTop = mDefaultPaddingTop;

            boolean atLeastOneVisible = false;

            if (!MiscUtils.Location.networkLocationProviderEnabled(getActivity())) {
                mErrorsLayout.setVisibility(View.VISIBLE);
                mErrorFrameNetwork.setVisibility(View.VISIBLE);
                paddingTop += getResources().getDimensionPixelSize(R.dimen.error_frame_height);
                atLeastOneVisible = true;
            } else {
                mErrorFrameNetwork.setVisibility(View.GONE);
            }

            if (!MiscUtils.Location.wifiLocationEnabled(getActivity())) {
                mErrorsLayout.setVisibility(View.VISIBLE);
                mErrorFrameWifi.setVisibility(View.VISIBLE);
                paddingTop += getResources().getDimensionPixelSize(R.dimen.error_frame_height);
                atLeastOneVisible = true;
            } else {
                mErrorFrameWifi.setVisibility(View.GONE);
            }

            if (!atLeastOneVisible) {
                mErrorsLayout.setVisibility(View.GONE);
            }

            mGridView.setPadding(mGridView.getPaddingLeft(),
                    paddingTop,
                    mGridView.getPaddingRight(),
                    mGridView.getPaddingBottom());
        }
    }

    private void checkLiveWallpaper() {
        if (!MiscUtils.Activity.isLiveWallpaperActive(getActivity())
                && GeofenceDatabase.hasDefaultWallpaper()
                && !mSetWallpaperActivityVisible) {
            startActivityForResult(new Intent(getActivity(), SetWallpaperActivity.class),
                    REQUEST_SET_LIVE_WALLPAPER);
            mSetWallpaperActivityVisible = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.geofence, menu);

        menu.findItem(R.id.action_auto_crop).setChecked(Preferences.getBoolean(
                getActivity(), R.string.preference_auto_crop,
                getResources().getBoolean(R.bool.auto_crop_default_val)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_wallpaper:
                mPickerFragment.pickWallpaper(new WallpaperPickerFragment.OnWallpaperPickedCallback() {
                    @Override
                    public void onWallpaperPicked(String uid) {
                        hideEmptyState();
                        showErrorFrames();
                        List<String> uidList = new ArrayList<>();
                        uidList.add(uid);

                        GeofenceService.broadcastAddGeofence(getActivity(), uidList);
                    }

                    @Override
                    public void onWallpaperPickFailed(String uid, int reason) {
                        FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
                        //TODO: Show message
                    }
                }, mDatabase.getNewUid());
                break;
            case R.id.action_settings:
                break;
            case R.id.action_auto_crop:
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                Preferences.setBoolean(getActivity(), R.string.preference_auto_crop, !checked);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SET_LIVE_WALLPAPER:
                if (resultCode != Activity.RESULT_OK) {
                    getActivity().finish();
                }
                mSetWallpaperActivityVisible = false;
                break;
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), R.string.error_location_not_found, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void onLocationConnected(Location location) {
        mDatabase.setLastLocation(location);
        setAdapter();
    }

    public void setAdapter() {
        GeofenceDatabaseAdapter adapter = new GeofenceDatabaseAdapter(getActivity(), mDatabase, this);
        mGridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onUndo(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            GeofenceService.broadcastAddGeofence(getActivity(), mDatabase.getDeletedGeofences());
            mDatabase.restoreGeofencesAsync();
        }
    }

    @Override
    public void onHide(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            mDatabase.clearDeletedGeofencesAsync();
        }
    }

    private void showUndobar() {
        showUndobar(0);
    }

    private void showUndobar(int elapsed) {
        mUndobarShownTimestamp = System.currentTimeMillis();
        int quantity = mDatabase.getDeletedGeofencesCount();

        String message = getResources().getQuantityString(R.plurals.wallpaper_deleted, quantity, quantity);
        mUndoBarController.showUndoBar(message, 0, elapsed);
    }

    private void restoreUndobar() {
        if (mUndobarShownTimestamp > 0) {
            int elapsed = ((int) (System.currentTimeMillis() - mUndobarShownTimestamp));
            showUndobar(elapsed);
        }
    }

    @Override
    public void onElementRemoved(String uid) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showUndobar();
            }
        });
        List<String> uids = new ArrayList<>();
        uids.add(uid);
        GeofenceService.broadcastRemoveGeofence(getActivity(), uids);
        LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
        if (cache != null) {
            cache.remove(uid);
            cache.remove(GeofenceDatabase.getSnapshotUid(uid));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_error_wifi:
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                break;
            case R.id.button_error_network_location:
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                break;
        }
    }

    private void showErrorDialog(int errorCode) {
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                getActivity(),
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        if (errorDialog != null) {
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            errorFragment.setDialog(errorDialog);
            errorFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    getActivity().finish();
                }
            });
            errorFragment.show(getFragmentManager(), "services_error");

        }
    }

    @Override
    public void onDefaultWallpaperEmptyStateClick(View view) {
        changeDefaultWallpaper();
    }

    @Override
    public void changeDefaultWallpaper() {
        mPickerFragment.pickDefaultWallpaper(new WallpaperPickerFragment.OnWallpaperPickedCallback() {
            @Override
            public void onWallpaperPicked(String uid) {
                LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
                if (cache != null) {
                    cache.remove(GeofenceDatabase.DEFAULT_WALLPAPER_UID);
                }
                mGridView.getAdapter().notifyDataSetChanged();
                GeofenceService.broadcastReloadWallpaper(getActivity());
                checkLiveWallpaper();
            }

            @Override
            public void onWallpaperPickFailed(String uid, int reason) {

            }
        });
    }

    private class GeofenceEditor implements BitmapIO.OnImageCroppedCallback {
        public static final int REQUEST_PICK_GEOFENCE = 0;
        public static final int REQUEST_PICK_WALLPAPER = 1;
        public static final int REQUEST_PICK_CROP_WALLPAPER = 2;
        public static final int REQUEST_CHANGE_GEOFENCE = 3;
        public static final int REQUEST_CHANGE_WALLPAPER = 4;
        public static final int REQUEST_CHANGE_CROP_WALLPAPER = 5;
        public boolean isPickingDefaultWallpaper = false;
        private boolean mPicking = false;
        private String mCurrentUid;
        private int mAutoCropRequest;

        private void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_PICK_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK)
                        continuePickGeowallpaperOne(data);
                    else
                        mPicking = false;
                    break;
                case REQUEST_PICK_CROP_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK)
                        continuePickGeowallpaperTwo();
                    else {
                        failedPickGeowallpaper();
                        mPicking = false;
                    }
                    break;
                case REQUEST_PICK_GEOFENCE:
                    if (resultCode == Activity.RESULT_OK)
                        finishPickGeowallpaper(data);
                    else {
                        failedPickGeowallpaper();
                        mPicking = false;
                    }
                    break;
                case REQUEST_CHANGE_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK)
                        continueChangeWallpaper(data);
                    else {
                        mPicking = false;
                        isPickingDefaultWallpaper = false;
                    }
                    break;
                case REQUEST_CHANGE_CROP_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK)
                        finishChangeWallpaper();
                    else {
                        mPicking = false;
                        isPickingDefaultWallpaper = false;
                    }
                    break;
                case REQUEST_CHANGE_GEOFENCE:
                    if (resultCode == Activity.RESULT_OK)
                        finishChangeGeofence(data);
                    else
                        mPicking = false;
                    break;
            }
        }

        private void pickDefaultWallpaper() {
            if (mPicking)
                return;
            mPicking = true;
            Intent i = MiscUtils.Activity.galleryPickerIntent();
            startActivityForResult(i, REQUEST_PICK_WALLPAPER);
            mCurrentUid = GeofenceDatabase.DEFAULT_WALLPAPER_UID;
        }

        /**
         * Begins the procedure for picking a new geowallpaper
         * The user is prompted to select an image.
         */
        private void pickGeowallpaper() {
            if (mPicking)
                return;
            mPicking = true;
            Intent i = MiscUtils.Activity.galleryPickerIntent();
            startActivityForResult(i, REQUEST_PICK_WALLPAPER);
            mCurrentUid = mDatabase.getNewUid();
        }

        /**
         * First part of the geowallpaper picking procedure.
         * Determines how to crop the chosen image
         *
         * @param data
         */
        private void continuePickGeowallpaperOne(Intent data) {
            if (data == null || data.getData() == null) {
                Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail), Toast.LENGTH_LONG).show();
                mPicking = false;
                return;
            }

            Uri image = data.getData();
            if (mCurrentUid.equals(GeofenceDatabase.DEFAULT_WALLPAPER_UID)) {
                GeofenceDatabase.deleteDefaultWallpaper();
            }
            if (!Preferences.getBoolean(getActivity(), R.string.preference_auto_crop,
                    getResources().getBoolean(R.bool.auto_crop_default_val))) {
                //Manual cropping
                Intent i = new Intent(getActivity(), CropperActivity.class);
                i.putExtra(CropperActivity.EXTRA_IMAGE, image);
                i.putExtra(CropperActivity.EXTRA_OUTPUT,
                        ImageManager.getInstance().getPictureUri(mCurrentUid));
                startActivityForResult(i, REQUEST_PICK_CROP_WALLPAPER);
            } else {
                //Automatic cropping
                DialogFragment f = new ProgressDialogFragment();
                f.show(getFragmentManager(), "crop_progress");
                mAutoCropRequest = REQUEST_PICK_CROP_WALLPAPER;
                WallpaperCropper.autoCropWallpaper(getActivity(), image,
                        ImageManager.getInstance().getPictureUri(mCurrentUid),
                        this);
            }
        }

        /**
         * Second part of the geowallpaper picking procedure
         * The user is prompted to choose a location for the geowallpaper
         */
        private void continuePickGeowallpaperTwo() {
            if (mCurrentUid.equals(GeofenceDatabase.DEFAULT_WALLPAPER_UID)) {
                LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
                if (cache != null) {
                    cache.remove(GeofenceDatabase.DEFAULT_WALLPAPER_UID);
                }
                ((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
                GeofenceService.broadcastReloadWallpaper(getActivity());
                checkLiveWallpaper();
                mPicking = false;
                return;
            }

            Intent i = new Intent(getActivity(), GeofencePickerActivity.class);
            i.putExtra(GeofencePickerActivity.EXTRA_UID, mCurrentUid);
            i.putExtra(GeofencePickerActivity.EXTRA_COLOR, mDatabase.getLeastUsedColor());
            i.putParcelableArrayListExtra(GeofencePickerActivity.EXTRA_GEOFENCES,
                    mDatabase.getGeofencesByDistance());
            startActivityForResult(i, REQUEST_PICK_GEOFENCE);
        }

        /**
         * Last part of the geowallpaper picking procedure
         * Stores the data into the database
         */
        private void finishPickGeowallpaper(Intent data) {
            if (data == null) {
                Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail), Toast.LENGTH_LONG).show();
                mPicking = false;
                return;
            }

            double latitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LATITUDE, 45);
            double longitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LONGITUDE, 8);
            float radius = data.getFloatExtra(GeofencePickerActivity.RESULT_RADIUS, 1);
            float distance = data.getFloatExtra(GeofencePickerActivity.RESULT_DISTANCE, -1);
            float zoom = data.getFloatExtra(GeofencePickerActivity.RESULT_ZOOM, 17);

            mDatabase.createGeowallpaper(mCurrentUid, new LatLng(latitude, longitude), radius,
                    mDatabase.getLeastUsedColor(), distance, zoom);

            hideEmptyState();
            showErrorFrames();
            mPicking = false;
            List<String> uid = new ArrayList<>();
            uid.add(mCurrentUid);

            GeofenceService.broadcastAddGeofence(getActivity(), uid);
        }

        /**
         * Called when the geofence picking failed
         */
        private void failedPickGeowallpaper() {
            Logger.w("GEO","Deleting unused picture " + mCurrentUid);
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(mCurrentUid));
        }

        /**
         * Begins the procedure for changing a wallpaper
         *
         * @param uid The uid of the geowallpaper to edit
         */
        private void beginChangeWallpaper(String uid) {
            if (mPicking)
                return;
            if (uid.equals(GeofenceDatabase.DEFAULT_WALLPAPER_UID)) {
                isPickingDefaultWallpaper = true;
            }
            mCurrentUid = uid;
            mPicking = true;
            Intent i = MiscUtils.Activity.galleryPickerIntent();
            startActivityForResult(i, REQUEST_CHANGE_WALLPAPER);
        }

        /**
         * First part of the wallpaper changing procedure.
         * Determines how to crop the chosen image
         *
         * @param data
         */
        private void continueChangeWallpaper(Intent data) {
            if (data == null || data.getData() == null) {
                Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail), Toast.LENGTH_LONG).show();
                mPicking = false;
                isPickingDefaultWallpaper = false;
                return;
            }
            Uri image = data.getData();
            if (!Preferences.getBoolean(getActivity(), R.string.preference_auto_crop,
                    getResources().getBoolean(R.bool.auto_crop_default_val))) {
                Intent i = new Intent(getActivity(), CropperActivity.class);
                i.putExtra(CropperActivity.EXTRA_IMAGE, image);
                i.putExtra(CropperActivity.EXTRA_OUTPUT, ImageManager.getInstance().getPictureUri(mCurrentUid));

                startActivityForResult(i, REQUEST_CHANGE_CROP_WALLPAPER);
            } else {
                DialogFragment f = new ProgressDialogFragment();
                f.show(getFragmentManager(), "crop_progress");
                mAutoCropRequest = REQUEST_CHANGE_CROP_WALLPAPER;
                WallpaperCropper.autoCropWallpaper(getActivity(), image,
                        ImageManager.getInstance().getPictureUri(mCurrentUid),
                        this);
            }

        }

        /**
         * Last part of the geowallpaper picking procedure
         * Updates the edited data
         */
        private void finishChangeWallpaper() {
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(mCurrentUid));
            if (mCurrentUid.equals(GeofenceDatabase.DEFAULT_WALLPAPER_UID)) {
                isPickingDefaultWallpaper = false;
            }
            mPicking = false;
        }

        /**
         * Display the activity to change a geofence
         *
         * @param uid the uid of the geowallpaper
         */
        private void beginChangeGeofence(String uid) {
            if (mPicking)
                return;
            mPicking = true;

            Intent i = new Intent(getActivity(), GeofencePickerActivity.class);
            i.putExtra(GeofencePickerActivity.EXTRA_UID, uid);
            i.putExtra(GeofencePickerActivity.EXTRA_TARGET_GEOFENCE, mDatabase.getWallpaper(uid));
            i.putParcelableArrayListExtra(GeofencePickerActivity.EXTRA_GEOFENCES,
                    mDatabase.getGeofencesByDistance());
            startActivityForResult(i, REQUEST_CHANGE_GEOFENCE);
        }

        /**
         * Updates the data of the changed geowallpaper
         *
         * @param data
         */
        private void finishChangeGeofence(Intent data) {
            double latitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LATITUDE, 45);
            double longitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LONGITUDE, 8);
            float radius = data.getFloatExtra(GeofencePickerActivity.RESULT_RADIUS, 1);
            String uid = data.getStringExtra(GeofencePickerActivity.RESULT_UID);
            float distance = data.getFloatExtra(GeofencePickerActivity.RESULT_DISTANCE, -1);

            GeofenceData d = mDatabase.getWallpaper(uid);
            if (d != null) {
                d.setLongitude(longitude);
                d.setLatitude(latitude);
                d.setRadius(radius);
                d.setDistance(distance);

                mDatabase.updateGeofence(d);
            }
            mPicking = false;
        }

        /**
         * Removes a geowallpaper and displays the undobar
         *
         * @param uid
         */
        private void removeGeowallpaper(String uid) {
          /*  for(int i = 0; i < getCards().size(); i++) {
                if(getCards().get(i).getId() == uid) {
                    getCards().remove(i);
                    break;
                }
            }
            mDatabase.deleteGeowallpaper(uid);
            if(GeowallpaperServiceLauncher.isActive(getActivity()) && mDatabase.getGeowallpaperCount() == 0){
                ServiceManager.stopActiveService(getActivity());
                mStartService = false;
                refreshHeader();
                ((MainActivity)getActivity()).onServiceActivated(-1);
            }
            getAdapter().notifyDataSetChanged();
            showUndobar(uid);*/
        }

        /**
         * Instantly removes a geowallpaper. No way back, be careful!
         *
         * @param uid
         */
        private void instantRemoveGeowallpaper(String uid) {
          /*  mDatabase.deleteGeowallpaper(uid);
            mDatabase.clearDeletedGeowallpapers();
            refreshCards();
            if(GeowallpaperServiceLauncher.isActive(getActivity()) && mDatabase.getGeowallpaperCount() == 0){
                ServiceManager.stopActiveService(getActivity());
                mStartService = false;
                refreshHeader();
                ((MainActivity)getActivity()).onServiceActivated(-1);
            }*/
        }

        @Override
        public void onImageCropped(Uri croppedImage) {
            DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("crop_progress");
            if (f != null) {
                f.dismiss();
            }
            switch (mAutoCropRequest) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    continuePickGeowallpaperTwo();
                    break;
                case REQUEST_CHANGE_CROP_WALLPAPER:
                    finishChangeWallpaper();
                    break;
            }
        }

        @Override
        public void onImageCropFailed() {
            DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("crop_progress");
            if (f != null) {
                f.dismiss();
            }
            switch (mAutoCropRequest) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    failedPickGeowallpaper();
                    mPicking = false;
                    break;
                case REQUEST_CHANGE_CROP_WALLPAPER:
                    mPicking = false;
                    isPickingDefaultWallpaper = false;
                    break;
            }
            isPickingDefaultWallpaper = false;
        }
    }
}
