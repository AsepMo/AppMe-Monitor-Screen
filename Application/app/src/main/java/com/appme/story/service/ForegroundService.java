package com.appme.story.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.appme.story.R;
import com.appme.story.AppController;
import com.appme.story.application.ApplicationMonitor;
import com.appme.story.engine.app.tasks.ForegroundTaskHandler;
import com.appme.story.engine.app.commons.connections.ScreenClient;
import com.appme.story.engine.app.commons.connections.ScreenServer;
import com.appme.story.engine.app.utils.NetworkStateUtil;
import com.appme.story.engine.graphics.ImageGenerator;
import com.appme.story.receiver.Remote;

public class ForegroundService extends Service {
    private static ForegroundService foregroundService;


    private ScreenServer httpServer;
    private ImageGenerator imageGenerator;
    private ForegroundTaskHandler foregroundServiceTaskHandler;

    private int httpServerStatus = ScreenServer.SERVER_STATUS_UNKNOWN;

    private int currentServiceMessage;

    // Fields for notifications
    private Notification startNotification;
    private BroadcastReceiver localNotificationReceiver;
    private final Intent startStreamIntent = new Intent(Remote.KEY_START);
    private final Intent stopStreamIntent = new Intent(Remote.KEY_STOP);
    private final Intent closeIntent = new Intent(Remote.KEY_CLOSE);
    
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate() {
        foregroundService = this;

        httpServer = new ScreenServer();
        imageGenerator = new ImageGenerator(
            getResources().getString(R.string.press),
            getResources().getString(R.string.start_stream),
            getResources().getString(R.string.on_device)
        );

        // Starting thread Handler
        final HandlerThread looperThread = new HandlerThread("ForegroundServiceHandlerThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        looperThread.start();
        foregroundServiceTaskHandler = new ForegroundTaskHandler(looperThread.getLooper());

        //Local notifications
        startNotification = getNotificationStart();

        final IntentFilter localNotificationIntentFilter = new IntentFilter();
        localNotificationIntentFilter.addAction(Remote.KEY_START);
        localNotificationIntentFilter.addAction(Remote.KEY_STOP);
        localNotificationIntentFilter.addAction(Remote.KEY_CLOSE);

        localNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Remote.KEY_START)) {
                    currentServiceMessage = Remote.SERVICE_MESSAGE_START_STREAMING;
                    relayMessageViaActivity();
                }

                if (intent.getAction().equals(Remote.KEY_STOP)) {
                    currentServiceMessage = Remote.SERVICE_MESSAGE_STOP_STREAMING;
                    relayMessageViaActivity();
                }

