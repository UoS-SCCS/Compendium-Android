package com.castellate.compendium.protocol.core.res;

import java.util.Map;

public class CoreGetResMessage extends CoreEncryptedResSubMessage {

    public CoreGetResMessage(){
        super();
    }

    @Override
    public String[] getLoadFields() {
        return Fields.LOAD_FIELDS;
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreGetResMessage.class;
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
        return Fields.SIGNATURE_MSG;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }


        public static final String TYPE = "type";
        public static final String DATA = "data";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,DATA,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,DATA};
        public static final String[] LOAD_FIELDS = new String[]{TYPE,DATA};


    }
}
