package com.appme.story.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ApplicationSettings {
    
    public static String TAG = ApplicationSettings.class.getSimpleName();
    private static final String DEFAULT_SERVER_PORT = "8080";
    private static final String DEFAULT_JPEG_QUALITY = "80";
    private static final String DEFAULT_CLIENT_TIMEOUT = "3000";

    private final SharedPreferences sharedPreferences;

    private boolean minimizeOnStream;
    private boolean pauseOnSleep;
    private volatile int serverPort;
    private volatile int jpegQuality;
    private volatile int clientTimeout;

    public ApplicationSettings(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        minimizeOnStream = sharedPreferences.getBoolean("minimize_on_stream", true);
        pauseOnSleep = sharedPreferences.getBoolean("pause_on_sleep", false);
        serverPort = Integer.parseInt(sharedPreferences.getString("port_number", DEFAULT_SERVER_PORT));
        jpegQuality = Integer.parseInt(sharedPreferences.getString("jpeg_quality", DEFAULT_JPEG_QUALITY));
        clientTimeout = Integer.parseInt(sharedPreferences.getString("client_connection_timeout", DEFAULT_CLIENT_TIMEOUT));
    }

    public boolean updateSettings() {
        minimizeOnStream = sharedPreferences.getBoolean("minimize_on_stream", true);
        pauseOnSleep = sharedPreferences.getBoolean("pause_on_sleep", false);

        jpegQuality = Integer.parseInt(sharedPreferences.getString("jpeg_quality", DEFAULT_JPEG_QUALITY));
        clientTimeout = Integer.parseInt(sharedPreferences.getString("client_connection_timeout", DEFAULT_CLIENT_TIMEOUT));

        final int newSeverPort = Integer.parseInt(sharedPreferences.getString("port_number", DEFAULT_SERVER_PORT));
        if (newSeverPort != serverPort) {
            serverPort = newSeverPort;
            return true;
        }

        return false;
    }

    public boolean isMinimizeOnStream() {
        return minimizeOnStream;
    }

    public boolean isPauseOnSleep() {
        return pauseOnSleep;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getJpegQuality() {
        return jpegQuality;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }
}
