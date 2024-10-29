package com.zencity.cordova.bgloc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BackgroundGpsPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGpsPlugin";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";

    private Intent updateServiceIntent;

    private Boolean isEnabled = false;

    private String url;
    private String params;
    private String headers;
    private String stationaryRadius = "30";
    private String desiredAccuracy = "100";
    private String distanceFilter = "30";
    private String locationTimeout = "60";
    private String isDebugging = "false";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String stopOnTerminate = "false";

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2564654) {
            // Check if the background location permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now use background location
            } else {
                // Permission denied, handle this case (show a rationale, notify user, etc.)
            }
        }
    }


    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);

        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;
            if (params == null || headers == null || url == null) {
                callbackContext.error("Call configure before calling start");
            } else {

                if (ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION )
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION )
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this.cordova.getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
                Log.i(TAG,"ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION )"+ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION ));
                Log.i(TAG,"ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION )"+ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION ));
                Log.i(TAG,"ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION )"+ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION ));


                if (ContextCompat.checkSelfPermission(this.cordova.getActivity(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this.cordova.getActivity(),
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {

                    // Open app settings for user to manually enable background location permission
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", this.cordova.getActivity().getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Request background location permission only on Android 10 or higher
                    ActivityCompat.requestPermissions(this.cordova.getActivity(),
                            new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION }, 2564654);
                }

                Log.i(TAG,"ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION )"+ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION ));

                callbackContext.success();
                updateServiceIntent.putExtra("url", url);
                updateServiceIntent.putExtra("params", params);
                updateServiceIntent.putExtra("headers", headers);
                updateServiceIntent.putExtra("stationaryRadius", stationaryRadius);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("distanceFilter", distanceFilter);
                updateServiceIntent.putExtra("locationTimeout", locationTimeout);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("isDebugging", isDebugging);
                updateServiceIntent.putExtra("notificationTitle", notificationTitle);
                updateServiceIntent.putExtra("notificationText", notificationText);
                updateServiceIntent.putExtra("stopOnTerminate", stopOnTerminate);
                updateServiceIntent.setFlags(Intent.FLAG_FROM_BACKGROUND);

                activity.startService(updateServiceIntent);
                isEnabled = true;
            }
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                // Params.
                //    0       1       2           3               4                5               6            7           8                9               10              11
                //[params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
                this.params = data.getString(0);
                this.headers = data.getString(1);
                this.url = data.getString(2);
                this.stationaryRadius = data.getString(3);
                this.distanceFilter = data.getString(4);
                this.locationTimeout = data.getString(5);
                this.desiredAccuracy = data.getString(6);
                this.isDebugging = data.getString(7);
                this.notificationTitle = data.getString(8);
                this.notificationText = data.getString(9);
                this.stopOnTerminate = data.getString(11);
            } catch (JSONException e) {
                callbackContext.error("authToken/url required as parameters: " + e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        }

        return result;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
        }
    }
}
