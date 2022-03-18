/*
 *  © Copyright 2022. University of Surrey
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.castellate.compendium;

import static com.castellate.compendium.protocol.Protocol.STATUS.READY_TO_SEND;
import static com.castellate.compendium.protocol.messages.Constants.ID_CD;

import android.util.Log;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.ProtocolViewModel;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;
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

/**
 * Represents a Companion Device, handles protocol and web socket client management
 */
public class CompanionDevice {
    private static final String TAG = "CompanionDevice";
    private final String id;

    //Temporary buffer to hold outgoing messages until the web socket client is initialised
    private final Queue<String> bufferedQueue = new ConcurrentLinkedQueue<>();

    private WebSocketClient mWebSocketClient;
    private Protocol currentProtocol = null;
    private boolean errorMessagePrepared = false;

    /**
     * Construct a new Companion Device with the specified companionId
     * @param companionId name of the Companion Device
     */
    public CompanionDevice(String companionId) {
        id = companionId;
    }

    /**
     * Runs the specified Protocol on this CompanionDevice, only one protocol can be run at
     * a time.
     *
     * @param protocol protocol to be run
     * @throws ProtocolException thrown if a protocol is already running
     */
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

    /**
     * Sends the specified message, queuing it in a buffer if the web socket client is not
     * yet initialise. As such, this does not guarantee immediate send, however, it will maintain
     * the ordering of messages sent via this method.
     *
     * @param message message to send
     */
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

    /**
     * Get the progress of the current protocol, which will be calculated via the current
     * state.
     * @return progress as a percentage
     */
    public int getProgress() {
        if (currentProtocol == null) {
            return 0;
        }
        return currentProtocol.getProgress();
    }

    /**
     * Clears the current message queue by polling to send the oldest first
     */
    private synchronized void clearQueue() {
        String msgToSend;
        Log.d(TAG, "Clearing Queue");
        while ((msgToSend = bufferedQueue.poll()) != null) {
            Log.d(TAG, "Sending queued message:" + msgToSend);
            mWebSocketClient.send(msgToSend);
        }
    }

    /**
     * Get the string representation of the current state of the running protocol or the string
     * "Null" if no protocol is running
     * @return current state of the protocol or the string "Null"
     */
    public String getCurrentStateOfProtocol() {
        if (currentProtocol != null) {
            return currentProtocol.getProtocolStateString();
        } else {
            return "Null";
        }
    }

    /**
     * Puts the specified key-value pair into the underlying Protocol data associated with
     * the currently running protocol. This will overwrite any existing data.
     * @param key name
     * @param value value
     */
    public void putInProtocolData(String key, String value) {
        if (currentProtocol != null) {
            currentProtocol.putInProtocolData(key, value);
        }

    }

    /**
     * Triggers the protocol to continue after it has been waiting for a UI update. This does
     * not pass any additional data to the protocol
     */
    public void updateFromUI() {
        updateFromUI(null);
    }

    public void reset() {
        Log.d(TAG, "Resetting Companion Device");
        if (this.currentProtocol != null) {
            this.currentProtocol.cleanUp();
            this.currentProtocol = null;
        }

        bufferedQueue.clear();
        if (this.mWebSocketClient.isOpen()) {
            Log.d(TAG, "WebSocket Client is still open will try to close");
            if (!this.mWebSocketClient.isClosing()) {
                this.mWebSocketClient.close();
            }
        }
        if (this.mWebSocketClient.isClosed()) {
            Log.d(TAG, "WebSocket Client is Closed");
        }

    }

    /**
     * Triggers an update from the UI with the provided data being added into the Protocol Data
     * first. This should be used when waiting for some UI action, like a biometric approval or
     * signature. Those actions happen within the UI framework and when complete they should
     * call this method to pass that newly created data back to the protocol so that it can
     * continue processing messages
     *
     * @param newData key-value data to add to the Protocol Data
     */
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
            if (this.currentProtocol != null) {
                this.currentProtocol.setErrorStatus();
            }

        }
    }

    /**
     * Process an incoming protocol message formatted as JSON object
     *
     * If no protocol is active the message is ignored
     *
     * @param msg JSONObject containing the message to be processed
     */
    public void processMessage(JSONObject msg) {
        if (currentProtocol == null || msg == null) {
            return;
        }
        //process the message and decide the what to do next
        switch (currentProtocol.parseIncomingMessage(msg)) {
            case READY_TO_SEND:
                //We have completed processing on the incoming message and its corresponding reply
                //and we are ready to send
                sendWSSMessage(currentProtocol.getNextMessage());

                currentProtocol.messageSent();
                if (currentProtocol.getStatus() == Protocol.STATUS.FINISHED) {
                    Log.d(TAG, "Calling Close");
                    mWebSocketClient.close();
                }
                break;
            case AWAITING_UI:
                //We need something from the UI before we can process the outgoing message
                Log.d(TAG, "Awaiting UI");
                break;
            case AWAITING_RESPONSE:
                //We have sent a message and are now awaiting a response
                Log.d(TAG, "Awaiting response");
                break;
            case IDLE:
                break;
        }

    }

    /**
     * Gets a particular value from the Protocol Data
     * @param field name of field to retrieve
     * @return value of that field or "" if no protocol is running
     */
    public String getProtocolData(String field) {
        if (currentProtocol != null) {
            return currentProtocol.getProtocolData(field);
        } else {
            return "";
        }
    }

    /**
     * TODO Check if we can combine this with other processMessage, in particular why do we not need the response?
     * @param msg message to process
     */
    public void processMessage(String msg) {
        if (currentProtocol != null) {
            currentProtocol.parseIncomingMessage(msg);
        }
    }

    /**
     * Sets the protocol to be in a state of error causing it to try ot send an error message
     * to the requester
     * @param errorCode error code
     * @param errorMessage error message
     */
    public void setProtocolInError(int errorCode, String errorMessage) {
        if (currentProtocol != null) {
            if (!errorMessagePrepared) {
                String errorMsg = currentProtocol.prepareErrorMessage(errorCode, errorMessage);
                if (errorMsg != null) {
                    this.sendWSSMessage(errorMsg);
                }
                errorMessagePrepared = true;
            }
            currentProtocol.setErrorStatus();
        }
    }

    /**
     * Sets the protocol to be in error with a generic unknown error, ideally call
     * setProtocolInError with error code and error message.
     */
    public void setProtocolInError() {
        setProtocolInError(100, "Unknown Error");
    }

    /**
     * Set the view model to use for the protocol. This view model will receive updates
     * on the progress of the protocol allowing the UI to be updated
     * @param model view model to use
     */
    public void setProtocolViewModel(ProtocolViewModel model) {
        if (currentProtocol != null) {
            currentProtocol.setProtocolViewModel(model);
        }
    }

    /**
     * Initialise the web socket client
     */
    public void initWebSocketClient() {
        URI uri;
        try {
            //TODO externalise this string to a config file
            uri = new URI("wss://compendium.dev.castellate.com:8001");
        } catch (URISyntaxException e) {
            Log.d(TAG, "WebSocketClient exception", e);
            if (this.currentProtocol != null) {
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
                Log.d(TAG, "WebSocket Closed" + s);
                //Logger.LogInfo("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                if (currentProtocol != null) {
                    currentProtocol.setErrorStatus();
                }
                Log.d(TAG, "WebSocket Error", e);
                //Logger.LogInfo("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }
}
