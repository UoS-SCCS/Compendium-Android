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

package com.castellate.compendium.protocol;

import android.util.Log;

import com.castellate.compendium.protocol.error.ErrorEncryptedSubMessage;
import com.castellate.compendium.protocol.error.ErrorProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Protocol class the underpins all other protocols. Implements core processing logic
 * as well defining necessary abstract methods
 */
public abstract class Protocol {
    private static final String TAG = "Protocol";
    protected final Map<String, String> protocolData = new HashMap<>();
    protected ProtocolMessage nextMessage = null;
    protected ProtocolViewModel model;
    protected STATUS status = STATUS.IDLE;

    /**
     * Get a list of message classes that define this protocol
     * @return message classes that define this protocol
     */
    public abstract Class<? extends ProtocolMessage>[] getMessages();

    /**
     * Get the ordinal (index) of the current state of the protocol
     * @return integer ordinal of the current state
     */
    public abstract int getStateOrdinal();

    /**
     * Checks if the protocol is finished
     * @return true if it has, false it not
     */
    public abstract boolean isFinished();

    /**
     * Get the protocol state as a string, the name of the enum
     * @return string name of the protocol state
     */
    public abstract String getProtocolStateString();

    /**
     * Process an incoming message
     * @param protoMessage message to process
     * @return true if successful, false it not
     * @throws ProtocolMessageException
     */
    public abstract boolean processIncomingMessage(ProtocolMessage protoMessage) throws ProtocolMessageException;

    /**
     * Get the protocol status
     * @return
     */
    public STATUS getStatus() {
        return status;
    }

    /**
     * Share the status with the ProtocolViewModel if set
     * @param status status to be shared
     * @return status that was shared
     */
    public STATUS shareStatus(STATUS status) {
        if (model != null) {
            model.postProtocolStatus(status);
        }
        return status;
    }

