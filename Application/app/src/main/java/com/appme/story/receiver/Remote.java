package com.appme.story.receiver;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.List;
import com.appme.story.service.MonitorScreenService;

public class Remote {
    public static String TAG = Remote.class.getSimpleName();

    public static final String BASE = "com.github.template.process";
    private static final String BASE_ACTION = "com.github.template.service.ScreenMonitorService.";
    
    public static final String PROCESS_BROADCAST_ACTION = BASE + ".RECEIVE_BROADCAST";
    public static final String PROCESS_STATUS_KEY = BASE +".STATUS_KEY";
    public static final String PROCESS_STATUS_MESSAGE = BASE + ".STATUS_MESSAGE";
    public static final String PROCESS_DIR = BASE + ".DIR";
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    public static final String EXTRA_FILE_NAME = "FILE_PATH";
    public static final String RECORDING_VIDEO_ID = BASE +".VIDEO_ID";
    public static final int PROCESS_NOTIFICATION_ID = 1;

    public static final String SERVICE_IS_READY = "SERVICE_IS_READY";
    public static final String START_RECORDING = "START_RECORDING";
    public static final String START_ACTIVITY = "START_ACTIVITY";
    public static final String PAUSE_RECORDING = "PAUSE_RECORDING";
    public static final String RESUME_RECORDING = "RESUME_RECORDING";
    public static final String STOP_RECORDING = "STOP_RECORDING";
    public static final String START_ACTIVITY_WITH_ERROR = "START_ACTIVITY_WITH_ERROR";
    public static final String EXIT_RECORDING_ON_ERROR = "EXIT_RECORDING_ON_ERROR";
    public static final String FINISH_RECORDING = "FINISH_RECORDING";
    public static final String RECORDING_IS_DONE = "RECORDING_IS_DONE";
    public static final String SERVICE_IS_SHUTDOWN = "SERVICE_IS_SHUTDOWN";

    public static final String EXTRA_SERVICE = "EXTRA_SERVICE";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_INTENT = "resultIntent";    
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";

    public static final String CHANNEL_WHATEVER = "channel_whatever";
    public static final int NOTIFY_ID = 9906;

    public static final String SCREEN_SHOT_IS_DONE = "SCREEN_SHOT_IS_DONE";
    public static final String SCREEN_RECORD_IS_DONE = "SCREEN_RECORD_IS_DONE";

    // Fields for broadcast
    public static final String BASE_ACTION_SCREEN_STREAM = "com.github.template.service";
    public static final String SERVICE_ACTION = BASE_ACTION_SCREEN_STREAM + ".ForegroundService.SERVICE_ACTION";

    public static final String SERVICE_PERMISSION = "com.github.template.RECEIVE_BROADCAST";
    public static final String SERVICE_MESSAGE = "SERVICE_MESSAGE";
    public static final int SERVICE_MESSAGE_GET_STATUS = 1000;
    public static final int SERVICE_MESSAGE_UPDATE_STATUS = 1005;
    public static final int SERVICE_MESSAGE_PREPARE_STREAMING = 1010;
    public static final int SERVICE_MESSAGE_START_STREAMING = 1020;
    public static final int SERVICE_MESSAGE_STOP_STREAMING = 1030;
    public static final int SERVICE_MESSAGE_RESTART_HTTP = 1040;
    public static final int SERVICE_MESSAGE_HTTP_PORT_IN_USE = 1050;
    public static final int SERVICE_MESSAGE_HTTP_OK = 1060;
    public static final int SERVICE_MESSAGE_EXIT = 1100;

    public static final String SERVICE_MESSAGE_CLIENTS_COUNT = "SERVICE_MESSAGE_CLIENTS_COUNT";
    public static final int SERVICE_MESSAGE_GET_CLIENT_COUNT = 1110;
    public static final String SERVICE_MESSAGE_SERVER_ADDRESS = "SERVICE_MESSAGE_SERVER_ADDRESS";
    public static final int SERVICE_MESSAGE_GET_SERVER_ADDRESS = 1120;
    public static final String KEY_START = BASE_ACTION_SCREEN_STREAM + ".ForegroundService.startStream";
    public static final String KEY_STOP = BASE_ACTION_SCREEN_STREAM + ".ForegroundService.stopStream";
    public static final String KEY_CLOSE = BASE_ACTION_SCREEN_STREAM + ".ForegroundService.closeService";
    
    
    public interface ACTION {
        String START_SERVICE = BASE_ACTION + ".ACTION_START_SERVICE";      
        String STOP_SCREEN_CAPTURE = BASE_ACTION + ".ACTION_STOP_SCREEN_CAPTURE";
        String STOP_SCREEN_SHOT = BASE_ACTION + ".ACTION_STOP_SCREEN_SHOT";
        String STOP_SCREEN_RECORD = BASE_ACTION + ".ACTION_STOP_RECORD";
        String START_SCREEN_CAPTURE = BASE_ACTION + ".ACTION_START_SCREEN_CAPTURE";
        String START_SCREEN_SHOT = BASE_ACTION + ".ACTION_START_SCREEN_SHOT";
        String START_SCREEN_RECORD = BASE_ACTION + ".ACTION_START_SCREEN_RECORD";
        String SCREEN_SHOT_DONE = BASE_ACTION + ".ACTION_SCREEN_SHOT_DONE";
        String SCREEN_RECORD_DONE = BASE_ACTION + ".ACTION_SCREEN_RECORD_DONE";  
        String PAUSE_SCREEN_CAPTURE = BASE + ".ACTION_PAUSE_SCREEN_CAPTURE";
        String PAUSE_SCREEN_RECORD = BASE + ".ACTION_PAUSE_SCREEN_RECORD";
        String PAUSE_SCREEN_SHOT = BASE + ".ACTION_PAUSE_SCREEN_SHOT";
        String RESUME_SCREEN_CAPTURE = BASE + ".ACTION_SCREEN_CAPTURE_RESUME";   
        String RESUME_SCREEN_SHOT = BASE + ".ACTION_SCREEN_SHOT_RESUME";  
        String RESUME_SCREEN_RECORD = BASE + ".ACTION_SCREEN_RECORD_RESUME";  
        String SHUTDOWN_SERVICE = BASE_ACTION + ".ACTION_SHUTDOWN_SERVICE";
    }

    public enum TYPE {
        SCREEN_PLAY,  
        SCREEN_SHOT, 
        SCREEN_RECORD   
    }
    
    public static void killAllService(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
            String processName = context.getPackageName() + ":service";
            if (next.processName.equals(processName)) {
                android.os.Process.killProcess(next.pid);
                break;
            }
        }
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
            String processName = context.getPackageName() + ":service";
            if (next.processName.equals(processName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning() {
        return MonitorScreenService.isRunning();
    }
}


