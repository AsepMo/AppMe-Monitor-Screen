package com.appme.story;

import android.support.v7.widget.Toolbar;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.util.Pair;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import com.appme.story.engine.app.commons.activity.ActionBarActivity;
import com.appme.story.application.ApplicationMonitor;

public class SplashActivity extends ActionBarActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();
    private ImageView mAppIcon;
    private TextView mAppName;
    private TextView mAppMessage;
    private PackageManager packageManager;
    private PackageInfo packageInfo;
    private File appFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Application);
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.activity_splash);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(null);

        }
        packageManager = getPackageManager();
        try {
            packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            appFile = new File(packageInfo.applicationInfo.sourceDir);
            mAppIcon = (ImageView) findViewById(R.id.splash_app_icon);
            mAppIcon.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));

            mAppName = (TextView) findViewById(R.id.splash_app_title);
            mAppName.setText(packageInfo.applicationInfo.loadLabel(packageManager).toString());

        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        mAppMessage = (TextView) findViewById(R.id.splash_app_message);
        mAppMessage.setText("Welcome To AppMe");

        new CountDownTimer(TIME_OUT, TIME_OUT){
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                mAppMessage.setText("Start Activity");
                ApplicationMonitor.startOutPutRecorder(SplashActivity.this, 0, "");
                SplashActivity.this.finish();
            }  
        }.start();
        
    }

}
/*don't forget to subscribe my YouTube channel for more Tutorial and mod*/
/*
https://youtube.com/channel/UC_lCMHEhEOFYgJL6fg1ZzQA */
