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

import static com.castellate.compendium.protocol.messages.Constants.ADR_PC;
import static com.castellate.compendium.protocol.messages.Constants.CD_PUBLIC_KEY;
import static com.castellate.compendium.protocol.messages.Constants.HASH_PC_PUBLIC_KEY;

import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.protocol.messages.VerifySignature;

import java.security.KeyPair;
import java.util.Map;

public class CoreKeyReqProtocolMessage extends ProtocolMessage implements StoreProtocolData, VerifySignature {

    public CoreKeyReqProtocolMessage() {
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String, String> protocolData) {
        return CoreKeyReqProtocolMessage.class;
    }


    @Override
    public boolean processMessage(Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            if (!super.processMessage(protocolData)) {
                return false;
            }
            CompanionKeyManager ckm = new CompanionKeyManager();
            KeyPair kp = ckm.getOrCreateIdentityKey();
            protocolData.put(CD_PUBLIC_KEY, CryptoUtils.encodePublicKey(kp.getPublic()));

            protocolData.put(Constants.HASH_CD_PUBLIC_KEY, CryptoUtils.getPublicKeyId(kp.getPublic()));
        } catch (CryptoException e) {
            throw new ProtocolMessageException("Exception getting CD public key ID", e);
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
    public String getPublicKey(Map<String, String> protocolData) throws ProtocolMessageException {
        IdentityStore identityStore = IdentityStore.getInstance();
        try {
            return identityStore.getPublicIdentityById(get(HASH_PC_PUBLIC_KEY));
        } catch (StorageException e) {
           throw new ProtocolMessageException("Exception getting PC Public Key ID",e);
        }

    }


    public static final class Fields {

        public static final String G_X = "g_to_x";
        public static final String SIGNATURE_PC = "signature_pc";
        public static final String[] ALL_FIELDS = new String[]{ADR_PC, HASH_PC_PUBLIC_KEY, G_X, SIGNATURE_PC};
        public static final String[] SIG_FIELDS = new String[]{ADR_PC, HASH_PC_PUBLIC_KEY, G_X};
        public static final String[] STORE_FIELDS = SIG_FIELDS;
        private Fields() {
            // restrict instantiation
        }
    }
}
