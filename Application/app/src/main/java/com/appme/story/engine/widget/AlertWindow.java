package com.appme.story.engine.widget;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

public class AlertWindow {
    
    public static String TAG = AlertWindow.class.getSimpleName();
    private Context mContext;
    public static WindowManager mWindowManager;
    public static WindowManager.LayoutParams mParams;
    private View mContentView;

    private boolean isShowing;

    public AlertWindow(Context context) {
        this.mContext = context.getApplicationContext();
        this.create();
    }

    private void create() {
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
        
    }

    /**
     * Set the view to show.
     *
     * @param layoutId target layout id.
     */
    public void setContentView(int layoutId) {
        setContentView(LayoutInflater.from(mContext).inflate(layoutId, null, false));
    }

    /**
     * Set the view to show.
     *
     * @param view target view.
     */
    public void setContentView(View view) {
        this.mContentView = view;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(-1, -2);
        } else {
            params.width = -1;
            params.height = -2;
        }
        this.mContentView.setLayoutParams(params);
        try {

            mContentView.setOnTouchListener(new View.OnTouchListener() {
                    private WindowManager.LayoutParams paramsF = mParams;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
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
                                mWindowManager.updateViewLayout(mContentView, paramsF);
                                break;
                        }
                        return false;
                    }
                });

        } catch (Exception e) {
            // TODO: handle exception
        } 
    }

    /**
     * AlertWindow is displayed.
     *
     * @return true, otherwise is false.
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Display the alert window.
     */
    public void show() {
        if (isShowing) {
            Log.w("AlertWindow", "AlertWindow is already displayed.");
        } else {
            isShowing = true;
            mWindowManager.addView(mContentView, mParams);
        }
    }

    /**
     * Dismiss the
     */
    public void dismiss() {
        if (!isShowing) {
            Log.w("AlertWindow", "AlertWindow is not displayed.");
        } else if (mContentView != null) {
            isShowing = false;
            mWindowManager.removeViewImmediate(mContentView);
        }
    }
    
}
