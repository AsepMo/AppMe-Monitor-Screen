package com.appme.story.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.appme.story.AppController;
import com.appme.story.receiver.Remote;

public class ScreenRecord {
    private static volatile ScreenRecord Instance = null;
    private Context context;

    public static ScreenRecord getInstance() {
        ScreenRecord localInstance = Instance;
        if (localInstance == null) {
            synchronized (ScreenRecord.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ScreenRecord(AppController.getAppContext());
                }
            }
        }
        return localInstance;
    }

    private ScreenRecord(Context context) {
        this.context = context;
    }

    public static ScreenRecord with(Context context) {
        return new ScreenRecord(context);
    }

    public void launchRecordingService() {
        Remote.killAllService(context);
        Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
        mServiceIntent.setAction(Remote.ACTION.START_SERVICE);
        context.startService(mServiceIntent);
    }

    public void startRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.START_SCREEN_RECORD);
            context.startService(mServiceIntent);
        }
    }

    public void pauseRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.PAUSE_SCREEN_RECORD);
            context.startService(mServiceIntent);
        }
    }

    public void resumeRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.RESUME_SCREEN_RECORD);
            context.startService(mServiceIntent);
        }
    }

    public void stopRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.STOP_SCREEN_RECORD);
            context.startService(mServiceIntent);
        }
    }

    public void doneRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.SCREEN_RECORD_DONE);       
            //mServiceIntent.putExtra(Remote.START_ACTIVITY, MonitorScreenService.getOutPut(context));
            context.startService(mServiceIntent);
        }
    }


    public void shutdownRecordingService() {
        if (Remote.isServiceRunning(context)) {
            Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
            mServiceIntent.setAction(Remote.ACTION.SHUTDOWN_SERVICE);
            context.startService(mServiceIntent);
        }
    }

    public void startScreenMonitorService(String message, int resultCode, Intent resultData) {
        Remote.killAllService(context);
        Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
        mServiceIntent.putExtra(Remote.EXTRA_RESULT_CODE, resultCode);
        mServiceIntent.putExtra(Remote.EXTRA_RESULT_INTENT, resultData);        
        mServiceIntent.putExtra(Remote.EXTRA_SERVICE, message);
        context.startService(mServiceIntent);
    }

    public void startScreenCaptureService(int resultCode, Intent resultData) {
        Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
        mServiceIntent.setAction(Remote.ACTION.START_SCREEN_CAPTURE);
        mServiceIntent.putExtra(Remote.EXTRA_RESULT_CODE, resultCode);
        mServiceIntent.putExtra(Remote.EXTRA_RESULT_INTENT, resultData);        
        context.startService(mServiceIntent);
    }

    public void startScreenShotService() {
        Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
        mServiceIntent.setAction(Remote.ACTION.START_SCREEN_SHOT);
        // mServiceIntent.putExtra(SendBroadcast.EXTRA_RESULT_CODE, resultCode);
        // mServiceIntent.putExtra(SendBroadcast.EXTRA_RESULT_INTENT, resultData);                    
        context.startService(mServiceIntent);
    }

    public void startScreenRecordService() {
        Intent mServiceIntent = new Intent(context, MonitorScreenService.class);
        mServiceIntent.setAction(Remote.ACTION.START_SCREEN_RECORD);
        context.startService(mServiceIntent);
    }

    public void stopScreenMonitorService() {
        final Intent intent = new Intent(context, MonitorScreenService.class);
        intent.setAction(Remote.ACTION.SHUTDOWN_SERVICE);
        context.startService(intent);
    }

    public void broadcastStatus(String statusKey) {

        Intent localIntent = new Intent(Remote.PROCESS_BROADCAST_ACTION)
            .putExtra(Remote.PROCESS_STATUS_KEY, statusKey);
        context.sendBroadcast(localIntent);
    }

    public void broadcastStatus(String statusKey, String statusData) {

        Intent localIntent = new Intent(Remote.PROCESS_BROADCAST_ACTION)
            .putExtra(Remote.PROCESS_STATUS_KEY, statusKey)
            .putExtra(Remote.PROCESS_STATUS_MESSAGE, statusData);
        context.sendBroadcast(localIntent);
    }

    public void broadcastStatus(String statusKey, String statusData, String dir) {

        Intent localIntent = new Intent(Remote.PROCESS_BROADCAST_ACTION)
            .putExtra(Remote.PROCESS_STATUS_KEY, statusKey)
            .putExtra(Remote.PROCESS_STATUS_MESSAGE, statusData)
            .putExtra(Remote.PROCESS_DIR, dir);     
        context.sendBroadcast(localIntent);
    }


    public void broadcastStatus(String statusKey, String statusData, int resultCode, Intent resultData) {

        Intent localIntent = new Intent(Remote.PROCESS_BROADCAST_ACTION)
            .putExtra(Remote.PROCESS_STATUS_KEY, statusKey)
            .putExtra(Remote.PROCESS_STATUS_MESSAGE, statusData)
            .putExtra(Remote.EXTRA_RESULT_CODE, resultCode)
            .putExtra(Remote.EXTRA_RESULT_INTENT, resultData);
        context.sendBroadcast(localIntent);
    }


}

