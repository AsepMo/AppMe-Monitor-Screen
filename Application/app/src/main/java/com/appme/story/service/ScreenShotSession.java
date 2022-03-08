package com.appme.story.service;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;
import java.io.IOException;

import com.appme.story.engine.graphics.ImageTransmogrifier;

public class ScreenShotSession {
    
    public static String TAG = ScreenShotSession.class.getSimpleName();
    private final Context ctxt;
    private final MonitorScreenService mScreenMonitorService;
    private final ToneGenerator beeper;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageTransmogrifier it;
    private static final int VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    
    public ScreenShotSession(Context ctxt, MonitorScreenService config, MediaProjection projection) {
        this.ctxt = ctxt.getApplicationContext();
        this.mScreenMonitorService = config;
        this.mMediaProjection = projection;
        this.beeper = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);    
        
    }
    
    public void start(Handler handler) {
        
        try {
            
            it = new ImageTransmogrifier(mScreenMonitorService);

            MediaProjection.Callback cb = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    mVirtualDisplay.release();
                }
            };

            mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenShot",
                                                              it.getWidth(), it.getHeight(),
                                                              ctxt.getResources().getDisplayMetrics().densityDpi,
                                                              VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
            mMediaProjection.registerCallback(cb, handler); 
            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
            
        } catch (Exception e) {
            throw new RuntimeException("Exception preparing recorder", e);
        }
    }

    public void stop() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mVirtualDisplay.release();
            mMediaProjection = null;
        }
    }
                                               
}
