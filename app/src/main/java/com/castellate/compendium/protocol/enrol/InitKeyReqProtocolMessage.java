package com.castellate.compendium.protocol.enrol;

import static com.castellate.compendium.protocol.messages.Constants.ADR_PC;
import static com.castellate.compendium.protocol.messages.Constants.CD_PUBLIC_KEY;
import static com.castellate.compendium.protocol.messages.Constants.ID_PC;
import static com.castellate.compendium.protocol.messages.Constants.PC_PUBLIC_KEY;

import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.protocol.messages.VerifySignature;

import java.security.KeyPair;
import java.util.Map;

public class InitKeyReqProtocolMessage extends ProtocolMessage implements StoreProtocolData, VerifySignature {

    public InitKeyReqProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return InitKeyReqProtocolMessage.class;
    }



    @Override
    public boolean processMessage(Map<String, String> protocolData) {
        if(!super.processMessage(protocolData)){
            return false;
        }
        CompanionKeyManager ckm = new CompanionKeyManager();
        KeyPair kp = ckm.getOrCreateIdentityKey();
        protocolData.put(CD_PUBLIC_KEY,CryptoUtils.encodePublicKey(kp.getPublic()));
        return true;
    }
    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }


    @Override
    public String[] getStoreFields() {
        return Fields.STORE_FIELDS;
    }

    @Override
    public String getSignature() {
        return get(Fields.SIGNATURE_PC);
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    @Override
    public String getPublicKey(Map<String,String> protocolData) {
        return get(PC_PUBLIC_KEY);
    }


    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String G_X = "g_to_x";
        public static final String SIGNATURE_PC = "signature_pc";
        public static final String[] ALL_FIELDS = new String[]{ADR_PC,PC_PUBLIC_KEY,ID_PC,G_X,SIGNATURE_PC};
        public static final String[] SIG_FIELDS = new String[]{ADR_PC,PC_PUBLIC_KEY,ID_PC,G_X};
        public static final String[] STORE_FIELDS = SIG_FIELDS;
    }
}
