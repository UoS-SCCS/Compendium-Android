package com.castellate.compendium.protocol.enrol;

import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.EmbeddedEncryptedMessage;
import com.castellate.compendium.protocol.messages.InitKeyRespProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

import java.security.KeyPair;
import java.util.Map;

public class EnrolKeyRespProtocolMessage extends InitKeyRespProtocolMessage implements EmbeddedEncryptedMessage {

    public EnrolKeyRespProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return EnrolKeyRespProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public Class<?> getEncryptedMessageClass() {
        return InitKeyEncryptedRespMessage.class;
    }

    @Override
    public boolean prepareOutgoingMessage(Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            KeyPair kp = CryptoUtils.generateEphemeralKeys();
            byte[] sharedSecret = CryptoUtils.performECDH(kp, CryptoUtils.getPublicKey(protocolData.get(InitKeyReqProtocolMessage.Fields.G_X)));
            protocolData.put(Fields.G_Y, CryptoUtils.encodePublicKey(kp.getPublic()));
            protocolData.put(Constants.DERIVED_KEY,CryptoUtils.deriveKey(sharedSecret));
        }catch(CryptoException e){
            throw new ProtocolMessageException("Exception processing key exchange",e);
        }
        if(!super.prepareOutgoingMessage(protocolData)){
            return false;
        }
        return true;
    }


}
