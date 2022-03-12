package com.castellate.compendium.protocol.core;

import static com.castellate.compendium.protocol.messages.Constants.ADR_PC;
import static com.castellate.compendium.protocol.messages.Constants.CD_PUBLIC_KEY;
import static com.castellate.compendium.protocol.messages.Constants.HASH_PC_PUBLIC_KEY;

import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.data.StorageException;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.protocol.messages.VerifySignature;

import java.security.KeyPair;
import java.util.Map;

public class CoreKeyReqProtocolMessage extends ProtocolMessage implements StoreProtocolData, VerifySignature {

    public CoreKeyReqProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreKeyReqProtocolMessage.class;
    }



    @Override
    public boolean processMessage(Map<String, String> protocolData) {
        if(!super.processMessage(protocolData)){
            return false;
        }
        CompanionKeyManager ckm = new CompanionKeyManager();
        KeyPair kp = ckm.getOrCreateIdentityKey();
        protocolData.put(CD_PUBLIC_KEY,CryptoUtils.encodePublicKey(kp.getPublic()));
        try {
            protocolData.put(Constants.HASH_CD_PUBLIC_KEY,CryptoUtils.getPublicKeyId(kp.getPublic()));
        } catch (CryptoException e) {
            e.printStackTrace();
        }
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
        IdentityStore identityStore = IdentityStore.getInstance();
        try {
            return identityStore.getPublicIdentityById(get(HASH_PC_PUBLIC_KEY));
        } catch (StorageException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String G_X = "g_to_x";
        public static final String SIGNATURE_PC = "signature_pc";

        public static final String[] ALL_FIELDS = new String[]{ADR_PC,HASH_PC_PUBLIC_KEY,G_X,SIGNATURE_PC};
        public static final String[] SIG_FIELDS = new String[]{ADR_PC,HASH_PC_PUBLIC_KEY,G_X};
        public static final String[] STORE_FIELDS = SIG_FIELDS;
    }
}
