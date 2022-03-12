package com.castellate.compendium.protocol.core;

import static com.castellate.compendium.protocol.messages.Constants.ADR_CD;
import static com.castellate.compendium.protocol.messages.Constants.HASH_CD_PUBLIC_KEY;

import com.castellate.compendium.protocol.messages.InitKeyRespProtocolMessage;
import com.castellate.compendium.protocol.messages.LoadProtocolData;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.SignMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class CoreKeyEncryptedRespMessage extends ProtocolMessage implements LoadProtocolData, SignMessage {

    public CoreKeyEncryptedRespMessage(){
        super();
    }
    @Override
    public boolean parse(String data)  {
        try {
            return parse(new JSONObject((data)));
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreKeyEncryptedRespMessage.class;
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
    public String getSignatureField() {
        return Fields.SIGNATURE_CD;
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }
        public static final String SIGNATURE_CD = "signature_cd";
        public static final String[] ALL_FIELDS = new String[]{ADR_CD,HASH_CD_PUBLIC_KEY,SIGNATURE_CD};
        public static final String[] SIG_FIELDS = new String[]{InitKeyRespProtocolMessage.Fields.G_Y, CoreKeyReqProtocolMessage.Fields.G_X,ADR_CD};
        //public static final String[] STORE_FIELDS = SIG_FIELDS;
        public static final String[] LOAD_FIELDS = new String[]{ADR_CD,HASH_CD_PUBLIC_KEY};
    }
}
