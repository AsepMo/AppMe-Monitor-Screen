package com.appme.story.engine.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback, MjpegInputStream.OnStreamFinishedListener {
    public interface OnMjpegCompletedListener {
        void onCompeleted();

        void onFailure(@NonNull String error);
    }

    protected static final int SIZE_STANDARD = 1;
    protected static final int SIZE_BEST_FIT = 4;
    protected static final int SIZE_FULLSCREEN = 8;

    private HandlerThread mHandlerThread;       //加載資料用的thread
    private Handler mHandler;                   //控制thread
    private boolean isShowFps = true;           //是否顯示fps

    private MjpegRunnable mjpegRunnable;
    private OnMjpegCompletedListener listener = null;

    public MjpegView(Context context) {
        this(context, null, 0);
    }

    public MjpegView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MjpegView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        setFocusable(true);

        //建立Thread
        mHandlerThread = new HandlerThread("MjpegView_" + this.hashCode());
        mjpegRunnable = new MjpegRunnable(MjpegView.SIZE_BEST_FIT, null, getHolder(), this.isShowFps, false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mjpegRunnable.setRunning(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mHandlerThread.quitSafely();
        } else {
            mHandlerThread.quit();
        }
        mHandler = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mjpegRunnable != null) {
            mjpegRunnable.setCreatedSurface(true);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mjpegRunnable != null) {
            mjpegRunnable.setSurfaceSize(i1, i2);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mjpegRunnable != null) {
            mjpegRunnable.setCreatedSurface(false);
        }
    }

    public void setOnMjpegCompletedListener(OnMjpegCompletedListener listener) {
        this.listener = listener;
    }

    public void startPlayback(MjpegInputStream inputStream) {
        try {
            mjpegRunnable.setRunning(true);
            if (mHandler != null) {
                mHandler.removeCallbacks(mjpegRunnable);
            } else {
                mHandler = new Handler(mHandlerThread.getLooper());
            }
            inputStream.setListener(this);
            mjpegRunnable.setSurfaceSize(getWidth(), getHeight());
            mjpegRunnable.setInputStream(inputStream);
            mHandler.post(mjpegRunnable);
        } catch (Exception e) {
            listener.onFailure("" + e.getMessage());
        }
    }

    public void stopPlayback() {
        if (mjpegRunnable != null) {
            mjpegRunnable.setRunning(false);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(mjpegRunnable);
        }
    }

    private void showFailure(String error) {
        if(listener != null){
           listener.onFailure("" + error);
        }
    }

    public void setFPSEnable(boolean enable) {
        this.isShowFps = enable;
        mjpegRunnable.setFpsEnable(enable);
    }

    public void startPlayback(String url) {
        new URLAsyncTask(new URLAsyncTask.OnURLAsyncTaskListener(){
            @Override 
            public void onDone(InputStream stream){
                startPlayback(new MjpegInputStream(stream));
            }
            @Override
            public void onFailure(String error){
                showFailure("" + error);
            }
        }).execute(url);
    }

    @Override
    public void onFinished() {
        stopPlayback();
        new Handler(Looper.myLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onCompeleted();
                }
            }
        });
    }

    private static class URLAsyncTask extends AsyncTask<String, Object, InputStream> {
        private OnURLAsyncTaskListener listener;
        public interface OnURLAsyncTaskListener {
            void onDone(InputStream input);
            void onFailure(String error);
        }
        
        public URLAsyncTask(OnURLAsyncTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected InputStream doInBackground(String... urls) {
            try {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };
                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                URL url = new URL(urls[0]);
                if (url.getProtocol().toUpperCase().equals("HTTPS")) {
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.addRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Mobile Safari/537.36"
                    );
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestMethod("GET");
                    if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                        return connection.getInputStream();
                    }
                } else {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.addRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Mobile Safari/537.36"
                    );
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestMethod("GET");
                    if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                        return connection.getInputStream();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                
            }
            return null;
        }

        @Override
        protected void onPostExecute(InputStream inputStream) {
            super.onPostExecute(inputStream);
            if (inputStream != null) {
                if(listener != null)
                    listener.onDone(inputStream);
            } else {
                if(listener != null)
                    listener.onFailure("InputStream is null.");
            }
        }
    }

}
