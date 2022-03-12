package com.castellate.compendium;

import static com.castellate.compendium.protocol.Protocol.STATUS.READY_TO_SEND;
import static com.castellate.compendium.protocol.messages.Constants.ID_CD;

import android.util.Log;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;
import com.castellate.compendium.protocol.ProtocolViewModel;
import com.castellate.compendium.ws.WSMessages;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompanionDevice {
    private static final String TAG = "CompanionDevice";
    private WebSocketClient mWebSocketClient;
    private Protocol currentProtocol = null;
    private final String id;
    private final Queue<String> bufferedQueue = new ConcurrentLinkedQueue<>();

    public CompanionDevice(String companionId) {
        id = companionId;
    }

    public void runProtocol(Protocol protocol) throws ProtocolException {
        if (currentProtocol != null) {
            throw new ProtocolException("Existing protocol running, stop first");
        }
        protocol.putInProtocolData(ID_CD, id);
        currentProtocol = protocol;
        if (currentProtocol.getStatus() == READY_TO_SEND) {
            bufferedQueue.add(currentProtocol.getNextMessage());
        }
        initWebSocketClient();
    }

    private void sendWSSMessage(String message) {
        if (message == null) {
            Log.d(TAG, "Null send message, assume dummy, will ignore");
            return;
        }
        if (!mWebSocketClient.isOpen()) {
            Log.d(TAG, "Queuing message:" + message);
            bufferedQueue.add(message);
        } else {
            clearQueue();
            Log.d(TAG, "Sending message direct:" + message);
            mWebSocketClient.send(message);
        }
    }

    public int getProgress() {
        if (currentProtocol == null) {
            return 0;
        }
        return currentProtocol.getProgress();
    }

    private synchronized void clearQueue() {
        String msgToSend;
        Log.d(TAG, "Clearing Queue");
        while ((msgToSend = bufferedQueue.poll()) != null) {
            Log.d(TAG, "Sending queued message:" + msgToSend);
            mWebSocketClient.send(msgToSend);
        }
    }

    public String getCurrentStateOfProtocol() {
        if (currentProtocol != null) {
            return currentProtocol.getProtocolStateString();
        } else {
            return "Null";
        }
    }

    public void putInProtocolData(String key, String value) {
        if (currentProtocol != null) {
            currentProtocol.putInProtocolData(key, value);
        }

    }

    public void updateFromUI() {
        updateFromUI(null);
    }
    public void reset(){
        Log.d(TAG,"Resetting Companion Device");
        if(this.currentProtocol!=null) {
            this.currentProtocol.cleanUp();
            this.currentProtocol = null;
        }

        bufferedQueue.clear();
        if(this.mWebSocketClient.isOpen()){
            Log.d(TAG,"WebSocket Client is still open will try to close");
            if(!this.mWebSocketClient.isClosing()) {
                this.mWebSocketClient.close();
            }
        }
        if(this.mWebSocketClient.isClosed()){
            Log.d(TAG,"WebSocket Client is Closed");
        }

    }
    public void updateFromUI(Map<String, String> newData) {
        if (newData != null) {
            currentProtocol.putAllInProtocolData(newData);
        }
        currentProtocol.receivedUI();
        try {
            currentProtocol.prepareNextMessage();
            if (currentProtocol.getStatus() == READY_TO_SEND) {
                sendWSSMessage(currentProtocol.getNextMessage());

                currentProtocol.messageSent();
                if (currentProtocol.getStatus() == Protocol.STATUS.FINISHED) {
                    Log.d(TAG, "Calling Close From Finished UI");
                    mWebSocketClient.close();
                }
            }


        } catch (ProtocolMessageException e) {
            Log.e(TAG, "Exception preparing message", e);
            if(this.currentProtocol!=null){
                this.currentProtocol.setErrorStatus();
            }

        }
    }

    public void processMessage(JSONObject msg) {
        if (currentProtocol == null || msg == null) {
            return;
        }

        switch (currentProtocol.parseIncomingMessage(msg)) {
            case READY_TO_SEND:
                sendWSSMessage(currentProtocol.getNextMessage());

                currentProtocol.messageSent();
                if (currentProtocol.getStatus() == Protocol.STATUS.FINISHED) {
                    Log.d(TAG, "Calling Close");
                    mWebSocketClient.close();
                }
                break;
            case AWAITING_UI:
                Log.d(TAG, "Awaiting UI");
                break;
            case AWAITING_RESPONSE:
                Log.d(TAG, "Awaiting response");
                break;
            case IDLE:
                break;
        }

    }

    public String getProtocolData(String field) {
        if (currentProtocol != null) {
            return currentProtocol.getProtocolData(field);
        } else {
            return "";
        }
    }

    public void processMessage(String msg) {
        if (currentProtocol != null) {
            currentProtocol.parseIncomingMessage(msg);
        }
    }
    public void setProtocolInError(){
        if(currentProtocol!=null){
            currentProtocol.setErrorStatus();
        }
    }
    public void setProtocolViewModel(ProtocolViewModel model) {
        if (currentProtocol != null) {
            currentProtocol.setProtocolViewModel(model);
        }
    }

    public void initWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://10.0.2.2:8001");
        } catch (URISyntaxException e) {
            Log.d(TAG,"WebSocketClient exception",e);
            if(this.currentProtocol!=null){
                this.currentProtocol.setErrorStatus();
            }

            return;
        }
        mWebSocketClient = new WebSocketClient(uri, new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.d(TAG, "Connected");
                clearQueue();
            }

            @Override
            public void onMessage(String s) {
                Log.d(TAG, "Received:" + s);
                JSONObject msg = WSMessages.parse(s);
                if (msg == null) {
                    return;
                }
                try {
                    switch (msg.getString(WSMessages.MSG_TYPE)) {
                        case WSMessages.MsgTypes.INITRESP:
                            Log.d(TAG, "Process INITRESP");
                            processMessage(msg);
                            break;
                        case WSMessages.MsgTypes.DELIVER:
                            Log.d(TAG, "Process Deliver");
                            processMessage(msg.optJSONObject(WSMessages.DeliverMsg.MSG));
                            break;
                        default:
                            Log.d(TAG, "Unknown message type:" + msg.getString(WSMessages.MSG_TYPE));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing JSON message:", e);
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.d(TAG, "WebSocket Closed");
                //Logger.LogInfo("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                if(currentProtocol!=null){
                    currentProtocol.setErrorStatus();
                }
                Log.d(TAG, "WebSocket Error", e);
                //Logger.LogInfo("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }
}
