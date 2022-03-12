package com.castellate.compendium.protocol.core.res;

import java.util.Map;

public class CoreRegResMessage extends CoreEncryptedResSubMessage {

    public CoreRegResMessage(){
        super();
    }
    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreRegResMessage.class;
    }
    @Override
    public String[] getLoadFields() {
        return Fields.LOAD_FIELDS;
    }
    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }



    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    @Override
    public String getSignatureField() {
        return CoreGetResMessage.Fields.SIGNATURE_MSG;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String TYPE = "type";
        public static final String APP_PK = "app_pk";
        public static final String APP_ID ="app_id";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,APP_ID,APP_PK,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,APP_ID,APP_PK};
        public static final String[] LOAD_FIELDS = new String[]{TYPE,APP_ID,APP_PK};


    }
}
