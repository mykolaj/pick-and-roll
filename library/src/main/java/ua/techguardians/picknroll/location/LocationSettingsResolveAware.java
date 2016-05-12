package com.vantagepnt.vptimetracker.permissionresolvers.location;

import android.support.v7.app.AppCompatActivity;

/**
 * Created on: Dec 24, 2015
 * Author: Antony Mykolaj
 */
// TODO Add some comments which explains how this class works
public interface LocationSettingsResolveAware {
    void onEnableLocationSettingChallengeReceived();
    void onCreate(AppCompatActivity activity);
    void onDestroy();
}
