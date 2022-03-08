package com.appme.story;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.View;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import com.appme.story.application.DebugActivity;
import com.appme.story.application.ApplicationMain;
import com.appme.story.application.ApplicationSettings;
import com.appme.story.engine.app.commons.connections.ScreenClient;
import com.appme.story.engine.app.utils.NetworkStateUtil;
import com.appme.story.engine.widget.AlertWindow;
import com.appme.story.engine.widget.CoverView;

public class AppController extends ApplicationMain {
    private static AppController isInstance;
    private static Context mContext;
    private ApplicationSettings applicationSettings;
    private WindowManager windowManager;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private int densityDPI;
    private String indexHtmlPage;
    private byte[] iconBytes;

    private final ConcurrentLinkedDeque<byte[]> JPEGQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedQueue<ScreenClient> clientQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean isStreamRunning;
    private volatile boolean isForegroundServiceRunning;
	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    
	@Override
	public void onCreate() {	
		super.onCreate();
        isInstance = this;
        mContext = this;
        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("error", getStackTrace(ex));
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(2);
                    uncaughtExceptionHandler.uncaughtException(thread, ex);
                }
            });
            
       applicationSettings = new ApplicationSettings(this);

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        densityDPI = getDensityDPI();
        indexHtmlPage = getIndexHTML();
        setFavicon();     
	}
    
    @Override
    public void initAnalytics() {
        super.initAnalytics();
    }

    @Override
    public void initCrashHandler() {
        super.initCrashHandler();
        
    }

    @Override
    public void initFolder() {
        super.initFolder();
    }

    @Override
    public void initSound() {
        super.initSound();
    }

    public static synchronized AppController getInstance() {
        return isInstance;
    }

    public static synchronized Context getAppContext() {
        return mContext;
    }

    public static ApplicationSettings getApplicationSettings() {
        return isInstance.applicationSettings;
    }

    public static WindowManager getWindowsManager() {
        return isInstance.windowManager;
    }

    public static MediaProjectionManager getProjectionManager() {
        return isInstance.projectionManager;
    }

    public static void setMediaProjection(final int resultCode, final Intent data) {
        isInstance.mediaProjection = isInstance.projectionManager.getMediaProjection(resultCode, data);
    }

    @Nullable
    public static MediaProjection getMediaProjection() {
        return isInstance.mediaProjection;
    }

    public static int getScreenDensity() {
        return isInstance.densityDPI;
    }

    public static float getScale() {
        return isInstance.getResources().getDisplayMetrics().density;
    }

    public static Point getScreenSize() {
        final Point screenSize = new Point();
        isInstance.windowManager.getDefaultDisplay().getRealSize(screenSize);
        return screenSize;
    }

    public static boolean isStreamRunning() {
        return isInstance.isStreamRunning;
    }

    public static void setIsStreamRunning(final boolean isRunning) {
        isInstance.isStreamRunning = isRunning;
    }

    public static boolean isForegroundServiceRunning() {
        return isInstance.isForegroundServiceRunning;
    }

    public static void setIsForegroundServiceRunning(final boolean isRunning) {
        isInstance.isForegroundServiceRunning = isRunning;
    }

    public static String getIndexHtmlPage() {
        return isInstance.indexHtmlPage;
    }

    public static byte[] getIconBytes() {
        return isInstance.iconBytes;
    }

    public static String getServerIP() {
        return "http://" + NetworkStateUtil.getCurrentIP(mContext) + ":" + isInstance.applicationSettings.getServerPort();
    }

    public static String getServerAddress() {
        return "http://" + isInstance.getIPAddress() + ":" + isInstance.applicationSettings.getServerPort();
    }

    public static ConcurrentLinkedDeque<byte[]> getJPEGQueue() {
        return isInstance.JPEGQueue;
    }

    public static ConcurrentLinkedQueue<ScreenClient> getClientQueue() {
        return isInstance.clientQueue;
    }

    public static boolean isWiFIConnected() {
        final WifiManager wifi = (WifiManager) isInstance.getSystemService(Context.WIFI_SERVICE);
        return wifi.getConnectionInfo().getNetworkId() != -1;
    }

    // Private methods
    private String getIPAddress() {
        final int ipInt = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return String.format(Locale.US, "%d.%d.%d.%d", (ipInt & 0xff), (ipInt >> 8 & 0xff), (ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
    }

    private int getDensityDPI() {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.densityDpi;
    }

    private String getIndexHTML() {
        final StringBuilder sb = new StringBuilder();
        String line;
        try {BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("index.html"), "UTF-8"));

            while ((line = reader.readLine()) != null) sb.append(line.toCharArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        final String html = sb.toString();
        sb.setLength(0);
        return html;
    }

    private void setFavicon() {
        try {
            InputStream inputStream = getAssets().open("favicon.png");
            iconBytes = new byte[inputStream.available()];
            inputStream.read(iconBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showLauncherView(Uri uri) {
        final AlertWindow alertWindow = new AlertWindow(this);
        CoverView view = new CoverView(this);
        view.setCancelClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertWindow.dismiss();
                }
            });
        view.showImage(uri);
        alertWindow.setContentView(view);
        alertWindow.show();
    }
    
	private String getStackTrace(Throwable th){
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		Throwable cause = th;
		while(cause != null){
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		final String stacktraceAsString = result.toString();
		printWriter.close();
		return stacktraceAsString;
	}
}
