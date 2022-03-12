package com.castellate.compendium.protocol.enrol;

import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.InitKeyRespProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.VerifySignature;

import java.util.Map;

public class ConfirmKeyEncryptedSubMessage extends ProtocolMessage implements VerifySignature {

    public ConfirmKeyEncryptedSubMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return ConfirmKeyEncryptedSubMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }



    @Override
    public String getSignature() {
        return get(Fields.SIGNATURE_CONFIRM);
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    @Override
    public String getPublicKey(Map<String,String>protocolData) {
        return protocolData.get(Constants.PC_PUBLIC_KEY);

    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }




        public static final String SIGNATURE_CONFIRM = "signature";
        public static final String[] ALL_FIELDS = new String[]{SIGNATURE_CONFIRM};
        public static final String[] SIG_FIELDS = new String[]{InitKeyReqProtocolMessage.Fields.G_X,InitKeyRespProtocolMessage.Fields.G_Y};


    }
}
