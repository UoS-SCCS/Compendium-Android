package com.castellate.compendium.protocol.messages;

import org.json.JSONObject;

import java.util.Map;

public abstract class  InitKeyRespProtocolMessage extends ProtocolMessage implements EmbeddedEncryptedMessage,LoadProtocolData {

    public InitKeyRespProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return InitKeyRespProtocolMessage.class;
    }

    @Override
    public boolean parse(JSONObject data) {
        return this.validate(Fields.ALL_FIELDS);
    }

    @Override
    public String getEncryptedMsgField() {
        return Fields.ENC_MSG;
    }

    @Override
    public Class<?> getEncryptedMessageClass() {
        return ProtocolMessage.class;
    }
    @Override
    public String[] getLoadFields(){
        return Fields.LOAD_FIELDS;
    }
    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String G_Y = "g_to_y";
        public static final String ENC_MSG = "enc_msg";
        public static final String[] ALL_FIELDS = new String[]{G_Y,ENC_MSG};
        public static final String[] LOAD_FIELDS = new String[]{G_Y};
    }
}
