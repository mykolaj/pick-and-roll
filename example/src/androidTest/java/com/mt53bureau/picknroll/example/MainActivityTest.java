package com.mt53bureau.picknroll.example;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.Until;
import android.support.v4.content.ContextCompat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.action.ViewActions.click;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.uiautomator.UiDevice;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class MainActivityTest {

    private static final String TAG = MainActivityTest.class.getSimpleName();
    public static final int MARSHMALLOW_SDK = 23;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    private static final int WAIT_FOR_UI_TIMEOUT = 3000;


    @Test
    public void test_user_declines_contact_permission() {
        disableContactPermissionInSettings();
//        allowPermissionsIfNeeded(mActivityRule.getActivity(), Manifest.permission.READ_CONTACTS);
    }

    public void allowPermissionsIfNeeded(Activity activity, String permissionNeeded) {
        if (Build.VERSION.SDK_INT < MARSHMALLOW_SDK) {
            return;
        }
        if (hasNeededPermission(activity, permissionNeeded)) {
            return;
        }
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final BySelector allowBtnSelector = By.clazz(Button.class)
                .res("com.android.packageinstaller:id/permission_allow_button");
        final UiObject2 btnAllow = device.wait(Until.findObject(allowBtnSelector), WAIT_FOR_UI_TIMEOUT);
        if (btnAllow != null) {
            btnAllow.click();
        }
    }

    private void disableContactPermissionInSettings() {
        if (Build.VERSION.SDK_INT < MARSHMALLOW_SDK) {
            Log.w(TAG, "disableContactPermissionInSettings: skip runtime permission resolution test"
                    + " device's SDK is less than " + MARSHMALLOW_SDK);
            return;
        }

        startInstalledAppDetailsActivity(mActivityRule.getActivity());

        // Find Permission entry in a ListView
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final BySelector listViewSelector = By.clazz(ListView.class)
                .pkg("com.android.settings")
                .res("android:id/list");
        final UiObject2 listView = device.wait(Until.findObject(listViewSelector), WAIT_FOR_UI_TIMEOUT);

    }

    public void startInstalledAppDetailsActivity(final Activity context) {
        final Intent i = new Intent()
                .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setData(Uri.parse("package:" + context.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    private boolean hasNeededPermission(Activity activity, String permissionNeeded) {
        int permissionStatus = ContextCompat.checkSelfPermission(activity, permissionNeeded);
        return permissionStatus == PackageManager.PERMISSION_GRANTED;
    }

}