package ua.techguardians.picknroll.location;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * Created on: Dec 24, 2015
 * Author: Antony Mykolaj
 */
// TODO Add some comments which explains how this class works
public interface LocationPermissionResolveAware extends ActivityCompat.OnRequestPermissionsResultCallback {
    
    interface LocationPermissionResolverListener {
        void onLocationPermissionsGranted();
    }
    
    void onRequestLocationPermissionsChallengeReceived(String[] permissionPack);
    void onNeedRationale(String[] permissions);
    void onNeverAskAgainCheckEnabled();
    void onCreate(AppCompatActivity activity, LocationPermissionResolverListener l);
    void onDestroy();
    @Override
    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
}
