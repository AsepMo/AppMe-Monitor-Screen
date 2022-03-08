package com.appme.story.receiver;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

import com.appme.story.R;
import com.appme.story.AppController;

public class RemoteReceiver extends BroadcastReceiver {

    public static String TAG = RemoteReceiver.class.getSimpleName();

    private OnSendBroadcastListener listener;
    public interface OnSendBroadcastListener {
        void onServiceReady(String message);
        void onScreenShotDone(String message);
        void onScreenRecordDone(String message);
        void onServiceShutDown(String message);
    }
    
    private OnRemoteReceiverListener mOnRemoteReceiverListener;
    public interface OnRemoteReceiverListener {
        void onUpdateStatus();
        void onStartStreaming();
        void onStopStreaming();
        void onClientCount(String message);
        void onServerIP(String message);
        void onServerPort();
        void onDisConnected();
        void onResult();
        void onExit();
    }

    public static void broadcastStatus(String status) {   
        Intent localIntent = new Intent(Remote.PROCESS_BROADCAST_ACTION);
        localIntent.putExtra(Remote.PROCESS_STATUS_KEY, status);
       AppController.getAppContext().sendBroadcast(localIntent);
    }
    
    public void setOnSendBroadcastListener(OnSendBroadcastListener listener) {
        this.listener = listener;
    }
    
    public void setOnRemoteReceiverListener(OnRemoteReceiverListener mOnRemoteReceiverListener) {
        this.mOnRemoteReceiverListener = mOnRemoteReceiverListener;
    }
    
    @Override
    public void onReceive(Context pContext, Intent pIntent) {
        String statusKey = "";
        String statusData = "";
        if (pIntent.hasExtra(Remote.PROCESS_STATUS_KEY)) {
            statusKey = pIntent.getStringExtra(Remote.PROCESS_STATUS_KEY);
        }
        if (pIntent.hasExtra(Remote.PROCESS_STATUS_MESSAGE)) {
            statusData = pIntent.getStringExtra(Remote.PROCESS_STATUS_MESSAGE);
        }
        final int serviceMessage = pIntent.getIntExtra(Remote.SERVICE_MESSAGE, 0);
        
        switch (statusKey) {
            case Remote.SERVICE_IS_READY:
                listener.onServiceReady(statusData);
                break;
            case Remote.SCREEN_SHOT_IS_DONE:
                listener.onScreenShotDone(statusData);
                break;  
            case Remote.SCREEN_RECORD_IS_DONE:
                listener.onScreenRecordDone(statusData);
                break;       
            case Remote.SERVICE_IS_SHUTDOWN:
                listener.onServiceShutDown(statusData);
                break;  
        }
        if (pIntent.getAction().equals(Remote.SERVICE_ACTION)) {
            
            // Service ask to update status
            if (serviceMessage == Remote.SERVICE_MESSAGE_UPDATE_STATUS){
                if(mOnRemoteReceiverListener != null)
                   mOnRemoteReceiverListener.onUpdateStatus();
             }
            // Service ask to start streaming
            if (serviceMessage == Remote.SERVICE_MESSAGE_START_STREAMING){
                if(mOnRemoteReceiverListener != null)
                   mOnRemoteReceiverListener.onStartStreaming();
             }
            // Service ask to stop streaming
            if (serviceMessage == Remote.SERVICE_MESSAGE_STOP_STREAMING){
                if(mOnRemoteReceiverListener != null)
                    mOnRemoteReceiverListener.onStopStreaming();
             }
            // Service ask to close application
            if (serviceMessage == Remote.SERVICE_MESSAGE_EXIT){
                if(mOnRemoteReceiverListener != null)
                    mOnRemoteReceiverListener.onExit();     
            }
            // Service ask to update client count
            if (serviceMessage == Remote.SERVICE_MESSAGE_GET_CLIENT_COUNT){          
                if(mOnRemoteReceiverListener != null){
                    final String clientCount = String.format(pContext.getResources().getString(R.string.connected_clients), pIntent.getIntExtra(Remote.SERVICE_MESSAGE_CLIENTS_COUNT, 0));                 
                    mOnRemoteReceiverListener.onClientCount(clientCount);
                 }
            }
            // Service ask to update server address
            if (serviceMessage == Remote.SERVICE_MESSAGE_GET_SERVER_ADDRESS) {
                if (AppController.isWiFIConnected()) {
                    if(mOnRemoteReceiverListener != null){
                        String serverIP = pIntent.getStringExtra(Remote.SERVICE_MESSAGE_SERVER_ADDRESS);                     
                        mOnRemoteReceiverListener.onServerIP(serverIP);
                     }
                 } else {
                     if(mOnRemoteReceiverListener != null)
                         mOnRemoteReceiverListener.onDisConnected();
                }
            }
            // Service ask notify HTTP Server port in use
            if (serviceMessage == Remote.SERVICE_MESSAGE_HTTP_PORT_IN_USE){
                if(mOnRemoteReceiverListener != null)
                    mOnRemoteReceiverListener.onServerPort();
            // Service ask notify HTTP Server ok
            if (serviceMessage == Remote.SERVICE_MESSAGE_HTTP_OK) {
                if(mOnRemoteReceiverListener != null)
                    mOnRemoteReceiverListener.onResult();
            }
            }
        }
    }

}


