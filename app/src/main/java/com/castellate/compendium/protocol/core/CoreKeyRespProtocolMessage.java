/*
 *  © Copyright 2022. University of Surrey
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.castellate.compendium.protocol.core;

import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.protocol.enrol.InitKeyReqProtocolMessage;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.EmbeddedEncryptedMessage;
import com.castellate.compendium.protocol.messages.InitKeyRespProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

import java.security.KeyPair;
import java.util.Map;

public class CoreKeyRespProtocolMessage extends InitKeyRespProtocolMessage implements EmbeddedEncryptedMessage {

    public CoreKeyRespProtocolMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreKeyRespProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public Class<?> getEncryptedMessageClass() {
        return CoreKeyEncryptedRespMessage.class;
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
