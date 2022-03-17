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

import android.util.Log;

import com.castellate.compendium.crypto.B64;

import org.json.JSONException;
import org.json.JSONObject;

public class EncryptedMessage {
    public static final String IV = "iv";
    public static final String CIPHER_TEXT = "cipher_text";
    private final JSONObject data;

    public EncryptedMessage(JSONObject msgObj) throws ProtocolMessageException {
        Log.d("EncryptedJSONMessage", msgObj.toString());
        data = msgObj;
        if (!data.has(IV) || !data.has(CIPHER_TEXT)) {
            throw new ProtocolMessageException("Missing fields in cipher");
        }

    }

    public EncryptedMessage(String msg) throws ProtocolMessageException {
        try {
            Log.d("EncryptedMessage", msg);
            data = new JSONObject(msg);
            if (!data.has(IV) || !data.has(CIPHER_TEXT)) {
                throw new ProtocolMessageException("Missing fields in cipher");
            }
        } catch (JSONException e) {
            throw new ProtocolMessageException("Exception processing cipher", e);
        }
    }

    public EncryptedMessage(byte[] cipherText, byte[] iv) throws ProtocolMessageException {
        try {
            data = new JSONObject();
            data.put(IV, B64.encode(iv));
            data.put(CIPHER_TEXT, B64.encode(cipherText));
        } catch (JSONException e) {
            throw new ProtocolMessageException("Exception processing cipher", e);
        }
    }

    public JSONObject getDataObj() {
        return this.data;
    }

    public String getString() {
        return this.data.toString();
    }

    public byte[] getIV() {
        return B64.decode(data.optString(IV));
    }

    public byte[] getCipher() {
        return B64.decode(data.optString(CIPHER_TEXT));
    }
}
