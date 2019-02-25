package com.eventmanagerapp.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.eventmanagerapp.classes.ApplicationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class BaseActivity extends AppCompatActivity {

    protected static ProgressDialog mProgressDialog;
    protected AlertDialog dialog;
    protected Timer timer;
    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            timerEvent();
        }
    };
    public ApplicationManager appManager;
    private int yesNoAlertAction = 0;
    public static BaseActivity baseActivity;
    // Declaring a Location Manager
    LocationManager locationManager;

    public static BaseActivity getInstance() {
        return baseActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set Background Color
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        appManager = (ApplicationManager) getApplicationContext();

        baseActivity = this;

        Log.w("Navigation", "onCreate event: " + this.getClass().getName());
    }

    public static void alignToolBarTitle(final Toolbar toolbar, final TextView textView) {
        //setup width of custom title to match in parent toolbar
        toolbar.postDelayed(new Runnable() {
            @Override
            public void run() {
                int maxWidth = toolbar.getWidth();
                int titleWidth = textView.getWidth();
                int iconWidth = maxWidth - titleWidth;

                if (iconWidth > 0) {
                    //icons (drawer, menu) are on left and right side
                    int width = maxWidth - iconWidth * 2;
                    textView.setMinimumWidth(width);
                    textView.getLayoutParams().width = width;
                }
            }
        }, 0);
    }

    public static void showErrorMessage(Context context, String message) {
        //setup width of custom title to match in parent toolbar
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public String getRupeeSymbol() {
        String string = "\u20B9";
        byte[] utf8;
        try {
            utf8 = string.getBytes("UTF-8");
            string = new String(utf8, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return string;
    }

    public static void showProgressDialog(Context context) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle("Loading ....");
        mProgressDialog.setMessage("Please Wait");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    public static void stopProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

   /* public void showActivityIndicator() {
        ProgressModal.activityViewForView(this);
    }

    public void dismissActivityIndicator() {
        ProgressModal.removeViewAnimated();
    }*/

    public boolean isNetworkAvailable(View view, Context context, boolean showSnackBar) {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork;
        if (connectivityManager != null) {
            activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                connected = true;
            }
        }
        return connected;
    }

    public String parseTaskCreatedDate(String createdDate)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date currentDate = new Date();
        String createdAt = "";

        try {
            Date d1 = simpleDateFormat.parse(simpleDateFormat.format(currentDate));
            Date d2 = simpleDateFormat.parse(createdDate);

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            if (diffDays > 365)
                return  (diffDays / 365) + " Years ago";
            if (diffDays > 30)
                return  (diffDays / 30) + " Months ago";
            if (diffDays > 0)
                return diffDays + " Days ago";
            if (diffHours > 0)
                return diffHours + " Hours ago";
            if (diffMinutes > 0)
                return diffMinutes + " Minutes ago";
            if (diffSeconds > 0)
                return diffSeconds + " Seconds ago";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return createdAt;
    }

    public long parseAndReturnTime(String createdDate)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm aa");

        Date date = null;
        try {
            date = simpleDateFormat.parse(createdDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    public String parseTaskDueDate(String dueDate)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date currentDate = new Date();
        String createdAt = "";

        try {
            Date d1 = simpleDateFormat.parse(simpleDateFormat.format(currentDate));
            Date d2 = simpleDateFormat.parse(dueDate);

            if (d2.getTime() < d1.getTime())//Return date as it is if old date
                return dueDate;

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            if (diffDays > 365)
                return  (diffDays / 365) + " Years";
            if (diffDays >= 30)
                return  (diffDays / 30) + " Months";
            if (diffDays >= 0)
            {
                if (diffDays == 1)
                    return "Tomorrow";
                else if (diffDays == 0)
                    return "Today";
                else
                   return diffDays + " Days";
            }
            if (diffHours > 0)
                return "Today";
            if (diffMinutes > 0)
                return "Today";
            if (diffSeconds > 0)
                return "Today";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return createdAt;
    }

    protected void startTimer(String name, Integer delay) {
        Log.w("Timer", "Request to Start with name: " + name + " and delay " + delay.toString());

        // Make it final so it is usable inside the callback
        final String _n = name;

        if (timer != null) {
            Log.w("Timer", "Timer already running. Stopping it.");
            timer.cancel();
            timer = null;
        }

        timer = new Timer(_n);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerInternalEvent(_n);
            }

        }, delay, delay);
    }

    protected void timerInternalEvent(String name) {
        Log.w("Timer", "tick event for " + name);
        timer.cancel();
        timer = null;
        this.runOnUiThread(Timer_Tick);
    }

    public void timerEvent() {
        Log.w("Timer", "BaseActivity method not overwritten: timerEvent()");
    }

    protected void showRequestErrorAlertDialog(JSONObject object) {
//        String title = getString(R.string.http_request_error_title);
        String title = "";
        String msg = "";
//        String msg = getString(R.string.http_request_error_message);

        if (object != null) {

            try {
                if (object.getString("message") != null)
                    msg = object.getString("message");

                if (object.getString("title") != null)
                    title = object.getString("title");

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

//        showOKAlertDialog(title, msg, getString(R.string.ok));
    }

    protected void showOKAlertDialog(String title, String message, String ok) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });

        dialog = builder.create();
        dialog.show();
    }


    protected void showOKAlertDialog(String title, String message, String ok, int action) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        yesNoAlertAction = action;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
                yesNoAlertDialogPositiveResponse(yesNoAlertAction);
            }
        });

        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }

    // method overloading when called without the action param
    protected void showYesNoAlertDialog(String title, String message, String yesLabel, String noLabel) {
        showYesNoAlertDialog(title, message, yesLabel, noLabel, 0);
    }

    protected void showYesNoAlertDialog(String title, String message, String yesLabel, String noLabel, int action) {
        Log.w("Alert (Y|N)", "showYesNoAlertDialog: " + title + " - action: " + action);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        yesNoAlertAction = action;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(yesLabel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
                yesNoAlertDialogPositiveResponse(yesNoAlertAction);
            }
        });
        builder.setNegativeButton(noLabel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
                yesNoAlertDialogNegativeResponse(yesNoAlertAction);
            }
        });

        dialog = builder.create();
        dialog.show();