    /**
     * Parse an incoming message and return the status of the protocol
     * @param msg JSONObject containing the incoming message
     * @return status of the protocol after the processing
     */
    public STATUS parseIncomingMessage(JSONObject msg) {
        ProtocolMessage protoMessage;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();

        } catch (IllegalAccessException | InstantiationException e) {
            status = STATUS.ERROR;
            return shareStatus(status);
        }
        if (!protoMessage.parse(msg)) {
            status = STATUS.AWAITING_RESPONSE;
            return shareStatus(status);
        }
        try {
            if (!processIncomingMessage(protoMessage) || !protoMessage.processSubMessages(protocolData)) {

                status = STATUS.AWAITING_RESPONSE;
                return shareStatus(status);
            }
        } catch (ProtocolMessageException e) {
            Log.e(TAG, "Exception processing incoming message", e);
            status = STATUS.ERROR;
            return shareStatus(status);
        }
        if (advancedStateTriggerUI()) {
            if (isFinished()) {
                status = STATUS.FINISHED;
                return shareStatus(status);
            }
            status = STATUS.AWAITING_UI;
            return shareStatus(status);
        } else if (getStateOrdinal() == 0) {
            status = STATUS.FINISHED;
            return shareStatus(status);
        } else {

            try {
                prepareNextMessage();
            } catch (ProtocolMessageException e) {
                Log.e(TAG, "Exception creating next message", e);
                status = STATUS.ERROR;
                return shareStatus(status);
            }
        }
        status = STATUS.READY_TO_SEND;
        return shareStatus(status);
    }

    /**
     * Put data into the ProtocolData store
     * @param field name
     * @param value value
     */
    public void putInProtocolData(String field, String value) {
        this.protocolData.put(field, value);
    }

    /**
     * Advance the state and check if we need to trigger the UI
     * @return true if UI should be triggered, false if not
     */
    public abstract boolean advancedStateTriggerUI();

    /**
     * Parse an incoming message, utility wrapper that first parses the string as a JSONObject
     * then calls the respective parseIncomingMessage
     * @param msg string message to parse
     * @return status of the protocol after processing the incoming message
     */
    public STATUS parseIncomingMessage(String msg) {
        try {
            return parseIncomingMessage(new JSONObject(msg));
        } catch (JSONException e) {
            status = STATUS.AWAITING_RESPONSE;
            return shareStatus(status);
        }

    }

    /**
     * Cleanup the protocol, clear any protocol data and set the status to idle, set the shared
     * model to null
     */
    public void cleanUp() {
        this.protocolData.clear();
        if (this.model != null) {
            this.model.postProtocolStatus(STATUS.IDLE);
        }
        this.model = null;

    }

    /**
     * Get a value from the ProtocolData
     * @param field name of field to get
     * @return value of field
     */
    public String getProtocolData(String field) {
        return protocolData.get(field);
    }

    /**
     * Get the next message as a WebSocket encoded string
     * @return string to write to the websocket
     */
    public String getNextMessage() {
        return nextMessage.getWebSocketMsg(protocolData);
    }

    /**
     * Get the total number of states, used to calculate progress
     * @return integer number of states
     */
    public abstract int getTotalStates();

    /**
     * Get progress as a rounded integer percentage
     * @return percentage progress
     */
    public int getProgress() {
        if (getStatus() == STATUS.FINISHED) {
            return 100;
        } else {
            double increment = ((double) 100) / getTotalStates();
            return (int) Math.round(increment * getStateOrdinal());
        }
    }

    /**
     * Called to indicate the message has been sent and check if we have finished
     */
    public void messageSent() {
        nextMessage = null;
        //We should never trigger UI on sending
        advancedStateTriggerUI();
        if (isFinished()) {
            status = STATUS.FINISHED;
            shareStatus(status);
        }
        //We can only advanced back to zero once finished
        if (getStateOrdinal() == 0) {
            status = STATUS.FINISHED;
            shareStatus(status);
        }


    }

    /**
     * Set the ProtocolViewModel
     * @param model view model to use
     */
    public void setProtocolViewModel(ProtocolViewModel model) {
        this.model = model;
    }

    /**
     * Change the status after receiving UI input
     */
    public void receivedUI() {
        status = STATUS.READY_TO_SEND;
    }

    /**
     * Puts all the data into the ProtocolData store
     * @param data Map of name value pairs to write to ProtocolData
     */
    public void putAllInProtocolData(Map<String, String> data) {
        for (String key : data.keySet()) {
            protocolData.put(key, data.get(key));
        }
    }

    /**
     * Prepare the next message to be sent
     * @throws ProtocolMessageException
     */
    public void prepareNextMessage() throws ProtocolMessageException {
        ProtocolMessage protoMessage;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new ProtocolMessageException("Cannot create protocol message class", e);
        }
        protoMessage.prepareOutgoingMessage(protocolData);
        nextMessage = protoMessage;

    }

    /**
     * Prepare an error message to be sent
     * @param errorCode error code
     * @param message error message
     * @return string containing a websocket formatted error message to write to the web socket
     */
    public String prepareErrorMessage(int errorCode, String message) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("error-code", errorCode);
            obj.put("error-message", message);
            String errorCondition = obj.toString();
            putInProtocolData(ErrorEncryptedSubMessage.Fields.ERROR_CONDITION, errorCondition);
            ProtocolMessage errorMessage = ErrorProtocolMessage.class.newInstance();
            errorMessage.prepareOutgoingMessage(protocolData);
            return errorMessage.getWebSocketMsg(protocolData);
        } catch (JSONException | IllegalAccessException | ProtocolMessageException | InstantiationException e) {
            Log.e(TAG, "Error whilst trying to prepare an error message");
        }
        return null;
    }

    /**
     * Set the protocol into an error status
     */
    public void setErrorStatus() {
        this.status = STATUS.ERROR;
        shareStatus(this.status);
    }

    /**
     * State enum
     */
    public enum STATE {
        EMPTY {
            @Override
            public STATE next() {
                return values()[0];
            }
        };

        /**
         * We use this to automatically move back to the start
         *
         * @return
         */
        public STATE next() {
            // No bounds checking required here, because the last instance overrides
            return values()[0];
        }
    }

    public enum STATUS {
        IDLE, ERROR, READY_TO_SEND, AWAITING_UI, AWAITING_RESPONSE, FINISHED
    }
}
