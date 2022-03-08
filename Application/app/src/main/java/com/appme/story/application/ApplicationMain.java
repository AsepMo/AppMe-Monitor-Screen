package com.appme.story.application;

import android.app.Application;

public class ApplicationMain extends Application {
    
    public static String TAG = ApplicationMain.class.getSimpleName();
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        initAnalytics();
        initCrashHandler();
        initFolder();
        initSound();
    }
    
    public void initAnalytics(){}
    public void initCrashHandler(){}
    public void initFolder(){}
    public void initSound(){}
}
