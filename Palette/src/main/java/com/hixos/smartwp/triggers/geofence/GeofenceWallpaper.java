package com.hixos.smartwp.triggers.geofence;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.triggers.Wallpaper;

import java.security.InvalidParameterException;

/**
 * Created by Hixos on 13/09/2015.
 */
public class GeofenceWallpaper extends Wallpaper implements Parcelable {
    public static final String TABLE_NAME = "[geofence]";
    public static final Creator<Wallpaper> CREATOR = new Creator<Wallpaper>() {
        public GeofenceWallpaper createFromParcel(Parcel in) {
            return new GeofenceWallpaper(in);
        }

        public GeofenceWallpaper[] newArray(int size) {
            return new GeofenceWallpaper[size];
        }
    };
    private static final String COL_UID = "geofence_uid";
    public static final String COLUMN_UID = "[" + COL_UID + "]";
    private static final String COL_ACTIVE = "active";
    public static final String COLUMN_ACTIVE = "[" + COL_ACTIVE  + "]";
    private static final String COL_COLOR = "color";
    public static final String COLUMN_COLOR = "[" + COL_COLOR  + "]";
    private static final String COL_DISTANCE = "distance";
    public static final String COLUMN_DISTANCE = "[" + COL_DISTANCE  + "]";
    private static final String COL_LATITUDE = "latitude";
    public static final String COLUMN_LATITUDE = "[" + COL_LATITUDE  + "]";
    private static final String COL_LONGITUDE = "longitude";
    public static final String COLUMN_LONGITUDE = "[" + COL_LONGITUDE  + "]";
    private static final String COL_RADIUS = "radius";
    public static final String COLUMN_RADIUS = "[" + COL_RADIUS  + "]"; 
    private static final String COL_ZOOM_LEVEL = "zoom_level";
    public static final String COLUMN_ZOOM_LEVEL = "[" + COL_ZOOM_LEVEL  + "]";
    private double mLatitude;
    private double mLongitude;
    private float mRadius;
    private int mColor;
    private float mDistance;
    private float mZoomLevel;

    public GeofenceWallpaper(Parcel in) {
        super(in);
    }

    public GeofenceWallpaper(Cursor c) {
        super(c);
        int colLatitude  = c.getColumnIndex(COL_LATITUDE);
        int colLongitude = c.getColumnIndex(COL_LONGITUDE);
        int colRadius = c.getColumnIndex(COL_RADIUS);
        int colColor = c.getColumnIndex(COL_COLOR);
        int colDistance = c.getColumnIndex(COL_DISTANCE);
        int colZoomLevel = c.getColumnIndex(COL_ZOOM_LEVEL);

        if(colColor < 0 || colLatitude < 0 || colLongitude < 0 || colRadius < 0 ||
                colDistance < 0 || colZoomLevel < 0){
            throw new InvalidParameterException("Required columns not found");
        }

        mLatitude = c.getDouble(colLatitude);
        mLongitude = c.getDouble(colLongitude);
        mRadius = c.getFloat(colRadius);
        mColor = c.getInt(colColor);
        mDistance = c.getFloat(colDistance);
        mZoomLevel = c.getFloat(colZoomLevel);
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public LatLng getLatLng() {
        return new LatLng(mLatitude, mLongitude);
    }

    public float getRadius() {
        return mRadius;
    }

    public int getColor() {
        return mColor;
    }

    public float getDistance() {
        return mDistance;
    }

    public float getZoomLevel() {
        return mZoomLevel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        super.writeToParcel(out, i);

        out.writeDouble(mLatitude);
        out.writeDouble(mLongitude);
        out.writeFloat(mRadius);
        out.writeInt(mColor);
        out.writeFloat(mDistance);
        out.writeFloat(mZoomLevel);
    }

    @Override
    protected void readFromParcel(Parcel in) {
        super.readFromParcel(in);

        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mRadius = in.readFloat();
        mColor = in.readInt();
        mDistance = in.readFloat();
        mZoomLevel = in.readFloat();
    }

    public Geofence toGeofence() {
        return new Geofence.Builder()
                .setRequestId(getUid())
                .setCircularRegion(mLatitude, mLongitude, mRadius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }
}
