package com.appme.story.engine.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.DateFormat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkStateUtil {

    public static String TAG = NetworkStateUtil.class.getSimpleName();
    private NetworkStateUtil() {
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null
            && networkInfo.isConnected()
            && networkInfo.getState().equals(NetworkInfo.State.CONNECTED);
    }
    
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * generates an unique number that can be user as a view's id
     * @return
     */
    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    /**
     * Get time accordingly to the format of the device
     * @param context
     * @return time
     */
    public static String getCurrentTime(Context context) {
        Calendar c = Calendar.getInstance();
        int mode = c.get(Calendar.AM_PM);
        Date date = c.getTime();
        String time;

        if (DateFormat.is24HourFormat(context))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            time = sdf.format(date).toString();
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm");
            time = sdf.format(date).toString() + " " + (mode == 0 ? "AM" : "PM");
        }
        return time;
    }

    public static String getCurrentIP(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {

                try {
                    if (method.invoke(wifi).toString().equals("false")) {
                        WifiInfo wifiInfo = wifi.getConnectionInfo();
                        int ipAddress = wifiInfo.getIpAddress();
                        return (ipAddress & 0xFF) + "." +
                            ((ipAddress >> 8) & 0xFF) + "." +
                            ((ipAddress >> 16) & 0xFF) + "." +
                            ((ipAddress >> 24) & 0xFF);
                    } else if (method.invoke(wifi).toString().equals("true")) {
                        return "192.168.43.1";
                    }
                } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return "Unknown";
    }
}

