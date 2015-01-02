package com.hixos.smartwp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment
        implements android.preference.Preference.OnPreferenceClickListener{

    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        getPreferenceManager().setSharedPreferencesName("palette_preferences");
        addPreferencesFromResource(R.xml.preferences);
        findPreference(getString(R.string.preference_contact_me)).setOnPreferenceClickListener(this);
        Preference version = findPreference(getString(R.string.preference_version));
        String versionString = "";
        try{
            versionString = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException NNFE){
            NNFE.printStackTrace();
        }
        version.setTitle(String.format("%s %s", getString(R.string.app_name), versionString));
    }

    @Override
    public PreferenceManager getPreferenceManager() {
        return super.getPreferenceManager();
    }

    public boolean onPreferenceClick(Preference preference){
        if (preference.getKey().equals(getString(R.string.preference_contact_me))){
            Intent intent = new Intent("android.intent.action.SENDTO",
                    Uri.fromParts("mailto", "lucaerbetta105@gmail.com", null));
            intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.app_name));
            startActivity(Intent.createChooser(intent, getString(R.string.contact_me)));
            return true;
        } else{
            return false;
        }
    }
}
