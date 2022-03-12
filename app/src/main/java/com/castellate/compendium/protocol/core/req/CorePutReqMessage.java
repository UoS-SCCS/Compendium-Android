package com.castellate.compendium.protocol.core.req;

import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.VerifySignature;

import org.json.JSONObject;

import java.util.Map;

public class CorePutReqMessage extends CoreEncryptedReqSubMessage implements VerifySignature {

    public CorePutReqMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CorePutReqMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public boolean parse(JSONObject data) {
        msgData=data;
        return super.validate(Fields.ALL_FIELDS);

    }

    @Override
    public String getSignature() {
        return get(Fields.SIGNATURE_MSG);
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    @Override
    public String getPublicKey(Map<String,String>protocolData) {
        return protocolData.get(Constants.PC_PUBLIC_KEY);

    }
    @Override
    public String[] getStoreFields() {
        return Fields.STORE_FIELDS;
    }
    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }




        public static final String TYPE = "type";
        public static final String ID_CD = "id_cd";
        public static final String APP_ID ="app_id";
        public static final String DESC = "desc";
        public static final String CODE = "code";
        public static final String DATA = "data";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,ID_CD,APP_ID,DESC,CODE,DATA,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,ID_CD,APP_ID,DESC,CODE,DATA};
        public static final String[] STORE_FIELDS = new String[]{TYPE,APP_ID,DESC,CODE,DATA};


    }
}
