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

package com.castellate.compendium.protocol.core.res;

import java.util.Map;

public class CoreVerifyResMessage extends CoreEncryptedResSubMessage {

    public CoreVerifyResMessage(){
        super();
    }

    @Override
    public Class<?> getClassObj(Map<String,String> protocolData) {
        return CoreVerifyResMessage.class;
    }
    @Override
    public String getSignatureField() {
        return Fields.SIGNATURE_MSG;
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
    public String[] getSignatureFields() {
        return Fields.SIG_FIELDS;
    }


    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }



        public static final String TYPE = "type";

        public static final String APP_ID ="app_id";
        public static final String APP_SIG = "app_sig";
        public static final String SIGNATURE_MSG = "signature";

        public static final String[] ALL_FIELDS = new String[]{TYPE,APP_ID,APP_SIG,SIGNATURE_MSG};
        public static final String[] SIG_FIELDS = new String[]{TYPE,APP_ID,APP_SIG};
        public static final String[] LOAD_FIELDS = new String[]{TYPE,APP_ID,APP_SIG};

    }


}
