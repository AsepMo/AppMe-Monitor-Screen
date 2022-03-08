package com.appme.story.engine.widget;

import android.animation.ValueAnimator;
import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.appme.story.R;
import java.io.FileDescriptor;
import java.io.IOException;

public class ScreenCapture implements View.OnClickListener {

    public static String TAG = ScreenCapture.class.getSimpleName();
    // private Activity mActivity;
    private Context mContext;
    private final LayoutInflater mInflater;
    private final View mView;

    private View mRootView;
    private View mPanel;
    private Surface mSurface;
    private SurfaceView mScreenMonitor;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private final ToneGenerator beeper;
    
    private View mMinifyButton;
    private LinearLayout mConsoleContainer;

    private static final int MINIFY_WIDTH = 132;
    private int CONSOLE_HEIGHT = 450;
    private int CONSOLE_WIDTH = 250;
    private int mScreenDensity;
    
    
    private View.OnClickListener mCancelClickListener;

    public ScreenCapture(Context context, MediaProjection mMediaProjection) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mView = mInflater.inflate(R.layout.layout_screen_monitor, null);
        this.mMediaProjection = mMediaProjection;
        this.beeper = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        
        init(mContext, mView);
    }

    private void create(View v) {
        this.mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        this.mParams = new WindowManager.LayoutParams();

        mParams.packageName = mContext.getPackageName();
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        int overlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        int alertWindow = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? overlay : alertWindow;
        mParams.format = PixelFormat.TRANSLUCENT;

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManager.addView(v, mParams);
    }
    
    public static ScreenCapture with(Context context, MediaProjection mMediaProjection) {
        return new ScreenCapture(context, mMediaProjection);
    }

    public ScreenCapture setCancelClickListener(View.OnClickListener cancelClickListener) {
        this.mCancelClickListener = cancelClickListener;
        return this;
    }

    public ScreenCapture stop(){    
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mVirtualDisplay.release();
            mMediaProjection = null;
        }
        if (mView != null) mWindowManager.removeView(mView);
        beeper.startTone(ToneGenerator.TONE_PROP_NACK);
        
        return this;
    }
    
    private void init(Context context, View view) {   
        create(view);
        mConsoleContainer = (LinearLayout) view.findViewById(R.id.debugkit_console_scroll_view);
        mMinifyButton = view.findViewById(R.id.debugkit_tools_minify);
        mScreenMonitor = (SurfaceView) view.findViewById(R.id.debugkit_console);  
        mSurface = mScreenMonitor.getHolder().getSurface();
        
        mPanel = view.findViewById(R.id.debugkit_tools_panel);

        mRootView = view.findViewById(R.id.debugkit_tools_close_button);
        mRootView.setOnClickListener(this);

        ViewGroup.LayoutParams layoutParams = mConsoleContainer.getLayoutParams();
        layoutParams.height = dpTopX(CONSOLE_HEIGHT);
        mConsoleContainer.setLayoutParams(layoutParams);

        layoutParams = mScreenMonitor.getLayoutParams();
        layoutParams.height = dpTopX(CONSOLE_HEIGHT);
        layoutParams.width = dpTopX(CONSOLE_WIDTH);
        mScreenMonitor.setLayoutParams(layoutParams);
        mScreenMonitor.setMinimumHeight(dpTopX(CONSOLE_HEIGHT));


        mMinifyButton.setTag(mMinifyButton.getId(), false);
        mMinifyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMinify();
                }
            });

        
        try {

            view.setOnTouchListener(new View.OnTouchListener() {
                    private WindowManager.LayoutParams paramsF = mParams;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;

                    @Override public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:

                                // Get current time in nano seconds.

                                initialX = paramsF.x;
                                initialY = paramsF.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                                paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                                mWindowManager.updateViewLayout(mView, paramsF);
                                break;
                        }
                        return false;
                    }
                });
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
    public void start() {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            mScreenDensity = metrics.densityDpi;
            
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenShot",
                                                                    mScreenMonitor.getWidth(), mScreenMonitor.getHeight(), mScreenDensity,
                                                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, null);
            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
            
        } catch (Exception e) {
            throw new RuntimeException("Exception preparing recorder", e);
        }
    }
    
   /* public ScreenCapture start(){
  
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, resultData);

        MediaProjection.Callback cb = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                mVirtualDisplay.release();
            }
        };

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                                                                mScreenMonitor.getWidth(), mScreenMonitor.getHeight(),
                                                                mContext.getResources().getDisplayMetrics().densityDpi,
                                                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, handler);
        mMediaProjection.registerCallback(cb, handler);
        
        return this;
    }*/
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.debugkit_tools_close_button: {
                    if (mCancelClickListener != null) {
                        mCancelClickListener.onClick(v);
                    }
                    break;
                }
        }
    }



    private int dpTopX(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mContext.getResources().getDisplayMetrics()));
    }
    /**
     * Switch the tool to minify mode.
     */
    private void switchMinify() {

        RotateAnimation rotateAnimation;
        ValueAnimator heightValueAnimator;
        ValueAnimator widthValueAnimator;

        if ((boolean) mMinifyButton.getTag(mMinifyButton.getId())) {
            rotateAnimation = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            heightValueAnimator = ValueAnimator.ofInt(0, dpTopX(CONSOLE_HEIGHT));
            widthValueAnimator = ValueAnimator.ofInt(dpTopX(MINIFY_WIDTH), dpTopX(CONSOLE_WIDTH));
            mMinifyButton.setTag(mMinifyButton.getId(), false);
        } else {
            rotateAnimation = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            heightValueAnimator = ValueAnimator.ofInt(dpTopX(CONSOLE_HEIGHT), 0);
            widthValueAnimator = ValueAnimator.ofInt(dpTopX(CONSOLE_WIDTH), dpTopX(MINIFY_WIDTH));
            mMinifyButton.setTag(mMinifyButton.getId(), true);
        }

        heightValueAnimator.setDuration(200);
        heightValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    mConsoleContainer.getLayoutParams().height = value.intValue();
                    mConsoleContainer.requestLayout();
                }
            });
        widthValueAnimator.setDuration(200);
        widthValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    mScreenMonitor.getLayoutParams().width = value.intValue();
                    mScreenMonitor.requestLayout();
                }
            });

        rotateAnimation.setDuration(200);
        rotateAnimation.setFillAfter(true);
        mMinifyButton.startAnimation(rotateAnimation);
        heightValueAnimator.setInterpolator(new AccelerateInterpolator());
        heightValueAnimator.start();
        widthValueAnimator.setInterpolator(new AccelerateInterpolator());
        widthValueAnimator.start();
    }



}
