package com.mt53bureau.picknroll.example;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import com.mt53bureau.picknroll.PermissionScreen;

public class MainActivity extends AppCompatActivity {

    private PermissionScreen mPermissionScreen;
    private AutoCompleteTextView mEditEmailAutoComplete;
    private EmailAdapter mEmailAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Prepare an AutoCompleteTextView
        mEditEmailAutoComplete = (AutoCompleteTextView) findViewById(R.id.edit_email);
        mEmailAdapter = new EmailAdapter(this);
        mEditEmailAutoComplete.setAdapter(mEmailAdapter);

        // Initialize a PermissionScreen object.
        // This must be done before first use of the object.
        PermissionScreen.setUp(this);
        mPermissionScreen = PermissionScreen.getInstance();
        mPermissionScreen.setForActivity(this);

        // Wire this activity's lifecycle with a PermissionScreen.
        // This must be done to prevent a memory leak.
        mPermissionScreen.onActivityCreate(this);

        // Now lets try to read contacts.
        // This will throw a SecurityException because a user haven't got READ_CONTACTS permission
        // confirmed yet.
        populateEmailsAutocomplete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Wire this activity's lifecycle with a PermissionScreen
        // This must be done to prevent a memory leak.
        mPermissionScreen.onActivityDestroy(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This must always be done to check if this Activity was started by a PermissionScreen
        // to resolve a permission.
        mPermissionScreen.checkIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Pass the results into PermissionScreen to examine.
        // This will check if a permission requested earlier is granted or not,
        // and will notify a listener about it.
        mPermissionScreen.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void populateEmailsAutocomplete() {
        final Set<String> emails = new HashSet<String>();
        final ContentResolver resolver = getContentResolver();
        try {
            final Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    final String columnName = ContactsContract.CommonDataKinds.Email.DATA;
                    final String email = cursor.getString(cursor.getColumnIndex(columnName));
                    emails.add(email);
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            mPermissionScreen.handlePermission(Manifest.permission.READ_CONTACTS, // A permission to resolve
                    Constants.CONTACT_PERMISSION_REQUEST_CODE,        // A request code to use to resolve this permission (must be unique among different permissions)
                    MainActivity.class,                               // Activity which should be launched to resolve this permission
                    new ContactPermissionResolverListenerImpl());     // A listener to be notified about protocol events
            return;
        }

        mEmailAdapter.clear();
        mEmailAdapter.addAll(emails);
    }

    /**
     * A listener to follow a permission resolve protocol
     */
    private class ContactPermissionResolverListenerImpl implements PermissionScreen.PermissionResolverListener {

        @Override
        public void onPermissionGranted(String permission) {
            // Required permission is confirmed.
            // Populate AutoCompleteTextView again.
            if (Manifest.permission.READ_CONTACTS.equals(permission)) {
                populateEmailsAutocomplete();
            }
        }

        @Override
        public void onPermissionNotGrantedAndNeverAskAgainChecked(String permission) {
            // TODO Show a dialog explaining why this activity needs a permission
        }

        @Override
        public void onNeedRationale(String permission) {

        }
    }

    /**
     * Adapter for showing email addresses in an AutoCompleteTextView
     */
    private static class EmailAdapter extends ArrayAdapter<String> {

        EmailAdapter(final Context context) {
            super(context, 0);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            final String item = getItem(position);
            final TextView label = (TextView) view.findViewById(android.R.id.text1);
            label.setText(item);
            return view;
        }
    }

}
