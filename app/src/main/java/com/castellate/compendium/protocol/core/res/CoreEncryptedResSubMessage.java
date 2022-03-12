package com.castellate.compendium.protocol.core.res;

import com.castellate.compendium.protocol.messages.LoadProtocolData;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.SignMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class CoreEncryptedResSubMessage extends ProtocolMessage implements SignMessage, LoadProtocolData {

    public CoreEncryptedResSubMessage(){
        super();
    }
    private CoreEncryptedResSubMessage innerSubType;

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        if(innerSubType!=null){
            return innerSubType.getClassObj(protocolData);
        }
        switch (protocolData.get(Fields.TYPE)) {
            case Fields.TYPE_GET:
                innerSubType = new CoreGetResMessage();
                break;
            case Fields.TYPE_PUT:
                innerSubType = new CorePutResMessage();
                break;
            case Fields.TYPE_REG:
                innerSubType = new CoreRegResMessage();
                break;
            case Fields.TYPE_VERIFY:
                innerSubType = new CoreVerifyResMessage();
                break;
            default:
                break;
        }
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
                    innerSubType = new CoreGetResMessage();
                    break;
                case Fields.TYPE_PUT:
                    innerSubType = new CorePutResMessage();
                    break;
                case Fields.TYPE_REG:
                    innerSubType = new CoreRegResMessage();
                    break;
                case Fields.TYPE_VERIFY:
                    innerSubType = new CoreVerifyResMessage();
                    break;
                default:
                    return false;

            }
        }catch(JSONException e){
            return false;
        }
        return super.parse(data);
    }



    @Override
    public String getSignatureField() {
        return innerSubType.getSignatureField();
    }

    @Override
    public String[] getSignatureFields() {
        return innerSubType.getSignatureFields();

    }


    @Override
    public String[] getLoadFields() {
        return innerSubType.getLoadFields();
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
