package com.hixos.smartwp.services.geofence;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;

public class ProviderFixActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_fix);

        View locationLayout = findViewById(R.id.layout_location);
        View wifiLayout = findViewById(R.id.layout_wifi);
        locationLayout.setOnClickListener(this);
        wifiLayout.setOnClickListener(this);

        CheckBox checkBox = (CheckBox)findViewById(R.id.checkbox_dontshowagain);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
           checkBox.setVisibility(View.GONE);
        }else{
            checkBox.setOnClickListener(this);
            checkBox.setChecked(!Preferences.getBoolean(this,
                    R.string.preference_show_provider_error, true));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View locationLayout = findViewById(R.id.layout_location);
        View wifiLayout = findViewById(R.id.layout_wifi);
        boolean location = MiscUtils.Location.networkLocationProviderEnabled(this);
        boolean wifi = MiscUtils.Location.wifiLocationEnabled(this);
        if(location && wifi){
            finish();
        }else if(location){
            locationLayout.setVisibility(View.GONE);
        }else if(wifi){
            wifiLayout.setVisibility(View.GONE);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_wifi:
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
            case R.id.layout_location:
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                break;
            case R.id.checkbox_dontshowagain:
                CheckBox checkBox = (CheckBox)findViewById(R.id.checkbox_dontshowagain);
                Preferences.setBoolean(this, R.string.preference_show_provider_error,
                        !checkBox.isChecked());
        }
    }
}
