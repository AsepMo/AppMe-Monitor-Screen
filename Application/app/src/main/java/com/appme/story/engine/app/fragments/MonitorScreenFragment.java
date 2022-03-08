package com.appme.story.engine.app.fragments;

import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Locale;

import com.appme.story.R;
import com.appme.story.AppController;
import com.appme.story.application.ApplicationMonitor;
import com.appme.story.service.ScreenRecord;
import com.appme.story.service.MonitorScreenService;
import com.appme.story.receiver.Remote;
import com.appme.story.receiver.RemoteReceiver;
import com.appme.story.engine.app.analytics.permission.PermissionsManager;
import com.appme.story.engine.app.analytics.permission.PermissionsResultAction;
import com.appme.story.engine.widget.AlertWindow;
import com.appme.story.engine.widget.ScreenCapture;
import com.appme.story.engine.widget.VideoBox;
import com.appme.story.engine.widget.VideoBox;
import com.appme.story.settings.theme.ThemePreference;

public class MonitorScreenFragment extends Fragment implements RemoteReceiver.OnSendBroadcastListener {
    
    private static final String TAG = MonitorScreenFragment.class.getSimpleName();
    private static final String EXTRA_TYPE_SCREEN = "EXTRA_TYPE_SCREEN";
    private static final String EXTRA_PATH = "EXTRA_PATH";
    private static final String EXTRA_TEXT = "EXTRA_TEXT";
    
    private static final boolean DEBUG = false;
    private static final int REQUEST_CODE_SCREEN_SERVICE = 1;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 2;    
    private static final int REQUEST_CODE_SCREEN_SHOT = 59707;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    public static final int SCREEN_PLAY = 0;
    public static final int SCREEN_SHOT = 1;
    public static final int SCREEN_RECORD = 2;
    
    private AppCompatActivity mActivity;
    private Context mContext;
    
    private Toolbar mToolbar;
    private VideoBox mVideo;
    
    private int mResultCode;
    private Intent mResultData;

    private int TYPE_SCREEN; // 0:，2 
    private String FILE_PATH;
    
    private RemoteReceiver processStatusReceiver;

    private MenuItem mStartService;
    private MenuItem mMenuScreen;
    private MenuItem mStartRecord;
    
    private boolean mServiceRunning = false;
    private boolean mRecording = false;
    
    /*
    * Use this factory method to create a new instance of
    * this fragment using the provided parameters.
    *
    * @return A new instance of fragment Record_Fragment.
    */
    public static MonitorScreenFragment newInstance(String title, String type, String file) {
        MonitorScreenFragment f = new MonitorScreenFragment();
        Bundle b = new Bundle();
        b.putString(EXTRA_TEXT, title);
        b.putString(EXTRA_TYPE_SCREEN, type);
        b.putString(EXTRA_PATH, file);
        f.setArguments(b);
        return f;
    }

    public MonitorScreenFragment() {
    }
    
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_application, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final String text = getArguments().getString(EXTRA_TEXT);
        TYPE_SCREEN = getArguments().getInt(EXTRA_TYPE_SCREEN, SCREEN_PLAY);
        FILE_PATH = getArguments().getString(EXTRA_PATH);
        
