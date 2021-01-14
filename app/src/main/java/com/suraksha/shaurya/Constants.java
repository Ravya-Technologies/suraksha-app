package com.suraksha.shaurya;

import android.Manifest;
import android.app.Activity;
import android.content.Context;

import pub.devrel.easypermissions.EasyPermissions;

public class Constants {

    static boolean hasBothPermissions(Context context) {
        return EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
        );
    }


    static final int RQ = 1983;

    static void requestBothPermission(Activity activity) {
        EasyPermissions.requestPermissions(activity, "Please accept the location Permission", RQ,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS);
    }

    static boolean hasLocationPermission(Context context) {
        return EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
    }

    static boolean hasSMSPermission(Context context) {
        return EasyPermissions.hasPermissions(
                context,
                Manifest.permission.SEND_SMS
        );
    }
}
