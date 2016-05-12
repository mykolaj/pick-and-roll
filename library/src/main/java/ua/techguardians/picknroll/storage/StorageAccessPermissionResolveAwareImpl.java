/**
 * Created on: Mar 15, 2016
 * Author: Antony Mykolaj
 */
package com.vantagepnt.vptimetracker.permissionresolvers.storage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import ua.techguardians.picknroll.storage.StorageAccessPermissionResolveAware;

public final class StorageAccessPermissionResolveAwareImpl implements StorageAccessPermissionResolveAware {
    
    private static final String TAG = StorageAccessPermissionResolveAwareImpl.class.getSimpleName();
    private StoragePermissionResolverListener mListener;
    private AppCompatActivity mActivity;
    
    @Override
    public void onRequestStoragePermissionsChallengeReceived(String[] permissionPack, StoragePermissionResolverListener l) {
        mListener = l;
        ActivityCompat.requestPermissions(mActivity, permissionPack,
                VpConstant.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSIONS);
    }

    
    private void onWriteExtStoragePermissionsGranted() {
        if (mListener != null) {
            mListener.onPermissionsGranted();
        }
    }

    @Override
    public void onNeedRationale(String[] permissions) {
        showPermissionsExplanationDialog(permissions);
    }

    @Override
    public void onNeverAskAgainCheckEnabled() {
        goToApplicationSettingsDialog();
        if (mListener != null) {
            mListener.onPermissionsNotGranted();
        }
    }

    @Override
    public void onCreate(AppCompatActivity activity, StoragePermissionResolverListener l) {
        mActivity = activity;
        mListener = l;
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case VpConstant.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSIONS:
                // If request is cancelled, the result arrays are empty.

                if (permissions == null || permissions.length == 0) {
                    return;
                }

                String permission = permissions[0];

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    onWriteExtStoragePermissionsGranted();
                    Trace.d(TAG + ": " + permission + " granted");
                } else {
                    // permission denied
                    Trace.d(TAG + ": " + permission + " denied");
                    boolean rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity,
                            permission);
                    if (rationale) {
                        // User maybe confused
                        onNeedRationale(permissions);
                    } else {
                        // Don't ask again checked
                        onNeverAskAgainCheckEnabled();
                    }
                }

                break;
        }
    }

    private void goToApplicationSettingsDialog() {
        String msg = "We've noticed that access to your storage is denied." +
                " To allow access to your location you need to proceed to application settings and" +
                " change your storage access policy in permissions section.";
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity, R.style.AppTheme_Dialog)
                .setMessage(msg)
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", mActivity.getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mActivity.startActivity(intent);
                    }
                })
                .create();
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
    }

    private void showPermissionsExplanationDialog(final String[] permissions) {
        String appName = mActivity.getString(R.string.app_name);
        String msg = "To download an update " + appName + " needs an access to your SD card.";
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity, R.style.AppTheme_Dialog)
                .setMessage(msg)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(mActivity, permissions,
                                VpConstant.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSIONS);
                    }
                }).create();
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
    }
    
}
