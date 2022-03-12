package com.castellate.compendium.protocol.enrol;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

public class EnrolProtocol extends Protocol {
    public enum STATE {
        INIT_KEY_REQ,
        INIT_WSS,
        INIT_WSS_RESP,
        INIT_KEY_RESP,
        KEY_CONFIRM_REQ,
        FINISHED {
            @Override
            public STATE next() {
                return values()[0];
            }
        };
        public STATE next() {
            // No bounds checking required here, because the last instance overrides
            return values()[ordinal() + 1];
        }
    }
    private STATE state = STATE.INIT_KEY_REQ;
    private static final Class<? extends ProtocolMessage>[] MESSAGES = new Class[]{InitKeyReqProtocolMessage.class,InitWSSProtocolMessage.class,InitWSSRespProtocolMessage.class,EnrolKeyRespProtocolMessage.class,ConfirmKeyProtocolMessage.class};
    public EnrolProtocol(){
        super();
    }
    @Override
    public Class<? extends ProtocolMessage>[] getMessages() {
        return EnrolProtocol.MESSAGES;
    }
    @Override
    public int getStateOrdinal() {
        return state.ordinal();
    }

    @Override
    public String getProtocolStateString() {
        return state.name();
    }


    @Override
    public boolean processIncomingMessage(ProtocolMessage protoMessage) throws ProtocolMessageException {
        return protoMessage.processMessage(this.protocolData);
    }

    @Override
    public boolean isFinished() {
        return state == STATE.FINISHED;
    }



    @Override
    public boolean advancedStateTriggerUI() {
        state=state.next();
        if(model!=null){
            model.postProtocolState(state.name());
        }
        return state == STATE.INIT_WSS || state == STATE.FINISHED;
    }

    @Override
    public int getTotalStates() {
        //We don't count the finished state
        return STATE.values().length-1;
    }


}
