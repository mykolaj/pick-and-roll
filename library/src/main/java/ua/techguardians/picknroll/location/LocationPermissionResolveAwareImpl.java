/**
 * Created on: Dec 24, 2015
 * Author: Antony Mykolaj
 */
package com.vantagepnt.vptimetracker.permissionresolvers.location;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.vantagepnt.vptimetracker.R;
import com.vantagepnt.vptimetracker.Trace;
import com.vantagepnt.vptimetracker.core.VpConstant;

public class LocationPermissionResolveAwareImpl implements LocationPermissionResolveAware {

    private static final String TAG = LocationPermissionResolveAwareImpl.class.getSimpleName();


    private AppCompatActivity mActivity;
    private LocationPermissionResolverListener mPermissionResolverListener;

    @Override
    public void onRequestLocationPermissionsChallengeReceived(String[] permissionPack) {
        ActivityCompat.requestPermissions(mActivity, permissionPack,
                VpConstant.REQUEST_CODE_ASK_LOCATION_PERMISSIONS);
    }

    private void onLocationPermissionsGranted() {
        if (mPermissionResolverListener != null) {
            mPermissionResolverListener.onLocationPermissionsGranted();
        }
    }

    @Override
    public void onNeedRationale(String[] permissions) {
        showLocationPermissionsExplanationDialog(permissions);
    }

    @Override
    public void onNeverAskAgainCheckEnabled() {
        goToApplicationSettingsDialog();
    }

    @Override
    public void onCreate(final AppCompatActivity activity,
            LocationPermissionResolverListener l) { // TODO Make it more base class `ish
        mActivity = activity;
        mPermissionResolverListener = l;
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        mPermissionResolverListener = null;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
            final int[] grantResults) {
        switch (requestCode) {
            case VpConstant.REQUEST_CODE_ASK_LOCATION_PERMISSIONS:
                // If request is cancelled, the result arrays are empty.

                if (permissions == null || permissions.length == 0) {
                    return;
                }

                String permission = permissions[0];

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    onLocationPermissionsGranted();
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
        String msg = "We've noticed that access to your location is denied." +
                " To allow access to your location you need to proceed to application settings and change your location" +
                " policy in permissions section.";
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

    private void showLocationPermissionsExplanationDialog(final String[] permissions) {
        String appName = mActivity.getString(R.string.app_name);
        String msg = "To record your work time properly " + appName + " needs an access to your location.";
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity, R.style.AppTheme_Dialog)
                .setMessage(msg)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(mActivity, permissions,
                                VpConstant.REQUEST_CODE_ASK_LOCATION_PERMISSIONS);
                    }
                })
                .create();
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
    }

}
