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
