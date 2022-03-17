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

public abstract class Protocol {
    private static final String TAG = "Protocol";
    public enum STATE {
        EMPTY{
            @Override
            public STATE next() {
                return values()[0];
            }
        };
        public STATE next() {
            // No bounds checking required here, because the last instance overrides
            return values()[0];
        }
    }
    public enum STATUS {
        IDLE,
        ERROR,
        READY_TO_SEND,
        AWAITING_UI,
        AWAITING_RESPONSE,
        FINISHED
    }
    protected final Map<String,String> protocolData = new HashMap<>();
    protected ProtocolMessage nextMessage =null;
    protected ProtocolViewModel model;
    public abstract Class<? extends ProtocolMessage>[] getMessages();
    public abstract int getStateOrdinal();
    public abstract boolean isFinished();

    public abstract String getProtocolStateString();
    public abstract boolean processIncomingMessage(ProtocolMessage protoMessage) throws ProtocolMessageException;
    protected STATUS status = STATUS.IDLE;
    public STATUS getStatus(){
        return status;
    }
    public STATUS shareStatus(STATUS status){
        if(model!=null){
            model.postProtocolStatus(status);
        }
        return status;
    }
    public STATUS parseIncomingMessage(JSONObject msg) {
        ProtocolMessage protoMessage;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();

        } catch (IllegalAccessException | InstantiationException e) {
            status = STATUS.ERROR;
            return shareStatus(status);
        }
        if(!protoMessage.parse(msg)){
            status = STATUS.AWAITING_RESPONSE;
            return shareStatus(status);
        }
        try {
            if (!processIncomingMessage(protoMessage) || !protoMessage.processSubMessages(protocolData)) {

                status = STATUS.AWAITING_RESPONSE;
                return shareStatus(status);
            }
        }catch(ProtocolMessageException e){
            Log.e(TAG,"Exception processing incoming message", e);
            status = STATUS.ERROR;
            return shareStatus(status);
        }
        if(advancedStateTriggerUI()){
            if(isFinished()){
                status = STATUS.FINISHED;
                return shareStatus(status);
            }
            status = STATUS.AWAITING_UI;
            return shareStatus(status);
        }else if(getStateOrdinal()==0){
            status=STATUS.FINISHED;
            return shareStatus(status);
        }else {

            try {
                prepareNextMessage();
            } catch (ProtocolMessageException e) {
                Log.e(TAG,"Exception creating next message", e);
                status = STATUS.ERROR;
                return shareStatus(status);
            }
        }
        status = STATUS.READY_TO_SEND;
        return shareStatus(status);
    }
    public void putInProtocolData(String field, String value){
        this.protocolData.put(field,value);
    }
    public abstract boolean advancedStateTriggerUI();
    public STATUS parseIncomingMessage(String msg) {
        try {
            return parseIncomingMessage(new JSONObject(msg));
        }catch(JSONException e) {
            status = STATUS.AWAITING_RESPONSE;
            return shareStatus(status);
        }

    }
    public void cleanUp(){
        this.protocolData.clear();
        if(this.model!=null){
            this.model.postProtocolStatus(STATUS.IDLE);
        }
        this.model=null;

    }

    public String getProtocolData(String field){
        return protocolData.get(field);
    }
    public String getNextMessage(){
        return nextMessage.getWebSocketMsg(protocolData);
    }
    public abstract int getTotalStates();
    public int getProgress(){
        if(getStatus() == STATUS.FINISHED){
            return 100;
        }else {
            double increment = ((double)100)/getTotalStates();
            return (int) Math.round(increment * getStateOrdinal());
        }
    }
    public void messageSent(){
        nextMessage = null;
        //We should never trigger UI on sending
        advancedStateTriggerUI();
        if(isFinished()){
            status = STATUS.FINISHED;
            shareStatus(status);
        }
        //We can only advanced back to zero once finished
        if(getStateOrdinal()==0){
            status=STATUS.FINISHED;
            shareStatus(status);
        }


    }
    public void setProtocolViewModel(ProtocolViewModel model){
        this.model = model;
    }
    public void receivedUI(){
        status = STATUS.READY_TO_SEND;
    }



    public void putAllInProtocolData(Map<String,String> data){
        for (String key : data.keySet()) {
            protocolData.put(key, data.get(key));
        }
    }
    public void prepareNextMessage() throws ProtocolMessageException {
        ProtocolMessage protoMessage;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();
        } catch (IllegalAccessException  | InstantiationException e) {
            throw new ProtocolMessageException("Cannot create protocol message class",e);
        }
        protoMessage.prepareOutgoingMessage(protocolData);
        nextMessage = protoMessage;

    }
    public String prepareErrorMessage(int errorCode, String message){

        try {
            JSONObject obj = new JSONObject();
            obj.put("error-code", errorCode);
            obj.put("error-message", message);
            String errorCondition = obj.toString();
            putInProtocolData(ErrorEncryptedSubMessage.Fields.ERROR_CONDITION, errorCondition);
            ProtocolMessage errorMessage = ErrorProtocolMessage.class.newInstance();
            errorMessage.prepareOutgoingMessage(protocolData);
            return errorMessage.getWebSocketMsg(protocolData);
        }catch(JSONException | IllegalAccessException |ProtocolMessageException| InstantiationException e){
            Log.e(TAG,"Error whilst trying to prepare an error message");
        }
        return null;
    }
    public void setErrorStatus(){
        this.status = STATUS.ERROR;
        shareStatus(this.status);
    }
}
