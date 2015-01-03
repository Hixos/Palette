package com.hixos.smartwp.services.geofence;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;

public class GeofenceData implements Parcelable {
    public final static String[] DATA_COLUMNS = {
            GeofenceDatabase.COLUMN_DATA_ID, //0
            GeofenceDatabase.COLUMN_DATA_LATITUDE, //1
            GeofenceDatabase.COLUMN_DATA_LONGITUDE, //2
            GeofenceDatabase.COLUMN_DATA_RADIUS, //3
            GeofenceDatabase.COLUMN_DATA_DISTANCE, //4
            GeofenceDatabase.COLUMN_DATA_COLOR, //5
            GeofenceDatabase.COLUMN_DATA_DELETED, //6
            GeofenceDatabase.COLUMN_DATA_ZOOM_LEVEL, //7
            GeofenceDatabase.COLUMN_DATA_NAME //8
    };
    public static final Parcelable.Creator<GeofenceData> CREATOR
            = new Parcelable.Creator<GeofenceData>() {
        public GeofenceData createFromParcel(Parcel in) {
            return new GeofenceData(in);
        }

        public GeofenceData[] newArray(int size) {
            return new GeofenceData[size];
        }
    };
    private String mUid;
    private boolean mDeleted;
    private double mLatitude;
    private double mLongitude;
    private float mRadius;
    private int mColor;
    private float mDistance;
    private float mZoomLevel;
    private String mName;

    /**
     * Required for simple xml, DO NOT use that
     */
    public GeofenceData() {
        mDeleted = false;
    }

    public GeofenceData(Parcel in) {
        readFromParcel(in);
    }

    public static GeofenceData fromCursor(Cursor c) {
        GeofenceData d = new GeofenceData();
        d.mUid = c.getString(0);
        d.mLatitude = c.getDouble(1);
        d.mLongitude = c.getDouble(2);
        d.mRadius = c.getFloat(3);
        d.mDistance = c.getFloat(4);
        d.mColor = c.getInt(5);
        d.mDeleted = c.getInt(6) == 1;
        d.mZoomLevel = c.getFloat(7);
        d.mName = c.getString(8);
        return d;
    }

    //region Getters & Setters
    public float getDistance() {
        return mDistance;
    }

    public void setDistance(float distance) {
        this.mDistance = distance;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int mColor) {
        this.mColor = mColor;
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String mUid) {
        this.mUid = mUid;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public LatLng getLatLng() {
        return new LatLng(mLatitude, mLongitude);
    }

    public void setLatLng(LatLng coords) {
        mLatitude = coords.latitude;
        mLongitude = coords.longitude;
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float mRadius) {
        this.mRadius = mRadius;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean mDeleted) {
        this.mDeleted = mDeleted;
    }
//endregion

    public float getZoomLevel() {
        return mZoomLevel;
    }

    public void setZoomLevel(float zoomLevel) {
        this.mZoomLevel = zoomLevel;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public Geofence toGeofence() {
        return new Geofence.Builder()
                .setRequestId(mUid)
                .setCircularRegion(mLatitude, mLongitude, mRadius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //Remember to change readFromParcel if you edit this
        out.writeString(mUid);
        out.writeDouble(mLatitude);
        out.writeDouble(mLongitude);
        out.writeFloat(mRadius);
        out.writeFloat(mDistance);
        out.writeInt(mColor);
        out.writeByte((byte) (mDeleted ? 1 : 0));
        out.writeFloat(mZoomLevel);
        out.writeString(mName);
    }

    private void readFromParcel(Parcel in) {
        mUid = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mRadius = in.readFloat();
        mDistance = in.readFloat();
        mColor = in.readInt();
        mDeleted = in.readByte() == 1;
        mZoomLevel = in.readFloat();
        mName = in.readString();
    }
}
