package com.hixos.smartwp.triggers.geofence;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.hixos.smartwp.R;
import com.hixos.smartwp.SetWallpaperActivity;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.ServicesActivity;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.utils.DefaultWallpaperTile;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.widget.AnimatedGridView;
import com.hixos.smartwp.widget.ArrowView;
import com.hixos.smartwp.widget.ErrorDialogFragment;
import com.hixos.smartwp.widget.UndoBarController;

import java.util.ArrayList;
import java.util.List;

public class GeofenceFragment extends Fragment implements UndoBarController.UndoListener,
        DefaultWallpaperTile.DefaultWallpaperTileListener,
        View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final static int REQUEST_SET_LIVE_WALLPAPER = 33;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    Handler handler = new Handler();
    private GeofenceDB mDatabase;
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

    private GeofenceDatabaseAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mDatabase = new GeofenceDB(getActivity());
        //mDatabase.confirmDeletion();
        //mDatabase.setOnElementRemovedListener(this);
        mDatabase.confirmDeletion();

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
        if (mDatabase.getWallpaperCount() != 0 || GeofenceDB.hasDefaultWallpaper()) {
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
        if (mDatabase.getWallpaperCount() > 0 || GeofenceDB.hasDefaultWallpaper()) {
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
                && GeofenceDB.hasDefaultWallpaper()
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
                        if(reason != WallpaperPickerFragment.REASON_CANCELED) {
                            Toast.makeText(getActivity(), R.string.error_picture_pick_fail,
                                    Toast.LENGTH_LONG).show();
                        }
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
        mAdapter = new GeofenceDatabaseAdapter(getActivity(), mDatabase, this);
        mGridView.setAdapter(mAdapter);
        mAdapter.addOnWallpaperDeletedListener(new GeofenceDatabaseAdapter.OnWallpaperDeletedListener() {
            @Override
            public void onWallpaperDeleted(String uid) {
                showUndobar();
                LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
                if (cache != null) {
                    cache.remove(uid);
                }
            }
        });
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onUndo(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            GeofenceService.broadcastAddGeofence2(getActivity(), mDatabase.getDeletedWallpapers());
            mDatabase.undoDeletion();
            mAdapter.reloadData();
        }
    }

    @Override
    public void onHide(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            mDatabase.confirmDeletion();
        }
    }

    private void showUndobar() {
        showUndobar(0);
    }

    private void showUndobar(int elapsed) {
        mUndobarShownTimestamp = System.currentTimeMillis();
        int quantity = mDatabase.getDeletedWallpapersCount();

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
                    cache.remove(GeofenceDB.DEFAULT_WALLPAPER_UID);
                }
                FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
                mGridView.getAdapter().notifyDataSetChanged();
                GeofenceService.broadcastReloadWallpaper(getActivity());
                checkLiveWallpaper();
            }

            @Override
            public void onWallpaperPickFailed(String uid, int reason) {

            }
        });
    }
}
