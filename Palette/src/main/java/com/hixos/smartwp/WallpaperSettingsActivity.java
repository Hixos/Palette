package com.hixos.smartwp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.hixos.smartwp.services.ServiceUtils;
import com.hixos.smartwp.services.ServicesActivity;

public class WallpaperSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ServicesActivity.class);
        intent.putExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK, true);
        int active = ServiceUtils.getActiveService(this);
        if(active != 0) {
            intent.putExtra(ServicesActivity.EXTRA_SERVIVE, active);
        }else{
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
