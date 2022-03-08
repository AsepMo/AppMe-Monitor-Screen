package com.appme.story.engine.app.commons.activity;

import android.annotation.TargetApi;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.appme.story.R;

public class ActionBarActivity extends AppCompatActivity 
{
    private Context mContext;
    public int TIME_OUT = 2000;
    public int SPLASH_TIME_OUT = 5000;
    
    private ProgressDialog progress;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {    
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        progress = new ProgressDialog(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupToolbar()
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final Drawable backArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        if (backArrow != null)
        {
            backArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        if (toolbar != null)
        {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(null);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(backArrow);
        }
        
        final TextView mAppName = (TextView) findViewById(R.id.app_title);
        mAppName.setText(getString(R.string.app_name));
    }
    
    public void setupToolbar(String mTitle)
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final Drawable backArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        if (backArrow != null)
        {
            backArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        if (toolbar != null)
        {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(backArrow);
        }
        final TextView mAppName = (TextView) findViewById(R.id.app_title);
        mAppName.setText(mTitle);
        
    }
    
    public void switchActivity(final AppCompatActivity activity, final String message, final Class<?> mClass) {
        showProgress();
        new CountDownTimer(TIME_OUT, TIME_OUT){
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();       
                hideProgress();
                Intent mIntent = new Intent(activity, mClass);
                activity.startActivity(mIntent);
                activity.finish();
            }  
        }.start();
    }

    public void switchActivity(final AppCompatActivity activity, final TextView mMessage, final String message, final Class<?> mClass) {
        mMessage.setText(message);     
        new CountDownTimer(TIME_OUT, TIME_OUT){
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                Intent mIntent = new Intent(activity, mClass);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(mIntent);
                activity.finish();
            }  
        }.start();
    }
    
    public void switchFragment(Fragment fragment){
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit();
	}
    
    public void showProgress() {
        //progress.setTitle(R.string.web_file_extract);
        progress.setMessage("Starting..");
        progress.setCanceledOnTouchOutside(false);
        progress.setCancelable(false);
        //progress.setOnCancelListener(this);
        progress.show();
    }

    public ProgressDialog getProgressDialog() {
        return progress;
    }

    public void hideProgress(){
        if (getProgressDialog() != null)
        {
            getProgressDialog().dismiss();
        }
    }
    
}

