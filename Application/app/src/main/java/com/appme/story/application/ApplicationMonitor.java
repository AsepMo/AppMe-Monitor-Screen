package com.appme.story.application;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.appme.story.R;
import com.appme.story.engine.app.commons.activity.ActionBarActivity;
import com.appme.story.engine.app.fragments.MonitorScreenFragment;

public class ApplicationMonitor extends ActionBarActivity {

    public static final String TAG = ApplicationMonitor.class.getSimpleName();
    public final static String BASE_ACTION = "com.appme.story";

    public final static String ACTION_MONITOR_ACTIVITY = BASE_ACTION + ".application.MONITOR_ACTIVITY";
    public final static String ACTION_MONITOR_RECEIVER = BASE_ACTION + ".application.MONITOR_RECEIVER";
    public final static String ACTION_MONITOR_PACKAGE_RECEIVER = BASE_ACTION + ".application.MONITOR_PACKAGE_RECEIVER";
    public final static String ACTION_MONITOR_SCREEN = BASE_ACTION + ".application.MONITOR_SCREEN";
    public final static String ACTION_MONITOR_SDCARD = BASE_ACTION + ".application.MONITOR_SDCARD";
    public final static String ACTION_MONITOR_MEMORY = BASE_ACTION + ".application.MONITOR_MEMORY";
    public final static String ACTION_MONITOR_LOGGER = BASE_ACTION + ".application.MONITOR_LOGGER";

    //APP SERVER
    public final static String ACTION_MESSAGE_SERVER = BASE_ACTION + ".application.MESSAGE_SERVER";
    public final static String ACTION_CAMERA_SERVER = BASE_ACTION + ".application.CAMERA_SERVER";
    public final static String ACTION_SCREEN_SERVER = BASE_ACTION + ".application.SCREEN_SERVER";
    public final static String ACTION_FILE_SERVER = BASE_ACTION + ".application.FILE_SERVER";
    public final static String ACTION_WEB_SERVER = BASE_ACTION + ".application.WEB_SERVER";
    public final static String ACTION_RC_SERVER = BASE_ACTION + ".application.RC_SERVER";
    public final static String ACTION_LOGCAT_SERVER = BASE_ACTION + ".application.LOGCAT_SERVER";

    public final static String ACTION_MESSAGE_CLIENT = BASE_ACTION + ".application.MESSAGE_CLIENT";
    public final static String ACTION_CAMERA_CLIENT = BASE_ACTION + ".application.CAMERA_CLIENT";
    public final static String ACTION_SCREEN_CLIENT = BASE_ACTION + ".application.SCREEN_CLIENT";
    public final static String ACTION_FILE_CLIENT = BASE_ACTION + ".application.FILE_CLIENT";
    public final static String ACTION_WEB_CLIENT = BASE_ACTION + ".application.WEB_CLIENT";
    public final static String ACTION_RC_CLIENT = BASE_ACTION + ".application.RC_CLIENT";
    public final static String ACTION_LOGCAT_CLIENT = BASE_ACTION + ".application.LOGCAT_CLIENT";

    public final static String EXTRA_TYPE = "EXTRA_TYPE";
    public final static String EXTRA_PATH = "EXTRA_PATH";

    public static void start(Context c, String action) {
        Intent mIntent = new Intent(c, ApplicationMonitor.class);
        mIntent.setAction(action);       
        c.startActivity(mIntent);
    }

    public static void startOutPutRecorder(Context c, int type, String path) {
        Intent mIntent = new Intent(c, ApplicationMonitor.class);
        mIntent.setAction(ApplicationMonitor.ACTION_MONITOR_SCREEN);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.putExtra(ApplicationMonitor.EXTRA_TYPE, type);
        mIntent.putExtra(ApplicationMonitor.EXTRA_PATH, path);
        c.startActivity(mIntent);
    }
    
    private String title = "Monitor";
    private String contentTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.AppTheme_Application);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);
        setupToolbar(title);

        String action = getIntent().getAction();        
        if (action != null && action.equals(ACTION_MONITOR_SCREEN)) {
            contentTitle = "Screen";
            String type = getIntent().getStringExtra(EXTRA_TYPE);
            String path = getIntent().getStringExtra(EXTRA_PATH);
            
            switchFragment(MonitorScreenFragment.newInstance(contentTitle, type, path)); 
        } 
        
    }

}
