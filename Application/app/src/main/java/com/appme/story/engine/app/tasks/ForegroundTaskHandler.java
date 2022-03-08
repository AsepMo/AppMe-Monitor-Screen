package com.appme.story.engine.app.tasks;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;

import com.appme.story.AppController;
import com.appme.story.service.ForegroundService; 

public class ForegroundTaskHandler extends Handler {
    
    public static String TAG = ForegroundTaskHandler.class.getSimpleName();
    public static final int HANDLER_START_STREAMING = 0;
    public static final int HANDLER_STOP_STREAMING = 1;

    private static final int HANDLER_PAUSE_STREAMING = 4;
    private static final int HANDLER_RESUME_STREAMING = 5;
    private static final int HANDLER_DETECT_ROTATION = 10;

    private int currentOrientation;

    public ForegroundTaskHandler(final Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case HANDLER_START_STREAMING:
                if (AppController.isStreamRunning()) break;
                removeMessages(HANDLER_DETECT_ROTATION);
                currentOrientation = getOrientation();
                ForegroundService.getImageGenerator().start();
                sendMessageDelayed(obtainMessage(HANDLER_DETECT_ROTATION), 250);
                AppController.setIsStreamRunning(true);
                break;
            case HANDLER_PAUSE_STREAMING:
                if (!AppController.isStreamRunning()) break;
                ForegroundService.getImageGenerator().stop();
                sendMessageDelayed(obtainMessage(HANDLER_RESUME_STREAMING), 250);
                break;
            case HANDLER_RESUME_STREAMING:
                if (!AppController.isStreamRunning()) break;
                ForegroundService.getImageGenerator().start();
                sendMessageDelayed(obtainMessage(HANDLER_DETECT_ROTATION), 250);
                break;
            case HANDLER_STOP_STREAMING:
                if (!AppController.isStreamRunning()) break;
                removeMessages(HANDLER_DETECT_ROTATION);
                removeMessages(HANDLER_STOP_STREAMING);
                ForegroundService.getImageGenerator().stop();
                if (AppController.getMediaProjection() != null)
                    AppController.getMediaProjection().stop();
                AppController.setIsStreamRunning(false);
                break;
            case HANDLER_DETECT_ROTATION:
                if (!AppController.isStreamRunning()) break;
                final int newOrientation = getOrientation();
                if (currentOrientation == newOrientation) {
                    sendMessageDelayed(obtainMessage(HANDLER_DETECT_ROTATION), 250);
                    break;
                }
                currentOrientation = newOrientation;
                obtainMessage(HANDLER_PAUSE_STREAMING).sendToTarget();
                break;
            default:
                Log.v(ForegroundTaskHandler.class.getSimpleName(), "Cannot handle message");
        }
    }

    private int getOrientation() {
        final int rotation = AppController.getWindowsManager().getDefaultDisplay().getRotation();
        if (rotation == ROTATION_0 || rotation == ROTATION_180) return 0;
        return 1;
    }
}
