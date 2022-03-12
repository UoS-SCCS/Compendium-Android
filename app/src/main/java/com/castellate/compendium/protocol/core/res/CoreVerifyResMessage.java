package com.castellate.compendium.protocol.core.res;

import java.util.Map;

public class CoreVerifyResMessage extends CoreEncryptedResSubMessage {

    public CoreVerifyResMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreVerifyResMessage.class;
    }
    @Override
    public String getSignatureField() {
        return CoreGetResMessage.Fields.SIGNATURE_MSG;
    }
    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }
    @Override
    public String[] getLoadFields() {
        return Fields.LOAD_FIELDS;
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

        public static final String APP_ID ="app_id";
        public static final String APP_SIG = "app_sig";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,APP_ID,APP_SIG,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,APP_ID,APP_SIG};
        public static final String[] LOAD_FIELDS = new String[]{TYPE,APP_ID,APP_SIG};

    }


}
