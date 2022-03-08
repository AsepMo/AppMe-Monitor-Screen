package com.appme.story.engine.widget;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.appme.story.AppController;
import com.appme.story.engine.app.commons.connections.ScreenClient;
import com.appme.story.service.ForegroundService;

public class MjpegStreamer {
    private static final String TAG = MjpegStreamer.class.getSimpleName();
    private final Object lock = new Object();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private JpegStreamerThread jpegStreamerThread;
    private volatile boolean isThreadRunning;

    private class JpegStreamerThread extends Thread {
        private byte[] currentJPEG;
        private byte[] lastJPEG;
        private int sleepCount;
        private Future future;

        public JpegStreamerThread() {
            super("JpegStreamerThread");
        }

        private void sendLastJPEGToClients() {
            sleepCount = 0;
            for (final ScreenClient currentClient : AppController.getClientQueue()) {
                currentClient.registerImage(lastJPEG);
                synchronized (lock) {
                    if (!isThreadRunning) return;
                    future = threadPool.submit(currentClient);
                }
                try {
                    future.get(AppController.getApplicationSettings().getClientTimeout(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Log.d(TAG, "Remove client: " + currentClient.getClientAddress() + " " + e.toString());
                    e.printStackTrace();
                    currentClient.closeSocket();
                    ForegroundService.removeClient(currentClient);
                }
            }
        }

        public void run() {
            while (!isInterrupted()) {
                if (!isThreadRunning) break;
                currentJPEG = AppController.getJPEGQueue().poll();
                if (currentJPEG == null) {
                    try {
                        sleep(16);
                    } catch (InterruptedException e) {
                        continue;
                    }
                    sleepCount++;
                    if (sleepCount >= 60) sendLastJPEGToClients();
                } else {
                    lastJPEG = currentJPEG;
                    sendLastJPEGToClients();
                }

            }
        }

    } // JpegStreamerThread

    public void addClient(final Socket clientSocket) {
        synchronized (lock) {
            if (!isThreadRunning) return;

            try {
                final ScreenClient newClient = new ScreenClient(clientSocket);
                newClient.sendHeader();
                ForegroundService.addClient(newClient);
                Log.d(TAG, "Added one client: " + newClient.getClientAddress());
            } catch (IOException e) {
                //NOOP
            }
        }
    }

    public void start() {
        synchronized (lock) {
            if (isThreadRunning) return;

            jpegStreamerThread = new JpegStreamerThread();
            jpegStreamerThread.start();

            isThreadRunning = true;
            Log.d(TAG, "JPEG Streamer started");
        }
    }

    public void stop() {
        synchronized (lock) {
            if (!isThreadRunning) return;
            isThreadRunning = false;

            jpegStreamerThread.interrupt();
            threadPool.shutdownNow();

            for (ScreenClient currentClient : AppController.getClientQueue())
                currentClient.closeSocket();

            ForegroundService.clearClients();
            Log.d(TAG, "JPEG Streamer stopped");
        }
    }
}
