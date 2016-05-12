package ua.techguardians.picknroll.storage;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.vantagepnt.vptimetracker.permissionresolvers.location.LocationPermissionResolveAware;

/**
 * Created on: Dec 24, 2015
 * Author: Antony Mykolaj
 */
// TODO Add some comments which explains how this class works
public interface StorageAccessPermissionResolveAware extends ActivityCompat.OnRequestPermissionsResultCallback {
    
    interface StoragePermissionResolverListener {
        void onPermissionsGranted();
        void onPermissionsNotGranted();
    }
    
    void onRequestStoragePermissionsChallengeReceived(String[] permissionPack, StoragePermissionResolverListener l);
    void onNeedRationale(String[] permissions);
    void onNeverAskAgainCheckEnabled();
    void onCreate(AppCompatActivity activity, StoragePermissionResolverListener l);
    void onDestroy();
    @Override
    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
}