        mActivity = (AppCompatActivity)getActivity();
        mContext = getActivity();
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mActivity.setSupportActionBar(mToolbar);
            final ActionBar actionbar = getSupportActionBar();
            actionbar.setTitle(text); 
        }
        changeActionBarColor();
        
        mVideo = (VideoBox)view.findViewById(R.id.monitor_screen);
        
        processStatusReceiver = new RemoteReceiver();
        processStatusReceiver.setOnSendBroadcastListener(this);

        registerBroadcastReceiver();
        setTypeScreen(TYPE_SCREEN);
    }

    private void setTypeScreen(final int type){
        switch (type) {
            case SCREEN_PLAY:
                mServiceRunning = !mServiceRunning;
                if (mServiceRunning) {
                    startScreenMonitor();
                }
                mActivity.supportInvalidateOptionsMenu();
                break;
            case SCREEN_SHOT:
                File file = new File(EXTRA_PATH);
                if (file.exists()) {
                    mVideo.setVideoThumbnail(file.getAbsolutePath());
                }
                break;
            case SCREEN_RECORD:
                File video = new File(EXTRA_PATH);
                if (video.exists()) {
                    mVideo.setVideoPath(video.getAbsolutePath());
                }
                break;  
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }

    @Override
    public void onServiceReady(final String message) {
        File file = new File(mContext.getExternalFilesDir(null), "screenshot.png");    
        if (file.exists())
            showImage(Uri.fromFile(file));
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScreenShotDone(final String message) {
        final File file = new File(message);
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
            new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, new PermissionsResultAction() {

                @Override
                public void onGranted() {
                    Log.i(TAG, "onGranted: Write Storage");

                    if (file.exists())
                        AppController.getInstance().showLauncherView(Uri.fromFile(file));         
                }

                @Override
                public void onDenied(String permission) {
                    Log.i(TAG, "onDenied: Write Storage: " + permission);
                    String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            });

        if (file.exists())
            showImage(Uri.fromFile(file));  
    }

    @Override
    public void onScreenRecordDone(String file) {
        File video = new File(file);
        if (video.exists())
            Toast.makeText(mContext, file, Toast.LENGTH_SHORT).show();

        mRecording = false;
    }

    @Override
    public void onServiceShutDown(String message) {

        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        mServiceRunning = false;
        getActivity().supportInvalidateOptionsMenu();
    }

    public void showImage(Uri uri) {
        // BEGIN_INCLUDE (create_show_image_dialog)
        if (uri != null) {

            // BEGIN_INCLUDE (show_image)
            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread.  Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask.  The doInBackground
            // method will run in a separate thread, but onPostExecute will run in the main
            // UI thread.
            AsyncTask<Uri, Void, Bitmap> imageLoadAsyncTask = new AsyncTask<Uri, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Uri... uris) {

                    return getBitmapFromUri(uris[0]);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    mVideo.setImageBitmap(bitmap);
                }
            };
            imageLoadAsyncTask.execute(uri);
            // END_INCLUDE (show_image)

        }
    }

    /** Create a Bitmap from the URI for that image and return it.
     *
     * @param uri the Uri for the image to return.
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume:");
        if (!Remote.isServiceRunning(mContext)) {

        } 
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.v(TAG, "onPause:");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext.unregisterReceiver(processStatusReceiver);
    }

    public void startScreenMonitor() {
        MediaProjectionManager mgr = (MediaProjectionManager)mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SERVICE);        
    }

    public void startScreenCapture() {
        MediaProjectionManager mgr = (MediaProjectionManager)mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);        
    }

    public void startScreenShot() {
        MediaProjectionManager mgr = (MediaProjectionManager)mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHOT);        
    }

    public void registerBroadcastReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(Remote.PROCESS_BROADCAST_ACTION);
        mContext.registerReceiver(processStatusReceiver, statusIntentFilter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mStartService = menu.findItem(R.id.action_start);
        mStartService.setVisible(true);
        mStartService.setTitle(mServiceRunning ? R.string.action_stop_service : R.string.action_start_service);
        mStartService.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        mMenuScreen = menu.findItem(R.id.action_menu_screen);
        mMenuScreen.setTitle(R.string.action_menu_screen);
        mMenuScreen.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);                     

        mStartRecord = menu.findItem(R.id.action_record);
        mStartRecord.setTitle(mRecording ? R.string.action_stop_record : R.string.action_start_record);
        mStartRecord.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);                     

        if (mServiceRunning) { 
            mMenuScreen.setEnabled(true);  
        } else {
            mMenuScreen.setEnabled(false);  
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_monitor_screen, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_start) {
            if (DEBUG) Log.v(TAG, "item:action_start");    
            mServiceRunning = !mServiceRunning;
            if (mServiceRunning) {
                startScreenMonitor();
            } else {
                ScreenRecord.getInstance().stopScreenMonitorService();     
            }

            getActivity().supportInvalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == R.id.action_play) {
            if (DEBUG) Log.v(TAG, "item:action_start");            
            startScreenCapture();
            return true;
        } else if (item.getItemId() == R.id.action_shot) {
            if (DEBUG) Log.v(TAG, "item:action_shot");
            if (Remote.isServiceRunning(mContext)) {          
                ScreenRecord.getInstance().startScreenShotService(); 
            } else {
                startScreenCapture();
            }
            return true;
        } else if (item.getItemId() == R.id.action_record) {
            mRecording = !mRecording;
            if (mServiceRunning) {
                if (mRecording) {
                    ScreenRecord.getInstance().startScreenRecordService();   
                } else {
                    ScreenRecord.getInstance().stopRecordingService();        
                }
            } 
            getActivity().supportInvalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == R.id.action_stream) {
            Intent intent = new Intent(mContext, ApplicationMonitor.class);
            intent.setAction(ApplicationMonitor.ACTION_MONITOR_SDCARD);
            startActivity(intent);    
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == REQUEST_CODE_SCREEN_SERVICE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                //Toast.makeText(getActivity(), R.string.record_user_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            Log.i(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = resultData;
            ScreenRecord.getInstance().startScreenMonitorService("Service Is Ready", resultCode, resultData);                            
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                ScreenRecord.getInstance().startScreenCaptureService(resultCode, resultData);                
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_SHOT) {
            if (resultCode == Activity.RESULT_OK) {
                //Services.getInstance().startScreenShotService(resultCode, resultData);                
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    
    
    public ActionBar getSupportActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }
    
    private Drawable oldBackground;
    private void changeActionBarColor() {

        int color = ThemePreference.getPrimaryColor();
        Drawable colorDrawable = new ColorDrawable(color);

        if (oldBackground == null) {
            mToolbar.setBackgroundDrawable(colorDrawable);
        } else {
            TransitionDrawable td = new TransitionDrawable(new Drawable[] { oldBackground, colorDrawable });
            mToolbar.setBackgroundDrawable(td);
            td.startTransition(200);
        }

        oldBackground = colorDrawable;
    }
    
}


