package com.castellate.compendium.protocol.core;

import com.castellate.compendium.protocol.messages.EmbeddedEncryptedMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessage;

import java.util.Map;

public class ConfirmKeyProtocolMessage extends ProtocolMessage implements EmbeddedEncryptedMessage {

    public ConfirmKeyProtocolMessage(){
        super();
    }


    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return ConfirmKeyProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }


    @Override
    public String getEncryptedMsgField() {
        return Fields.ENC_SIG;
    }

    @Override
    public Class<?> getEncryptedMessageClass() {
        return ConfirmKeyEncryptedSubMessage.class;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String ENC_SIG = "enc_sig_confirm";
        public static final String[] ALL_FIELDS = new String[]{ENC_SIG};
    }
}
