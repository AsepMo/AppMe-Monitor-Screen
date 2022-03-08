package com.appme.story.service;

import android.animation.ValueAnimator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.app.Service;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import com.appme.story.R;
import com.appme.story.application.ApplicationMonitor;
import com.appme.story.AppController;
import com.appme.story.receiver.Remote;
import com.appme.story.service.ScreenRecord;
import com.appme.story.engine.widget.ScreenCapture;
import com.appme.story.engine.graphics.ImageTransmogrifier;

public class MonitorScreenService extends Service {

    public static String TAG = MonitorScreenService.class.getSimpleName();
    private static final boolean DEBUG = false;



    private final HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler;

    private WindowManager mWindowManager;
    
    private Handler mHandlerScreen;
    private Runnable mRunnerScreen = new Runnable(){
        @Override
        public void run() {
            mScreenCapture.start();
        }
    };

    private int resultCode;
    private Intent resultData;

    private static boolean isRunning;
    private boolean recordOnNextStart = false;
    private ScreenRecordingSession session = null;
    private ScreenCapture mScreenCapture = null;
    private ScreenShotSession mScreenShotSession = null;
    private final ToneGenerator beeper = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    private boolean isForeground = false;
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("Binding not supported. Go away.");
    }

    public WindowManager getWindowManager() {
        return(mWindowManager);
    }

    public Handler getHandler() {
        return(handler);
    }

    @Override 
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.v(TAG, "onCreate:");

        //mMediaProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        isRunning = true;
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.v(TAG, "onStartCommand:intent=" + intent);

        if (intent.getAction() == null) {
            resultCode = intent.getIntExtra(Remote.EXTRA_RESULT_CODE, 1337);
            resultData = intent.getParcelableExtra(Remote.EXTRA_RESULT_INTENT);            
            String message = intent.getStringExtra(Remote.EXTRA_SERVICE);

            if (recordOnNextStart) {
                startRecorder();
            }

            foregroundify(!recordOnNextStart);
            recordOnNextStart = false;
            if (resultData != null) {
                ScreenRecord.getInstance().broadcastStatus(Remote.SERVICE_IS_READY, message);
            }
            isRunning = true;
        } else if (Remote.ACTION.START_SCREEN_CAPTURE.equals(intent.getAction())) {
            int mResultCode = intent.getIntExtra(Remote.EXTRA_RESULT_CODE, 1337);
            Intent mResultData = intent.getParcelableExtra(Remote.EXTRA_RESULT_INTENT);            

            if (mResultData != null) {
                startScreenPlay(mResultCode, mResultData);
            }
            foregroundify(true);

        } else if (Remote.ACTION.START_SCREEN_SHOT.equals(intent.getAction())) {

            if (resultData != null) {
                startScreenShot();
            } else {
                Intent ui = new Intent(this, ApplicationMonitor.class);
                ui.setAction(ApplicationMonitor.ACTION_MONITOR_SCREEN);
                ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ui);
            }

        } else if (Remote.ACTION.START_SCREEN_RECORD.equals(intent.getAction())) {
            if (resultData != null) {
                foregroundify(false);
                startRecorder();
                isRunning = true;
            } else {
                Intent ui = new Intent(this, ApplicationMonitor.class);
                ui.setAction(ApplicationMonitor.ACTION_MONITOR_SCREEN);
                ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ui);
                recordOnNextStart = true;
            }
        } else if (Remote.ACTION.PAUSE_SCREEN_RECORD.equals(intent.getAction())) {

        } else if (Remote.ACTION.RESUME_SCREEN_RECORD.equals(intent.getAction())) {

        } else if (Remote.ACTION.STOP_SCREEN_RECORD.equals(intent.getAction())) {
            foregroundify(true);
            stopRecorder();
            isRunning = false;
        } else if (Remote.ACTION.SHUTDOWN_SERVICE.equals(intent.getAction())) {
            beeper.startTone(ToneGenerator.TONE_PROP_NACK);
            if(!isRunning){
                foregroundify(true);
                stopRecorder();
            }
            ScreenRecord.getInstance().broadcastStatus(Remote.SERVICE_IS_SHUTDOWN, "Service Is Shutdown");                                 
            stopForeground(true);
            stopSelf();
        }

        return(START_NOT_STICKY);

    }
    
    public static boolean isRunning(){
        return isRunning;
    }

    public void processImage(final byte[] png) {
        new Thread() {
            @Override
            public void run() {
                File output = new File(getExternalFilesDir(null), "screenshot.png");

                try {
                    FileOutputStream fos=new FileOutputStream(output);

                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                    MediaScannerConnection.scanFile(MonitorScreenService.this,
                                                    new String[] {output.getAbsolutePath()},
                                                    new String[] {"image/png"},
                                                    null);
                    ScreenRecord.getInstance().broadcastStatus(Remote.SCREEN_SHOT_IS_DONE, output.getAbsolutePath());                                 

                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception writing out screenshot", e);
                }
            }
        }.start();
        beeper.startTone(ToneGenerator.TONE_PROP_ACK);
        stopScreenShot();
    }

    

    private synchronized void startScreenShot() {
        if (mScreenShotSession == null) {
            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            MediaProjection projection =  mgr.getMediaProjection(resultCode, resultData);

            mScreenShotSession = new ScreenShotSession(this, this, projection);
            mScreenShotSession.start(handler);
        }
    }
    
    private synchronized void stopScreenShot() {
        if (mScreenShotSession != null) {
            mScreenShotSession.stop();
            mScreenShotSession = null;
        }
    }

    private synchronized void startScreenPlay(final int code, final Intent data) {
        if (mScreenCapture == null) {
            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            MediaProjection projection =  mgr.getMediaProjection(code, data);

            mScreenCapture = ScreenCapture.with(this, projection);                 
            mScreenCapture.setCancelClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScreenRecord.getInstance().broadcastStatus(Remote.SERVICE_IS_SHUTDOWN, "Service Is Shutdown", code, data);            
                        stopScreenPlay();                 

                    }
                });
            mHandlerScreen = new Handler(); 
            mHandlerScreen.postDelayed(mRunnerScreen, 1200);
        }
    }
    
    private synchronized void stopScreenPlay() {
        if (mScreenCapture != null) {
            mScreenCapture.stop();
            mScreenCapture = null;
        }
    }

    private synchronized void startRecorder() {
        if (session == null) {
            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            MediaProjection projection =  mgr.getMediaProjection(resultCode, resultData);

            session = new ScreenRecordingSession(this, new ScreenRecordingConfig(this), projection);
            session.start();
        }     
    }

    private synchronized void stopRecorder() {
        if (session != null) {
            session.stop();
            session = null;
        }
    }

    private void foregroundify(boolean screenRecord) {
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            mgr.getNotificationChannel(Remote.CHANNEL_WHATEVER) == null) {
            mgr.createNotificationChannel(new NotificationChannel(Remote.CHANNEL_WHATEVER,
                                                                  "Whatever", NotificationManager.IMPORTANCE_DEFAULT));
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, Remote.CHANNEL_WHATEVER);

        b.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL);

        b.setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(getString(R.string.app_name));

        b.addAction(R.drawable.ic_monitor_screenshot,
                    getString(R.string.notify_screen_shot),
                    buildPendingIntent(Remote.ACTION.START_SCREEN_SHOT));
        if (screenRecord) {
            b.addAction(R.drawable.ic_recorder_play,
                        getString(R.string.notify_record), buildPendingIntent(Remote.ACTION.START_SCREEN_RECORD));
        } else {
            b.addAction(R.drawable.ic_recorder_stop,
                        getString(R.string.notify_stop), buildPendingIntent(Remote.ACTION.STOP_SCREEN_RECORD));
        }       
        b.addAction(R.drawable.ic_window_close,
                    getString(R.string.notify_shutdown),
                    buildPendingIntent(Remote.ACTION.SHUTDOWN_SERVICE));
        if (isForeground) {
            mgr.notify(Remote.NOTIFY_ID, b.build());
        }
        else {
            startForeground(Remote.NOTIFY_ID, b.build());
            isForeground=true;
        }
        startForeground(Remote.NOTIFY_ID, b.build());
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i=new Intent(this, getClass());

        i.setAction(action);

        return(PendingIntent.getService(this, 0, i, 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecorder();
        stopForeground(true);
    }
}
