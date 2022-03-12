package com.castellate.compendium.protocol.core.req;

import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.data.StorageException;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.protocol.messages.VerifySignature;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class CoreEncryptedReqSubMessage extends ProtocolMessage implements VerifySignature, StoreProtocolData {

    public CoreEncryptedReqSubMessage(){
        super();
    }
    private CoreEncryptedReqSubMessage innerSubType;
    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return this.innerSubType.getClassObj(protocolData);

    }

    @Override
    public String[] getAllFields() {
        return this.innerSubType.getAllFields();

    }

    @Override
    public boolean parse(JSONObject data) {
        msgData=data;
        try {
            switch (msgData.getString(Fields.TYPE)) {
                case Fields.TYPE_GET:
                    innerSubType = new CoreGetReqMessage();
                    break;
                case Fields.TYPE_PUT:
                    innerSubType = new CorePutReqMessage();
                    break;
                case Fields.TYPE_REG:
                    innerSubType = new CoreRegReqMessage();
                    break;
                case Fields.TYPE_VERIFY:
                    innerSubType = new CoreVerifyReqMessage();
                    break;
                default:
                    return false;

            }
        }catch(JSONException e){
            return false;
        }
        innerSubType.parse(msgData);
        return super.parse(data);
    }

    @Override
    public String getSignature() {
        return innerSubType.getSignature();
    }

    @Override
    public String[] getSignatureFields() {
        return innerSubType.getSignatureFields();

    }

    @Override
    public String getPublicKey(Map<String,String>protocolData) {
        try {
            return IdentityStore.getInstance().getPublicIdentityById(protocolData.get(Constants.HASH_PC_PUBLIC_KEY));
        } catch (StorageException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] getStoreFields() {
        return innerSubType.getStoreFields();
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }



        public static final String TYPE = "type";
        public static final String TYPE_PUT = "Put";
        public static final String TYPE_GET = "Get";
        public static final String TYPE_REG = "Reg";
        public static final String TYPE_VERIFY = "Verify";



    }
}
