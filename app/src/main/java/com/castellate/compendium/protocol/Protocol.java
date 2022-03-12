package com.castellate.compendium.protocol;

import android.util.Log;

import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Protocol {
    private static final String TAG = "Protocol";
    public enum STATE {
        EMPTY{
            @Override
            public STATE next() {
                return values()[0];
            };
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
    protected Map<String,String> protocolData = new HashMap<String,String>();
    protected ProtocolMessage nextMessage =null;
    protected ProtocolViewModel model;
    public abstract Class[] getMessages();
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
        ProtocolMessage protoMessage = null;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();
            if(protoMessage == null){
                status = STATUS.AWAITING_RESPONSE;
                return shareStatus(status);

            }
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
        Iterator<String> itr = data.keySet().iterator();
        while(itr.hasNext()){
            String key = itr.next();
            protocolData.put(key,data.get(key));
        }
    }
    public void prepareNextMessage() throws ProtocolMessageException {
        ProtocolMessage protoMessage = null;
        try {
            protoMessage = (ProtocolMessage) getMessages()[getStateOrdinal()].newInstance();
        } catch (IllegalAccessException  | InstantiationException e) {
            throw new ProtocolMessageException("Cannot create protocol message class",e);
        }
        protoMessage.prepareOutgoingMessage(protocolData);
        nextMessage = protoMessage;

    }
    public void setErrorStatus(){
        this.status = STATUS.ERROR;
        shareStatus(this.status);
    }
}
