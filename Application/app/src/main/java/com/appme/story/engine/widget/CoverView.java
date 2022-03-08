package com.appme.story.engine.widget;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.appme.story.R;
import java.io.FileDescriptor;
import java.io.IOException;

public class CoverView extends RelativeLayout implements View.OnClickListener {

    public static String TAG = CoverView.class.getSimpleName();
    private Activity mActivity;
    private Context mContext;
    private View mRootView;
    private View mPanel;
    private ImageView mScreenMonitor;

    private View mMinifyButton;
    private LinearLayout mConsoleContainer;

    private static final int MINIFY_WIDTH = 132;
    private int CONSOLE_HEIGHT = 450;
    private int CONSOLE_WIDTH = 250;

    private float dX;
    private float dY;

    private View.OnClickListener mCancelClickListener;

    public CoverView(Context context) {
        this(context, null, 0);
        init(context, null);
    }

    public CoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init(context, attrs);
    }

    public CoverView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.layout_alert_window, this);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {
        setBackgroundColor(Color.BLACK);

        mContext = context;
        mConsoleContainer = (LinearLayout) findViewById(R.id.debugkit_console_scroll_view);
        mMinifyButton = findViewById(R.id.debugkit_tools_minify);
        mScreenMonitor = (ImageView) findViewById(R.id.debugkit_console);  

        mPanel = findViewById(R.id.debugkit_tools_panel);

        mRootView = findViewById(R.id.debugkit_tools_close_button);
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
                    mScreenMonitor.setImageBitmap(bitmap);
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

    public void setCancelClickListener(OnClickListener cancelClickListener) {
        this.mCancelClickListener = cancelClickListener;
    }

    private int dpTopX(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
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
