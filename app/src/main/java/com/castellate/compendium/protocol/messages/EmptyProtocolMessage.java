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

package com.castellate.compendium.protocol.messages;

import com.castellate.compendium.protocol.enrol.InitKeyReqProtocolMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Empty placeholder message, used when needing to put a placeholder message in a protocol
 */
public class EmptyProtocolMessage extends ProtocolMessage {

    /**
     * Construct an empty protocol message
     */
    public EmptyProtocolMessage(){
        super();
    }

    @Override
    public String getWebSocketMsg(Map<String, String> protocolData) {
        return null;
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
        return InitKeyReqProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public boolean parse(JSONObject data) {
        this.validate(Fields.ALL_FIELDS);
        return false;
    }

    /**
     * Fields class defining fields used by this message type
     */
    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String[] ALL_FIELDS = new String[0];
    }
}
