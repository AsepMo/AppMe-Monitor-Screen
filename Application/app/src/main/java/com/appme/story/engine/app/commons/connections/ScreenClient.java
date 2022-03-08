package com.appme.story.engine.app.commons.connections;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ScreenClient implements Callable {
    private final Socket socket;
    private final OutputStreamWriter outputStreamWriter;
    private byte[] jpegImage;

    public ScreenClient(final Socket socket) throws IOException {
        this.socket = socket;
        outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
    }

    public void closeSocket() {
        try {
            outputStreamWriter.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientAddress() {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    public void sendHeader() throws IOException {
        outputStreamWriter.write("HTTP/1.1 200 OK\r\n");
        outputStreamWriter.write("Content-Type: multipart/x-mixed-replace; boundary=y5exa7CYPPqoASFONZJMz4Ky\r\n");
        outputStreamWriter.write("Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n");
        outputStreamWriter.write("Pragma: no-cache\r\n");
        outputStreamWriter.write("Connection: keep-alive\r\n");
        outputStreamWriter.write("\r\n");
        outputStreamWriter.flush();
    }

    public void registerImage(final byte[] newJpegImage) {
        jpegImage = newJpegImage;
    }

    @Override
    public Object call() throws Exception {
        outputStreamWriter.write("--y5exa7CYPPqoASFONZJMz4Ky\r\n");
        outputStreamWriter.write("Content-Type: image/jpeg\r\n");
        outputStreamWriter.write("Content-Length: " + jpegImage.length + "\r\n");
        outputStreamWriter.write("\r\n");
        outputStreamWriter.flush();
        socket.getOutputStream().write(jpegImage);
        socket.getOutputStream().flush();
        outputStreamWriter.write("\r\n");
        outputStreamWriter.flush();
        return null;
    }
}
