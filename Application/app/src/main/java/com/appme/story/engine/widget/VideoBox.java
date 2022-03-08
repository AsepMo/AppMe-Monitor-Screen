package com.appme.story.engine.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.database.Cursor;
import android.net.Uri;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import com.appme.story.R;
import com.bumptech.glide.Glide;

public class VideoBox extends RelativeLayout {

    public static String TAG = VideoBox.class.getSimpleName();
    private Context mContext;
    private VideoView mVideoView;
    private ImageView mVideoThumbnails;
    private Integer shortAnimDuration;
    private MediaController mediaControls;
    
    private int position  = 0;
    public VideoBox(Context context) {
        super(context);
        init(context, null);
    }

    public VideoBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VideoBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("unused")
    public VideoBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        setBackgroundColor(Color.TRANSPARENT);
        shortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setKeepScreenOn(true);
        // Instantiate and add VideoView for rendering
        final LayoutInflater li = LayoutInflater.from(getContext());
        View mVideoFrame = li.inflate(R.layout.layout_video_box, this, false);
        addView(mVideoFrame);

        mVideoView = (VideoView) mVideoFrame.findViewById(R.id.videoView);   
        mVideoThumbnails = (ImageView)mVideoFrame.findViewById(R.id.videoThumbnails);
        mVideoThumbnails.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    File file = new File(mContext.getExternalFilesDir(null), "andcorder.mp4");    
                    if(file.exists()){
                        setVideoPath(file.getAbsolutePath());
                    }
                       
                }
            });

        if (mediaControls == null) {
            mediaControls = new MediaController(mContext);
        }
        shortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime); 
    }
    
    public void setImageBitmap(Bitmap bitmap) {
        mVideoThumbnails.setImageBitmap(bitmap);
        crossFade(findViewById(R.id.videoView), findViewById(R.id.videoThumbnails));                         
   }

    public void setVideoThumbnail(String thumbnail) {
        Glide.with(mContext)
            .load(thumbnail)
            .placeholder(R.drawable.team_viewer)
            .into(mVideoThumbnails);

        crossFade(findViewById(R.id.videoView), findViewById(R.id.videoThumbnails));                         
    }
    
    public void setVideoPath(String video) {
        try {
            mVideoView.setMediaController(mediaControls);
            mVideoView.setVideoURI(Uri.parse(video));
            mVideoView.requestFocus();
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    // Close the progress bar and play the video
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        //progressDialog.dismiss();
                        mVideoView.seekTo(position);
                        if (position == 0) {
                            mVideoView.start();              
                        } else {
                            mVideoView.pause();

                        }
                    }
                });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mVideoView.seekTo(0);
                        mVideoView.pause();
                        File file = new File(mContext.getExternalFilesDir(null), "screenshot.png");    
                        if(file.exists())
                        crossFade(findViewById(R.id.videoView), findViewById(R.id.videoThumbnails));                         
                    }
                });
            crossFade(findViewById(R.id.videoThumbnails), findViewById(R.id.videoView));                         
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public void crossFade(final View toHide, View toShow) {

        toShow.setAlpha(0.0f);
        toShow.setVisibility(View.VISIBLE);

        toShow.animate()
            .alpha(1.0f)
            .setDuration(shortAnimDuration)
            .setListener(null);

        toHide.animate()
            .alpha(0.0f)
            .setDuration(shortAnimDuration)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    toHide.setVisibility(View.GONE);
                }
            });
    }
}
