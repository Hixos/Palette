package com.hixos.smartwp.utils;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.R;

/**
 * Created by Luca on 17/11/13.
 */
public class GeoMath {

    private static int mUnitChange[] = {1000, 1760};

    public static String getDistanceUnitString(Context c, float distance, boolean abbreviate) {
        int dist = getDistanceInUnit(c, distance);
        int unit = 0;
        if(Preferences.getBoolean(c, R.string.preference_imperial_system, false)){
            unit = 1;
        }

        if(distance < mUnitChange[unit]) {
            if(unit == 0){
                return c.getResources().getQuantityString(R.plurals.meter, dist);
            }else{
                return c.getResources().getQuantityString(R.plurals.yard, dist);
            }
        }else{
            if(unit == 0){
                return c.getResources().getQuantityString(R.plurals.kilometer, dist);
            }else{
                return c.getResources().getQuantityString(R.plurals.mile, dist);
            }
        }
    }

    public static int getDistanceInUnit(Context context, float d) {
        int unit = 0;
        if(Preferences.getBoolean(context, R.string.preference_imperial_system, false)){
            unit = 1;
        }
        int c = mUnitChange[unit];
        if(d >= c){
            d = Math.round(d / (float) c);
        }

        for(int i = 10; i < 1000000; i *= 10){

            if(d < i){
                d = Math.round(d / (float) (i / 10)) * (float)(i / 10);
                break;
            }
        }
        return Math.round(d);
    }

    public static LatLng pointBearingDistance(LatLng center, double bearing, double distance)
    {
        double radiusEarthKilometres = 6371.01;
        double distRatio = (distance / 1000) / radiusEarthKilometres;
        double distRatioSine = Math.sin(distRatio);
        double distRatioCosine = Math.cos(distRatio);

        double startLatRad = Math.toRadians(center.latitude);
        double startLonRad = Math.toRadians(center.longitude);

        double startLatCos = Math.cos(startLatRad);
        double startLatSin = Math.sin(startLatRad);

        double endLatRads = Math.asin((startLatSin * distRatioCosine) + (startLatCos * distRatioSine * Math.cos(Math.toRadians(bearing))));

        double endLonRads = startLonRad
                + Math.atan2(
                Math.sin(Math.toRadians(bearing)) * distRatioSine * startLatCos,
                distRatioCosine - startLatSin * Math.sin(endLatRads));
        return new LatLng(Math.toDegrees(endLatRads), Math.toDegrees(endLonRads));
    }

    public static float getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        float[] f = new float[1];
        Location.distanceBetween(latitude1, longitude1, latitude2, longitude2, f);
        return f[0];
    }
}
