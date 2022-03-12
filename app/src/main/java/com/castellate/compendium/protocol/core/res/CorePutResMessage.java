package com.castellate.compendium.protocol.core.res;

import java.util.Map;

public class CorePutResMessage extends CoreEncryptedResSubMessage {

    public CorePutResMessage(){
        super();
    }
    @Override
    public String[] getLoadFields() {
        return Fields.LOAD_FIELDS;
    }
    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CorePutResMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }


    @Override
    public String getSignatureField() {
        return CoreGetResMessage.Fields.SIGNATURE_MSG;
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }



    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }


        public static final String TYPE = "type";
        public static final String ENC_DATA = "encdata";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,ENC_DATA,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,ENC_DATA};
        public static final String[] LOAD_FIELDS = new String[]{TYPE,ENC_DATA};


    }
}
