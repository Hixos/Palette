package com.hixos.smartwp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Geocoder {
    private static final String LOGTAG = "Geocoder";
    private static final String PREFERENCES_GEOCODER = Geocoder.class.getName()
            + ".GEOCODER";
    private static final String KEY_ALLOW = Geocoder.class.getName()
            + ".KEY_ALLOW";

    private static final String STATUS_OK = "OK";

    private static final String STATUS_OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";

    private final Context context;

    public Geocoder(Context context) {
        this.context = context;
    }

   /* public List<Address> getFromLocation(double latitude, double longitude,
                                         int maxResults) throws IOException, LimitExceededException
    {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude == " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude == " + longitude);
        }

        if (isLimitExceeded(context)) {
            throw new LimitExceededException();
        }

        final List<Address> results = new ArrayList<Address>();

        final StringBuilder url = new StringBuilder(
                "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&latlng=");
        url.append(latitude);
        url.append(',');
        url.append(longitude);
        url.append("&language=");
        url.append(Locale.getDefault().getLanguage());

        final byte[] data = WebserviceClient.download(url.toString());
        if (data != null) {
            this.parseJson(results, maxResults, data);
        }
        return results;
    }*/

    private static boolean isLimitExceeded(Context context) {
        return System.currentTimeMillis() <= getAllowedDate(context);
    }

    private static void setAllowedDate(Context context, long date) {
        final SharedPreferences p = context.getSharedPreferences(
                PREFERENCES_GEOCODER, Context.MODE_PRIVATE);
        final Editor e = p.edit();
        e.putLong(KEY_ALLOW, date);
        e.commit();
    }

    private static long getAllowedDate(Context context) {
        final SharedPreferences p = context.getSharedPreferences(
                PREFERENCES_GEOCODER, Context.MODE_PRIVATE);
        return p.getLong(KEY_ALLOW, 0);
    }

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName == null");
        }

        if (isLimitExceeded(context)) {
            return null;
        }

        final List<Address> results = new ArrayList<>();

        final StringBuilder request = new StringBuilder(
                "http://maps.googleapis.com/maps/api/geocode/json?sensor=false");
        request.append("&language=").append(Locale.getDefault().getLanguage());
        request.append("&address=").append(URLEncoder.encode(locationName, "UTF-8"));


        String data;
        try {
            data = readUrl(request.toString());
        } catch (Exception ex) {
            data = null;
        }

        if (data != null) {
            try {
                this.parseJson(results, maxResults, data);
            } catch (LimitExceededException e) {
                //LimitExceededException could be thrown if too many calls per second
                //If after two seconds, it is thrown again - then it means there are too much calls per 24 hours
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    return results;
                }

                try {
                    data = readUrl(request.toString());
                } catch (Exception ex) {
                    data = null;
                }
                if (data != null) {
                    try {
                        this.parseJson(results, maxResults, data);
                    } catch (LimitExceededException lee) {
                        // available in 24 hours
                        setAllowedDate(context, System.currentTimeMillis() + 86400000L);
                        return null;
                    }
                }
            }
        }
        return results;
    }

    private void parseJson(List<Address> address, int maxResults, String json)
            throws LimitExceededException {
        try {
            final JSONObject o = new JSONObject(json);
            final String status = o.getString("status");
            if (status.equals(STATUS_OK)) {

                final JSONArray a = o.getJSONArray("results");

                for (int i = 0; i < maxResults && i < a.length(); i++) {
                    final Address current = new Address(Locale.getDefault());
                    final JSONObject item = a.getJSONObject(i);

                    current.setFeatureName(item.getString("formatted_address"));
                    final JSONObject location = item.getJSONObject("geometry")
                            .getJSONObject("location");
                    current.setLatitude(location.getDouble("lat"));
                    current.setLongitude(location.getDouble("lng"));

                    address.add(current);
                }

            } else if (status.equals(STATUS_OVER_QUERY_LIMIT)) {

                throw new LimitExceededException();

            }
        } catch (LimitExceededException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public static final class LimitExceededException extends Exception {
    }
}