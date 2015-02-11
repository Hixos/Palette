package com.hixos.smartwp.utils;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.hixos.smartwp.R;
import com.hixos.smartwp.wallpaper.LiveWallpaperService;

public class MiscUtils {
    public static boolean usingGoogleNowLauncher(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        return res.activityInfo != null &&
                (res.activityInfo.packageName.equals("com.google.android.googlequicksearchbox")
                        || res.activityInfo.packageName.equals("com.google.android.launcher.GEL"));
    }

    public static class UI {

        /**
         * Returns the size of the display in pixels
         * The returned size is adjusted based on display rotation
         * @param context Current context
         * @return Point
         */
        public static Point getDisplaySize(Context context) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            Point screenSize = new Point();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                display.getRealSize(screenSize);
            else
                display.getSize(screenSize);

            return screenSize;
        }


        /**
         * Returns the height of the status bar
         *
         * @return height in pixels
         */
        public static int getStatusBarHeight(Context context) {
            int result = 0;
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }

        public static boolean addStatusBarPadding(Context context) {
            return hasTranslucentNavigation(context) || hasTranslucentStatus(context);
        }

        public static boolean hasTranslucentStatus(Context activity) {
            if (Build.VERSION.SDK_INT >= 19) {
                TypedArray a = activity.getTheme().obtainStyledAttributes(
                        R.style.Theme_DarkPalette_Material_Translucent,
                        new int[]{android.R.attr.windowTranslucentStatus});
                boolean out = a.getBoolean(0, false);
                a.recycle();
                return out;
            } else
                return false;
        }

        public static boolean hasTranslucentNavigation(Context activity) {
            if (Build.VERSION.SDK_INT >= 19) {
                TypedArray a = activity.getTheme().obtainStyledAttributes(
                        R.style.Theme_DarkPalette_Material_Translucent,
                        new int[]{android.R.attr.windowTranslucentNavigation});
                boolean out = a.getBoolean(0, false);
                a.recycle();
                return out;
            } else
                return false;
        }
        /**
         * Returns the height of the navigation bar
         *
         * @return height in pixels
         */
        public static int getNavBarHeight(Context context) {
            int result = 0;
            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }

        public static int getActionBarHeight(Context context) {
            int ab_height = 0;
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                ab_height = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            }
            return ab_height;
        }
    }

    public static class Activity {
        public static Intent galleryPickerIntent() {
            Intent intent;
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            return intent;
        }


        public static Intent liveWallpaperIntent(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(context, LiveWallpaperService.class));
                return intent;
            } else {
                Intent intent = new Intent();
                intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                return intent;
            }
        }

        public static void showLiveWallpaperActivity(Context context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                Toast.makeText(context,
                        context.getString(R.string.toast_live_wallpaper_chooser),
                        Toast.LENGTH_LONG).show();
            }

            Intent intent = liveWallpaperIntent(context);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

        public static boolean isLiveWallpaperActive(Context context) {
            if (context.getPackageName().endsWith("debug")
                    && Preferences.getBoolean(context,
                    R.string.preference_disable_wallpaper_not_set_bubble, false)) return true;

            WallpaperManager manager = WallpaperManager.getInstance(context);
            WallpaperInfo info = manager.getWallpaperInfo();
            return info != null && info.getPackageName().equals(context.getPackageName());
        }
    }

    public static class Location {
        public static boolean networkLocationProviderEnabled(Context c) {
            LocationManager m;
            m = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            return m.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        public static boolean wifiLocationEnabled(Context c) {
            WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return wifi.isScanAlwaysAvailable() || wifi.isWifiEnabled();
            } else {
                return wifi.isWifiEnabled();
            }
        }
    }
}
