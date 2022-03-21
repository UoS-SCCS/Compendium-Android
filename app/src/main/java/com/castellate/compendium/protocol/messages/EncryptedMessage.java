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

/**
 * Encrypted Message class this is a general class that defines any encrypted blob, consisting of
 * an IV and the cipher text.
 */
public class EncryptedMessage {
    public static final String IV = "iv";
    public static final String CIPHER_TEXT = "cipher_text";
    private final JSONObject data;

    /**
     * Create a new EncryptedMessage from the JSONObject
     * @param msgObj JSONObject containing IV and cipher text
     * @throws ProtocolMessageException
     */
    public EncryptedMessage(JSONObject msgObj) throws ProtocolMessageException {
        Log.d("EncryptedJSONMessage", msgObj.toString());
        data = msgObj;
        if (!data.has(IV) || !data.has(CIPHER_TEXT)) {
            throw new ProtocolMessageException("Missing fields in cipher");
        }

    }

    /**
     * Create a new EncryptedMessage from a String that will first be parsed as a JSONObject, which
     * should contain an IV and cipher text
     * @param msg JSON String containing IV and cipher text
     * @throws ProtocolMessageException
     */
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

    /**
     * Create a new EncryptedMessage from a cipher text and the IV. Will convert the values
     * to Base64 and place into a JSONObject
     * @param cipherText bytes containing cipher text
     * @param iv bytes containing IV
     * @throws ProtocolMessageException
     */
    public EncryptedMessage(byte[] cipherText, byte[] iv) throws ProtocolMessageException {
        try {
            data = new JSONObject();
            data.put(IV, B64.encode(iv));
            data.put(CIPHER_TEXT, B64.encode(cipherText));
        } catch (JSONException e) {
            throw new ProtocolMessageException("Exception processing cipher", e);
        }
    }

    /**
     * Get the underlying JSONObject containing the IV and cipher text
     * @return JSONObject with IV and ciphertext
     */
    public JSONObject getDataObj() {
        return this.data;
    }

    /**
     * Get the underlying JSONObject as a string
     * @return string representation of data object
     */
    public String getString() {
        return this.data.toString();
    }

    /**
     * Get the IV bytes from the Base64 encoded field
     * @return bytes containing the IV
     */
    public byte[] getIV() {
        return B64.decode(data.optString(IV));
    }

    /**
     * Get the cipher text bytes from the Base64 encoded field
     * @return bytes containing cipher text
     */
    public byte[] getCipher() {
        return B64.decode(data.optString(CIPHER_TEXT));
    }
}
