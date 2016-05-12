/**
 * Created on: Dec 24, 2015
 * Author: Antony Mykolaj
 */
package com.vantagepnt.vptimetracker.permissionresolvers.location;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.vantagepnt.vptimetracker.R;

public class LocationSettingsResolveAwareImpl implements LocationSettingsResolveAware {
    
    private AppCompatActivity mActivity;
    
    @Override
    public void onEnableLocationSettingChallengeReceived() {
        showSettingsProxyDialog();
    }

    @Override
    public void onCreate(final AppCompatActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onDestroy() {
        mActivity = null;
    }
    
    private void showSettingsProxyDialog() {
        String msg = "Access to your location is disabled in system settings";
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity, R.style.AppTheme_Dialog)
                .setMessage(msg)
                .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mActivity.startActivity(intent);
                    }
                })
                .create();
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
    }
    
}
