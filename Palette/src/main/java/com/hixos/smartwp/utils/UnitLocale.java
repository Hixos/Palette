package com.hixos.smartwp.utils;

import java.util.Locale;

/**
 * Created by Luca on 18/12/13.
 */
public class UnitLocale {
    public static UnitLocale Imperial = new UnitLocale();
    public static UnitLocale Metric = new UnitLocale();

    public static UnitLocale getDefault() {
        return getFrom(Locale.getDefault());
    }
    public static UnitLocale getFrom(Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode)) return Imperial; // USA
        if ("LR".equals(countryCode)) return Imperial; // liberia
        if ("MM".equals(countryCode)) return Imperial; // burma
        return Metric;
    }

    public static float toMeters(float yards){
        return yards * 0.9144f;
    }

    public static float toYards(float meters){
        return meters / 0.9144f;
    }
}