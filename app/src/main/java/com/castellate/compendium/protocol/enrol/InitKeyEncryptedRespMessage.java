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

import static com.castellate.compendium.protocol.messages.Constants.ADR_CD;
import static com.castellate.compendium.protocol.messages.Constants.CD_PUBLIC_KEY;
import static com.castellate.compendium.protocol.messages.Constants.ID_CD;

import com.castellate.compendium.protocol.messages.InitKeyRespProtocolMessage;
import com.castellate.compendium.protocol.messages.LoadProtocolData;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.SignMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class InitKeyEncryptedRespMessage extends ProtocolMessage implements LoadProtocolData, SignMessage {

    public InitKeyEncryptedRespMessage(){
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
        return InitKeyEncryptedRespMessage.class;
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
        public static final String[] ALL_FIELDS = new String[]{ADR_CD,ID_CD,CD_PUBLIC_KEY,SIGNATURE_CD};
        public static final String[] SIG_FIELDS = new String[]{InitKeyRespProtocolMessage.Fields.G_Y,InitKeyReqProtocolMessage.Fields.G_X,ADR_CD,ID_CD,CD_PUBLIC_KEY};
        //public static final String[] STORE_FIELDS = SIG_FIELDS;
        public static final String[] LOAD_FIELDS = new String[]{ADR_CD,ID_CD,CD_PUBLIC_KEY};
    }
}
