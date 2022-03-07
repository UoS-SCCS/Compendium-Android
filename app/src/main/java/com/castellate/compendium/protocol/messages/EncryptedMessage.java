package com.castellate.compendium.protocol.messages;

import android.util.Log;

import com.castellate.compendium.crypto.B64;

import org.json.JSONException;
import org.json.JSONObject;

public class EncryptedMessage {
    private static final String IV = "iv";
    private static final String CIPHER_TEXT = "cipher_text";
    private JSONObject data;

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
