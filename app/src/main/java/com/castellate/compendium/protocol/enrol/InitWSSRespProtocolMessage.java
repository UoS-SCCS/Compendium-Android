package com.castellate.compendium.protocol.enrol;

import static com.castellate.compendium.protocol.messages.Constants.ADR_CD;

import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.ws.WSMessages;

import java.util.Map;

public class InitWSSRespProtocolMessage extends ProtocolMessage implements StoreProtocolData {

    public InitWSSRespProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj() {
        return InitWSSRespProtocolMessage.class;
    }

    @Override
    public String getWebSocketMsg(Map<String, String> protocolData) {
        return WSMessages.createInitMsg().toString();
    }
    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }



    @Override
    public String[] getStoreFields() {
        return Fields.STORE_FIELDS;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }
        public static final String EPHE_WSS_ADDR = "EpheWssAddr";
        public static final String TYPE = "type";
        public static final String[] ALL_FIELDS = new String[]{EPHE_WSS_ADDR,TYPE};
        public static final String[] STORE_FIELDS = new String[]{EPHE_WSS_ADDR+":"+ADR_CD};
    }
}
