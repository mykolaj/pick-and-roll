/*
 * Copyright (C) 2016 Antony Mykolaj, mykolaj (antony.mykolaj@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.techguardians.picknroll;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on: 5/12/16
 * Author:     A. Mykolaj
 * Project:    Pick'n'Roll
 */
public final class PermissionScreen implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ACTION_PERMISSION_PICK_RESOLVE_REQUEST = "action_permission_pick_resolve_request";
    private static final String PERMISSION_PICK_EXTRA_PERMISSION = "permission_pick_extra_permission";
    public static String LOG_TAG = "PermissionScreen";

    @SuppressLint("StaticFieldLeak")
    private static PermissionScreen instance;

    private final Queue<String> permissionResolveQueue = new LinkedList<String>();
    private final Map<String, Short> permissionToReqCodeMapping = new ConcurrentHashMap<String, Short>();
    private final Map<Short, String> permissionToReqCodeReverseMapping = new ConcurrentHashMap<Short, String>();
    private final Map<String, Class<? extends Activity>> permissionToActivityClassMapping = new ConcurrentHashMap<String, Class<? extends Activity>>();
    private final Set<Activity> createdActivities = Collections.synchronizedSet(new HashSet<Activity>());
    private final Map<String, Set<PermissionResolverListener>> listenerMapping = new ConcurrentHashMap<String, Set<PermissionResolverListener>>();
    private final Context appContext;

    public static PermissionScreen getInstance() {
        if (instance == null) {
            throw new IllegalStateException(LOG_TAG + ": setUp(Context context) method must be " +
                    "called before the first usage of " + PermissionScreen.class.getSimpleName());
        }
        return instance;
    }

    public static PermissionScreen setUp(Context context) {
        if (instance == null) {
            synchronized (PermissionScreen.class) {
                if (instance == null) {
                    instance = new PermissionScreen(context);
                }
            }
        }
        return instance;
    }

    /* Should not be used in a client code */
    private PermissionScreen(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void handlePermission(String permission, short requestCode, Class<? extends Activity> activityClass, PermissionResolverListener l) {
        // TODO Check if a permission is already in a queue and skip it
        final int result = ContextCompat.checkSelfPermission(this.appContext, permission);
        if (PackageManager.PERMISSION_GRANTED == result) {
            // Skip this permission because it's granted already
            if (l != null) {
                l.onPermissionGranted(permission);
            }
            return;
        }
        permissionResolveQueue.add(permission);
        permissionToReqCodeMapping.put(permission, requestCode);
        permissionToReqCodeReverseMapping.put(requestCode, permission);
        permissionToActivityClassMapping.put(permission, activityClass);
        if (l != null) {
            Set<PermissionResolverListener> listeners = listenerMapping.get(permission);
            if (listeners == null) {
                listeners = new HashSet<PermissionResolverListener>();
                listenerMapping.put(permission, listeners);
            }
            listeners.add(l);
        }
        // TODO Check if any permission resolve protocol is active at the moment before trying to start the next one
        resolveNext();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        final String permission = permissionToReqCodeReverseMapping.get((short)requestCode);
        for (int i = 0; i < permissions.length; i++) {
            final String p = permissions[i];
            if (p.equals(permission)) {
                final int result = grantResults[i];
                if (result == PackageManager.PERMISSION_GRANTED) {
                    notifyPermissionGranted(permission);
                    // Resolve a next permission if there is any
                    resolveNext();
                } else {
                    final Activity activity = getActivityForPermission(permission);
                    if (activity == null) {
                        Log.e("Pick'n'Roll", "No activity registered to resolve permission \"" + permission + "\"");
                        return;
                    }
                    boolean rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
                    if (rationale) {
                        // User maybe confused
                        notifyNeedRationale(permission);
                    } else {
                        // Don't ask again checked
                        notifyPermissionDeniedAndNeverAskAgainChecked(permission);
                    }
                }
            }
        }
    }

    private void notifyNeedRationale(final String permission) {
        final List<PermissionResolverListener> listeners = getListenersForPermission(permission);
        if (!listeners.isEmpty()) {
            synchronized (this.listenerMapping) {
                for (final PermissionResolverListener listener : listeners) {
                    listener.onNeedRationale(permission);
                }
            }
        }
    }

    private void notifyPermissionDeniedAndNeverAskAgainChecked(final String permission) {
        final List<PermissionResolverListener> listeners = getListenersForPermission(permission);
        if (!listeners.isEmpty()) {
            synchronized (this.listenerMapping) {
                for (final PermissionResolverListener listener : listeners) {
                    listener.onPermissionNotGrantedAndNeverAskAgainChecked(permission);
                }
            }
        }
    }

    private void notifyPermissionGranted(final String permission) {
        final List<PermissionResolverListener> listeners = getListenersForPermission(permission);
        if (!listeners.isEmpty()) {
            synchronized (this.listenerMapping) {
                final Iterator<PermissionResolverListener> iterator = listeners.iterator();
                while (iterator.hasNext()) {
                    PermissionResolverListener listener = iterator.next();
                    listener.onPermissionGranted(permission);
                    iterator.remove();
                }
            }
        }
        // Remove this permission from a queue
        synchronized (permissionResolveQueue) {
            permissionResolveQueue.remove(permission);
        }
        final Short code = permissionToReqCodeMapping.get(permission);
        if (code != null) {
            permissionToReqCodeReverseMapping.remove(code);
        }
        permissionToReqCodeMapping.remove(permission);
        permissionToActivityClassMapping.remove(permission);
    }

    /**
     * [Lifecycle]
     * This method must be called from Activity.onCreate()
     * @param activity
     */
    public void onActivityCreate(Activity activity) {
        // "Remember" an activity when it's created. Maybe we will need it to resolve a permission later
        this.createdActivities.add(activity);
    }

    /**
     * [Lifecycle]
     * This method must be called from Activity.onDestroy()
     * @param activity
     */
    public void onActivityDestroy(Activity activity) {
        // Remove any held reference to an Activity
        createdActivities.remove(activity);
    }

    private void resolveNext() {
        final String permission = permissionResolveQueue.peek();
        if (permission == null) {
            return; // Queue is empty
        }
        startResolveProtocol(permission);
    }

    private void startResolveProtocol(@NonNull String permission) {
        //noinspection ConstantConditions
        if (permission == null) {
            // Should not happen
            throw new IllegalStateException(LOG_TAG + ": permission must not be null");
        }
        final Activity activity = getActivityForPermission(permission);

        // In case a required activity is already created in a task we use it to resolve a permission.
        // In other case we start the required activity.
        if (activity != null) {
            final short requestCode = getRequestCodeForPermission(permission);
            if (requestCode == -1) {
                // Should not happen
                throw new IllegalStateException(LOG_TAG + ": request code must be specified");
            }
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        } else {
            final Class<? extends Activity> cls = permissionToActivityClassMapping.get(permission);
            final Intent intent = new Intent(appContext, cls)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setAction(ACTION_PERMISSION_PICK_RESOLVE_REQUEST)
                    .putExtra(PERMISSION_PICK_EXTRA_PERMISSION, permission);
            if (!Activity.class.isAssignableFrom(appContext.getClass())) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            appContext.startActivity(intent);
        }
    }

    @SuppressWarnings({"WhileLoopReplaceableByForEach"})
    private Activity getActivityForPermission(@NonNull String permission) {
        //noinspection ConstantConditions
        if (permission == null) {
            return null;
        }
        final Class<? extends Activity> cls = permissionToActivityClassMapping.get(permission);
        if (cls == null) {
            return null;
        }
        synchronized (this.createdActivities) {
            final Iterator<Activity> iterator = this.createdActivities.iterator();
            while (iterator.hasNext()) {
                final Activity a = iterator.next();
                if (a.getClass().equals(cls)) {
                    return a;
                }
            }
        }
        return null;
    }

    private short getRequestCodeForPermission(@NonNull String permission) {
        //noinspection ConstantConditions
        if (permission == null) {
            return -1;
        }
        final Short code = this.permissionToReqCodeMapping.get(permission);
        return code != null ? code : -1;
    }

    private List<PermissionResolverListener> getListenersForPermission(String permission) {
        final Set<PermissionResolverListener> listenerList = listenerMapping.get(permission);
        return listenerList != null ? new ArrayList<PermissionResolverListener>(listenerList) : Collections.<PermissionResolverListener>emptyList();
    }

    public void setForActivity(@NonNull AppCompatActivity activity) {
        final Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        checkIntent(intent);
    }

    public void checkIntent(@NonNull Intent intent) {
        //noinspection ConstantConditions
        if (intent == null) {
            return;
        }
        final String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (ACTION_PERMISSION_PICK_RESOLVE_REQUEST.equals(action)) {
            final String permission = intent.getStringExtra(PERMISSION_PICK_EXTRA_PERMISSION);
            if (permission == null) {
                // Should not happen
                throw new IllegalStateException(LOG_TAG + ": permission can not be 'null'");
            }
            startResolveProtocol(permission);
        }
    }

    /**
     * Allows to listen to a permission resolve protocol events
     */
    public interface PermissionResolverListener {

        /**
         * User grants a permission by pressing "Allow" button
         *
         * @param permission
         */
        void onPermissionGranted(String permission);

        /**
         * User checks a "Never ask again" checkbox and presses "Cancel" button
         *
         * @param permission
         */
        void onPermissionNotGrantedAndNeverAskAgainChecked(String permission);

        /**
         * User may be confused and presses "Cancel" button
         *
         * @param permission
         */
        void onNeedRationale(String permission);

    }
}
