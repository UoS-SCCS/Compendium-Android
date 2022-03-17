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

import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;
import com.castellate.compendium.protocol.messages.StoreProtocolData;
import com.castellate.compendium.protocol.messages.VerifySignature;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class CoreEncryptedReqSubMessage extends ProtocolMessage implements VerifySignature, StoreProtocolData {

    public CoreEncryptedReqSubMessage(){
        super();
    }
    private CoreEncryptedReqSubMessage innerSubType;
    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
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
                    innerSubType = new CoreGetReqMessage();
                    break;
                case Fields.TYPE_PUT:
                    innerSubType = new CorePutReqMessage();
                    break;
                case Fields.TYPE_REG:
                    innerSubType = new CoreRegReqMessage();
                    break;
                case Fields.TYPE_VERIFY:
                    innerSubType = new CoreVerifyReqMessage();
                    break;
                default:
                    return false;

            }
        }catch(JSONException e){
            return false;
        }
        innerSubType.parse(msgData);
        return super.parse(data);
    }

    @Override
    public String getSignature() {
        return innerSubType.getSignature();
    }

    @Override
    public String[] getSignatureFields() {
        return innerSubType.getSignatureFields();

    }

    @Override
    public String getPublicKey(Map<String,String>protocolData) throws ProtocolMessageException {
        try {
            return IdentityStore.getInstance().getPublicIdentityById(protocolData.get(Constants.HASH_PC_PUBLIC_KEY));
        } catch (StorageException e) {
            throw new ProtocolMessageException("Exception getting PC Public Key ID",e);
        }
    }

    @Override
    public String[] getStoreFields() {
        return innerSubType.getStoreFields();
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
