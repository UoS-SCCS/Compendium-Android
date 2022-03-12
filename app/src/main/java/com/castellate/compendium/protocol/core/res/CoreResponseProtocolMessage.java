package com.castellate.compendium.protocol.core.res;

import com.castellate.compendium.protocol.messages.EmbeddedEncryptedMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessage;

import java.util.Map;

public class CoreResponseProtocolMessage extends ProtocolMessage implements EmbeddedEncryptedMessage {

    public CoreResponseProtocolMessage(){
        super();
    }


    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreResponseProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }


    @Override
    public String getEncryptedMsgField() {
        return Fields.ENC_MSG;
    }

    @Override
    public Class<?> getEncryptedMessageClass() {
        return CoreEncryptedResSubMessage.class;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String ENC_MSG = "enc_msg";
        public static final String[] ALL_FIELDS = new String[]{ENC_MSG};
    }
}