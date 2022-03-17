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

package com.castellate.compendium.protocol.core.req;

import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.VerifySignature;

import org.json.JSONObject;

import java.util.Map;

public class CorePutReqMessage extends CoreEncryptedReqSubMessage implements VerifySignature {

    public CorePutReqMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CorePutReqMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public boolean parse(JSONObject data) {
        msgData=data;
        return super.validate(Fields.ALL_FIELDS);

    }

    @Override
    public String getSignature() {
        return get(Fields.SIGNATURE_MSG);
    }

    @Override
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }

    @Override
    public String getPublicKey(Map<String,String>protocolData) {
        return protocolData.get(Constants.PC_PUBLIC_KEY);

    }
    @Override
    public String[] getStoreFields() {
        return Fields.STORE_FIELDS;
    }
    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }




        public static final String TYPE = "type";
        public static final String ID_CD = "id_cd";
        public static final String APP_ID ="app_id";
        public static final String DESC = "desc";
        public static final String CODE = "code";
        public static final String DATA = "data";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,ID_CD,APP_ID,DESC,CODE,DATA,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,ID_CD,APP_ID,DESC,CODE,DATA};
        public static final String[] STORE_FIELDS = new String[]{TYPE,APP_ID,DESC,CODE,DATA};


    }
}
