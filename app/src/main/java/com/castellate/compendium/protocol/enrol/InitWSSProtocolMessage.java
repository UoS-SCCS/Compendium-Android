package com.castellate.compendium.protocol.enrol;

import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.ws.WSMessages;

import java.util.Map;

public class InitWSSProtocolMessage extends ProtocolMessage {

    public InitWSSProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return InitWSSProtocolMessage.class;
    }

    @Override
    public String getWebSocketMsg(Map<String, String> protocolData) {
        return WSMessages.createInitMsg().toString();
    }
    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String TYPE = "type";
        public static final String[] ALL_FIELDS = new String[]{TYPE};

    }
}
