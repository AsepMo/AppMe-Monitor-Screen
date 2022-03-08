package com.appme.story.engine.app.commons.connections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.util.Log;

import com.appme.story.AppController;
import com.appme.story.engine.widget.MjpegStreamer;

public class ScreenServer {
    private static final String TAG = ScreenServer.class.getSimpleName();
    
    public static final int SERVER_STATUS_UNKNOWN = -1;
    public static final int SERVER_OK = 0;
    public static final int SERVER_ERROR_ALREADY_RUNNING = 1;
    public static final int SERVER_ERROR_PORT_IN_USE = 2;
    public static final int SERVER_ERROR_UNKNOWN = 3;

    private final Object lock = new Object();
    private static final int SEVER_SOCKET_TIMEOUT = 50;

    private volatile boolean isThreadRunning;

    private ServerSocket serverSocket;
    private HTTPServerThread httpServerThread;
    private MjpegStreamer jpegStreamer;

    private class HTTPServerThread extends Thread {


        public HTTPServerThread() {
            super("HTTPServerThread");
        }

        public void run() {
            while (!isInterrupted()) {
                synchronized (lock) {
                    if (!isThreadRunning) continue;
                    try {
                        final Socket clientSocket = ScreenServer.this.serverSocket.accept();
                        final BufferedReader bufferedReaderFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        final String requestLine = bufferedReaderFromClient.readLine();

                        if (requestLine == null) continue;

                        final String[] requestUriArray = requestLine.split(" ");

                        String requestUri = "NOT_SET";
                        if (requestUriArray.length >= 2) requestUri = requestUriArray[1];

                        switch (requestUri) {
                            case "/":
                                sendMainPage(clientSocket);
                                break;
                            case "/screen_stream.mjpeg":
                                ScreenServer.this.jpegStreamer.addClient(clientSocket);
                                break;
                            case "/favicon.ico":
                                sendFavicon(clientSocket);
                                break;
                            default:
                                sendNotFound(clientSocket);
                        }
                    } catch (SocketTimeoutException ex) {
                        // NOOP
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } // while
        } // run()

        private void sendMainPage(final Socket socket) throws IOException {
            try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {
                outputStreamWriter.write("HTTP/1.1 200 OK\r\n");
                outputStreamWriter.write("Content-Type: text/html\r\n");
                outputStreamWriter.write("Connection: close\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.write(AppController.getIndexHtmlPage());
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
            }
        }

        private void sendFavicon(Socket socket) throws IOException {
            try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {
                outputStreamWriter.write("HTTP/1.1 200 OK\r\n");
                outputStreamWriter.write("Content-Type: image/png\r\n");
                outputStreamWriter.write("Connection: close\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
                socket.getOutputStream().write(AppController.getIconBytes());
                socket.getOutputStream().flush();
            }
        }

        private void sendNotFound(final Socket socket) throws IOException {
            try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {
                outputStreamWriter.write("HTTP/1.1 301 Moved Permanently\r\n");
                outputStreamWriter.write("Location: " + AppController.getServerAddress() + "\r\n");
                outputStreamWriter.write("Connection: close\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
            }
        }

    }


    public int start() {
        synchronized (lock) {
            if (isThreadRunning) return SERVER_ERROR_ALREADY_RUNNING;
            try {
                serverSocket = new ServerSocket(AppController.getApplicationSettings().getServerPort());
                serverSocket.setSoTimeout(SEVER_SOCKET_TIMEOUT);

                jpegStreamer = new MjpegStreamer();
                jpegStreamer.start();

                httpServerThread = new HTTPServerThread();
                httpServerThread.start();

                isThreadRunning = true;
                Log.d(TAG, "HTTP server started on port: " + AppController.getApplicationSettings().getServerPort());
            } catch (BindException e) {
                return SERVER_ERROR_PORT_IN_USE;
            } catch (IOException e) {
                  e.printStackTrace();
                return SERVER_ERROR_UNKNOWN;
            }
        }
        return SERVER_OK;
    }

    public void stop() {
        synchronized (lock) {
            if (!isThreadRunning) return;
            isThreadRunning = false;

            httpServerThread.interrupt();
            httpServerThread = null;

            jpegStreamer.stop();
            jpegStreamer = null;

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
            Log.d(TAG, "HTTP server stopped");
        }
    }
}