                if (intent.getAction().equals(Remote.KEY_CLOSE)) {
                    currentServiceMessage = Remote.SERVICE_MESSAGE_EXIT;
                    relayMessageViaActivity();
                }
            }
        };
        
        registerReceiver(localNotificationReceiver, localNotificationIntentFilter);

        // Registering receiver for screen off messages
        final IntentFilter screenOnOffFilter = new IntentFilter();
        screenOnOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenOnOffFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        screenOnOffFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AppController.getApplicationSettings().isPauseOnSleep())
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                        if (AppController.isStreamRunning()) {
                            currentServiceMessage = Remote.SERVICE_MESSAGE_STOP_STREAMING;
                            relayMessageViaActivity();
                        }

                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    currentServiceMessage = Remote.SERVICE_MESSAGE_GET_STATUS;
                    sendBroadcast(new Intent(Remote.SERVICE_ACTION).putExtra(Remote.SERVICE_MESSAGE, Remote.SERVICE_MESSAGE_UPDATE_STATUS), Remote.SERVICE_PERMISSION);
                }      
            }
        };

        registerReceiver(broadcastReceiver, screenOnOffFilter);

        sendBroadcast(new Intent(Remote.SERVICE_ACTION).putExtra(Remote.SERVICE_MESSAGE, Remote.SERVICE_MESSAGE_UPDATE_STATUS), Remote.SERVICE_PERMISSION);

        imageGenerator.addDefaultScreen();
        httpServerStartAndCheck();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int messageFromActivity = intent.getIntExtra(Remote.SERVICE_MESSAGE, 0);
        if (messageFromActivity == 0) return START_NOT_STICKY;

        if (messageFromActivity == Remote.SERVICE_MESSAGE_PREPARE_STREAMING) {
            startForeground(110, startNotification);
            AppController.setIsForegroundServiceRunning(true);
        }

        if (messageFromActivity == Remote.SERVICE_MESSAGE_GET_STATUS) {
            sendCurrentServiceMessage();
            sendServerAddress();
            sendClientCount();
        }

        if (messageFromActivity == Remote.SERVICE_MESSAGE_START_STREAMING) {
            stopForeground(true);
            foregroundServiceTaskHandler.obtainMessage(ForegroundTaskHandler.HANDLER_START_STREAMING).sendToTarget();
            startForeground(120, getNotificationStop());
        }

        if (messageFromActivity == Remote.SERVICE_MESSAGE_STOP_STREAMING) {
            stopForeground(true);
            foregroundServiceTaskHandler.obtainMessage(ForegroundTaskHandler.HANDLER_STOP_STREAMING).sendToTarget();
            startForeground(110, startNotification);

            imageGenerator.addDefaultScreen();
        }

        if (messageFromActivity == Remote.SERVICE_MESSAGE_RESTART_HTTP) {
            httpServer.stop();
            imageGenerator.addDefaultScreen();
            httpServerStartAndCheck();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        httpServer.stop();
        stopForeground(true);
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(localNotificationReceiver);
        foregroundServiceTaskHandler.getLooper().quit();
    }

    private void relayMessageViaActivity() {
        Intent intent = new Intent(this, ApplicationMonitor.class);
        intent.setAction(ApplicationMonitor.ACTION_SCREEN_SERVER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        sendBroadcast(new Intent(Remote.SERVICE_ACTION).putExtra(Remote.SERVICE_MESSAGE, Remote.SERVICE_MESSAGE_UPDATE_STATUS), Remote.SERVICE_PERMISSION);
    }

    private void httpServerStartAndCheck() {
        httpServerStatus = httpServer.start();
        if (httpServerStatus == ScreenServer.SERVER_ERROR_PORT_IN_USE) {
            currentServiceMessage = Remote.SERVICE_MESSAGE_HTTP_PORT_IN_USE;
            relayMessageViaActivity();
        } else {
            currentServiceMessage = Remote.SERVICE_MESSAGE_HTTP_OK;
            relayMessageViaActivity();
        }
    }

    // Static methods
    public static ImageGenerator getImageGenerator() {
        return foregroundService.imageGenerator;
    }

    public static void addClient(final ScreenClient client) {
        AppController.getClientQueue().add(client);
        foregroundService.sendClientCount();
    }

    public static void removeClient(final ScreenClient client) {
        AppController.getClientQueue().remove(client);
        foregroundService.sendClientCount();
    }

    public static void clearClients() {
        AppController.getClientQueue().clear();
        foregroundService.sendClientCount();
    }

    public static int getHttpServerStatus() {
        if (foregroundService == null) return ScreenServer.SERVER_STATUS_UNKNOWN;
        return foregroundService.httpServerStatus;
    }

    // Private methods
    private Notification getNotificationStart() {
        final Intent mainActivityIntent = new Intent(this, ApplicationMonitor.class);
        mainActivityIntent.setAction(ApplicationMonitor.ACTION_SCREEN_SERVER);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent pendingMainActivityIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, 0);

        final NotificationCompat.Builder startNotificationBuilder = new NotificationCompat.Builder(this);
        startNotificationBuilder.setSmallIcon(R.drawable.ic_app_cast);
        startNotificationBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        startNotificationBuilder.setContentTitle(getResources().getString(R.string.ready_to_stream));
        startNotificationBuilder.setContentText(getResources().getString(R.string.press_start));
        startNotificationBuilder.setContentIntent(pendingMainActivityIntent);
        startNotificationBuilder.addAction(R.drawable.ic_recorder_stop, getResources().getString(R.string.start), PendingIntent.getBroadcast(this, 0, startStreamIntent, 0));
        startNotificationBuilder.addAction(R.drawable.ic_service_close, getResources().getString(R.string.close), PendingIntent.getBroadcast(this, 0, closeIntent, 0));
        startNotificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        return startNotificationBuilder.build();
    }

    private Notification getNotificationStop() {
        final Intent mainActivityIntent = new Intent(this, ApplicationMonitor.class);
        mainActivityIntent.setAction(ApplicationMonitor.ACTION_SCREEN_SERVER);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent pendingMainActivityIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, 0);

        final NotificationCompat.Builder stopNotificationBuilder = new NotificationCompat.Builder(this);
        stopNotificationBuilder.setSmallIcon(R.drawable.ic_app_cast);
        stopNotificationBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        stopNotificationBuilder.setContentTitle(getResources().getString(R.string.stream));
        stopNotificationBuilder.setContentText(getResources().getString(R.string.go_to) + AppController.getServerAddress());
        stopNotificationBuilder.setContentIntent(pendingMainActivityIntent);
        stopNotificationBuilder.addAction(R.drawable.ic_recorder_stop, getResources().getString(R.string.stop), PendingIntent.getBroadcast(this, 0, stopStreamIntent, 0));
        stopNotificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        return stopNotificationBuilder.build();
    }

    private void sendCurrentServiceMessage() {
        sendBroadcast(new Intent(Remote.SERVICE_ACTION).putExtra(Remote.SERVICE_MESSAGE, currentServiceMessage),
                      Remote.SERVICE_PERMISSION);
        currentServiceMessage = 0;
    }

    private void sendClientCount() {
        sendBroadcast(new Intent(Remote.SERVICE_ACTION)
                      .putExtra(Remote.SERVICE_MESSAGE, Remote.SERVICE_MESSAGE_GET_CLIENT_COUNT)
                      .putExtra(Remote.SERVICE_MESSAGE_CLIENTS_COUNT, AppController.getClientQueue().size()),
                      Remote.SERVICE_PERMISSION);
    }

    private void sendServerAddress() {
        sendBroadcast(new Intent(Remote.SERVICE_ACTION)
                      .putExtra(Remote.SERVICE_MESSAGE, Remote.SERVICE_MESSAGE_GET_SERVER_ADDRESS)
                      .putExtra(Remote.SERVICE_MESSAGE_SERVER_ADDRESS, AppController.getServerAddress()),
                      Remote.SERVICE_PERMISSION);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