//        Button buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
//        buttonPositive.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    protected void showYesNoAlertDialog(String title, String message, String yesLabel, String noLabel, int action, String calledFrom) {
        Log.w("Alert (Y|N)", "showYesNoAlertDialog: " + title + " - action: " + action);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        yesNoAlertAction = action;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(yesLabel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
                yesNoAlertDialogPositiveResponse(yesNoAlertAction);
            }
        });
        builder.setNegativeButton(noLabel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null)
                    dialog.dismiss();
                yesNoAlertDialogNegativeResponse(yesNoAlertAction);
            }
        });


        dialog = builder.create();
        dialog.show();
    }

    public void yesNoAlertDialogPositiveResponse(int action) {
        Log.w("Alert (Y|N)", "yesNoAlertDialogPositiveResponse");
    }

    public void yesNoAlertDialogNegativeResponse(int action) {
        Log.w("Alert (Y|N)", "yesNoAlertDialogNegativeResponse");
    }

    // Overwrite Toolbar back button to control stack operations.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                hideSoftKeyboard();
                onBackPressed();
                return true;
        }

        return false;
    }

    /*
    protected void hideSoftKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Log.w("Keyboard", "Try to hide soft keyboard");
    }
    */
    public void hideSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (this.getCurrentFocus() != null && this.getCurrentFocus().getWindowToken() != null) {
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void closeKeyboard(View view) {
        Log.w("UI Event", "Close Keyboard request");
        hideSoftKeyboard();
    }

    final int CALL_PERMISSION_ID = 1;
    final int LOCATION_PERMISSION_ID = 2;


    protected boolean callPhoneNumber(String number) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_ID);
            return false;
        }
        // Permission was granted. Make the phone call.
//        Intent callIntent = new Intent(Intent.ACTION_CALL);
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + number));
        startActivity(callIntent);
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // callback method when permissions are requested at run time.
        switch (requestCode) {
            case CALL_PERMISSION_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    callPhoneNumber();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case LOCATION_PERMISSION_ID: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
//                    onLocationResult(null);
                }
                return;            // other 'case' lines to check for other
                // permissions this app might request
            }
        }
    }


    public static boolean isAndroidMOrLast() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public void storeStringInUserDefaults(String key, String value) {
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences.Editor editor = getUserDefaultEditor();
        editor.putString(key, value);
        editor.commit();
    }

    public void storeBooleanInUserDefaults(String key, Boolean value) {
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences.Editor editor = getUserDefaultEditor();
        editor.putBoolean(key, value);
        editor.commit();
    }


    public String getStringFromUserDefaults(String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String value = sharedPref.getString(key, "");
        return value;
    }

    public Boolean getBooleanFromUserDefaults(String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean value = sharedPref.getBoolean(key, false);
        return value;
    }


    private SharedPreferences.Editor getUserDefaultEditor() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        return editor;
    }

    //To get current version of app
    public String getAppVersion() {
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }
}