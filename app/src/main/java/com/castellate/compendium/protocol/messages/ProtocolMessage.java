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

import static com.castellate.compendium.protocol.messages.Constants.ADR_PC;
import static com.castellate.compendium.protocol.messages.Constants.DERIVED_KEY;

import android.util.Log;

import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.protocol.error.ErrorProtocolMessage;
import com.castellate.compendium.ws.WSMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Abstract ProtocolMessage that forms the base of all protocol messages in other protocols.
 * Provides shared logic and a series of abstract classes used to generalise processing
 */
public abstract class ProtocolMessage {
    private static final String TAG = "ProtocolMessage";
    protected JSONObject msgData = new JSONObject();

    /**
     * Get the class of the ProtocolMessage so it can be instantiated
     * @param protocolData protocol data
     * @return Class representing this object
     */
    public abstract Class<?> getClassObj(Map<String, String> protocolData);

    /**
     * Get the ALL_FIELDS value of the inner Fields class. This represents all fields that are
     * valid for this type of message
     * @return array of string field names
     */
    public abstract String[] getAllFields();


    /**
     * Construct a WebSocket compatible message from this ProtocolMessage so that it can be written
     * to the web socket
     * @param protocolData map of protocol data
     * @return string containing a message to be written to the web socket
     */
    public String getWebSocketMsg(Map<String, String> protocolData) {
        return WSMessages.createRoute(protocolData.get(ADR_PC), msgData).toString();
    }

    /**
     * Process an incoming message
     * @param protocolData map of name values pairs of protocol data
     * @return true if processed successfully, false if not
     * @throws ProtocolMessageException
     */
    public boolean processMessage(Map<String, String> protocolData) throws ProtocolMessageException {
        if (VerifySignature.class.isAssignableFrom(getClassObj(protocolData))) {
            VerifySignature verifySig = (VerifySignature) this;
            verifySig.getPublicKey(protocolData);
            boolean verified = this.verifySignature(verifySig.getSignature(), verifySig.getPublicKey(protocolData), verifySig.getSignatureFields(), protocolData);
            if (!verified) {
                Log.d(TAG, "Signature verification failed");
                return false;
            }
        }
        if (StoreProtocolData.class.isAssignableFrom(getClassObj(protocolData))) {
            addToProtocolData(protocolData, ((StoreProtocolData) this).getStoreFields());
        }

        return true;
    }

    /**
     * Process any sub messages, for examples, embedded encrypted sub messages
     * @param protocolData map of name value pairs of protocol data
     * @return true if successfully processed, false if not
     * @throws ProtocolMessageException
     */
    public boolean processSubMessages(Map<String, String> protocolData) throws ProtocolMessageException {
        if (EmbeddedEncryptedMessage.class.isAssignableFrom(getClassObj(protocolData))) {
            EmbeddedEncryptedMessage embedded = ((EmbeddedEncryptedMessage) this);
            ProtocolMessage subMessage;
            try {
                subMessage = decryptMessage(embedded.getEncryptedMsgField(), embedded.getEncryptedMessageClass(), protocolData);
            } catch (ProtocolMessageException e) {
                Log.e(TAG, "Exception processing sub message", e);
                return false;
            }
            return subMessage.processMessage(protocolData);

        }
        //If no embedded message we return true as this has completed
        return true;
    }

    /**
     * Prepare the outgoing message, loading any data, encrypting and signing as required
     * @param protocolData map of name value pairs of protocol data
     * @return true if successful, false it not
     * @throws ProtocolMessageException
     */
    public boolean prepareOutgoingMessage(Map<String, String> protocolData) throws ProtocolMessageException {
        getClassObj(protocolData);
        if (LoadProtocolData.class.isAssignableFrom(getClassObj(protocolData))) {
            addFromProtocolData(protocolData, ((LoadProtocolData) this).getLoadFields());
        }
        if (EmbeddedEncryptedMessage.class.isAssignableFrom(getClassObj(protocolData))) {
            EmbeddedEncryptedMessage embedded = (EmbeddedEncryptedMessage) this;
            ProtocolMessage protoMessage = null;
            try {
                protoMessage = (ProtocolMessage) embedded.getEncryptedMessageClass().newInstance();
                protoMessage.prepareOutgoingMessage(protocolData);
                msgData.put(embedded.getEncryptedMsgField(), encryptMessage(protoMessage, protocolData));
            } catch (IllegalAccessException | InstantiationException | JSONException e) {
                throw new ProtocolMessageException("Exception creating embedded encrypted message", e);
            } catch (ProtocolErrorPreKeyException e) {
                //Extra security check to make sure this is only an Error Message
                if (ProtocolMessage.class.isAssignableFrom(ErrorProtocolMessage.class)) {
                    Log.d(TAG, "Error trying to prepare error message, no derived key, will send in plaintext");
                    if (protoMessage != null) {
                        try {
                            msgData.put("Error", protoMessage.msgData);
                        } catch (JSONException jsonException) {
                            Log.e(TAG, "Exception trying to send plaintext error");
                        }
                    }
                }
            }

        }

        if (SignMessage.class.isAssignableFrom(getClassObj(protocolData))) {
            signMessage(((SignMessage) this).getSignatureField(), ((SignMessage) this).getSignatureFields(), protocolData);
        }

        return true;
    }

    /**
     * Get the JSON string representation of the underlying data
     * @return JSON String of the underling data
     */
    public String getString() {
        return this.msgData.toString();
    }

    /**
     * Get the JSONObject underlying this message
     * @return JSONObject with message data
     */
    public JSONObject getDataObj() {
        return this.msgData;
    }

    /**
     * Get a field from this message
     * @param field name of field to get
     * @return String value of field or ""
     */
    public String get(String field) {
        return msgData.optString(field, "");
    }

    /**
     * Get a JSONObject from this message
     * @param field name of JSONObject to get
     * @return JSONObject for that field or null
     */
    public JSONObject getJSON(String field) {
        return msgData.optJSONObject(field);
    }

    /**
     * Parse string data by first converting to JSON and then calling parse
     * @param data string of JSON data to parse
     * @return true if successful, false if not
     */
    public boolean parse(String data) {
        try {
            return parse(new JSONObject(data));
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Parse the JSONObject data and check it is valid against the ALL_FIELDS value
     * @param data JSONObject of message data
     * @return true if valid, false if not
     */
    public boolean parse(JSONObject data) {
        msgData = data;
        return this.validate(getAllFields());

    }

    /**
     * Load data from the map of protocol data into the underlying message
     * @param protocolData map of name value pairs of protocol data
     * @param fields list of string field names to retrieve from protocol data and load into message
     * @throws ProtocolMessageException
     */
    protected void addFromProtocolData(Map<String, String> protocolData, String... fields) throws ProtocolMessageException {
        for (String field : fields) {
            try {
                msgData.put(field, protocolData.get(field));
            } catch (JSONException e) {
                throw new ProtocolMessageException("Missing data trying to be loaded", e);
            }
        }
    }

    /**
     * Store data from the message to protocol data map
     * @param protocolData map of name value pairs of protocol data
     * @param fields list of fields to save into protocol data map
     */
    protected void addToProtocolData(Map<String, String> protocolData, String... fields) {
        for (String field : fields) {
            if (field.contains(":")) {
                String[] rename = field.split(":");
                protocolData.put(rename[1], get(rename[0]));
            } else {
                protocolData.put(field, get(field));
            }
        }
    }

    /**
     * Validate the message object by checking that the specified fields exist in
     * the message object and no fields other than those specified exist. As such, fields must be an
     * exhaustive list of fields.
     * @param fields string array of fields that should appear
     * @return true if valid, false if not
     */
    protected boolean validate(String[] fields) {
        Set<String> allFields = new HashSet<>();
        for (String field : fields) {
            if (!msgData.has(field)) {
                Log.d(TAG, "Missing field :" + field);
                return false;
            }
            allFields.add(field);
        }
        Iterator<String> itr = msgData.keys();
        while (itr.hasNext()) {
            String field = itr.next();
            if (!allFields.contains(field)) {
                Log.d(TAG, "Missing field :" + field);
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies the signature of a message creating the message digest from the values in the
     * message first, and if they don't exist, loading them protocol data
     * @param signature Base64 encoded signature
     * @param key Base64 encoded public key
     * @param fields array of string fields to add to the message digest
     * @param protocolData map of protocol data to load data from if necessary
     * @return true if valid, false if not
     */
    public boolean verifySignature(String signature, String key, String[] fields, Map<String, String> protocolData) {
        try {
            ECPublicKey publicKey = CryptoUtils.getPublicKey(key);
            byte[] signatureBytes = B64.decode(signature);
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            for (String field : fields) {
                if (msgData.has(field)) {
                    sig.update(msgData.getString(field).getBytes(StandardCharsets.UTF_8));
                } else if (protocolData.containsKey(field)) {
                    sig.update(Objects.requireNonNull(protocolData.get(field)).getBytes(StandardCharsets.UTF_8));
                }
            }
            return sig.verify(signatureBytes);
        } catch (CryptoException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | JSONException e) {
            return false;
        }


    }

    /**
     * Signs the message, creating the signature from value in the message first, but if they don't
     * exist will look in protocol data second.
     *
     * @param signatureField field name of field to store signature in
     * @param signatureFields array of string field name to add to message digest
     * @param protocolData protocol data to load data from if necessary
     * @throws ProtocolMessageException
     */
    public void signMessage(String signatureField, String[] signatureFields, Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            CompanionKeyManager ckm = new CompanionKeyManager();
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(ckm.getOrCreateIdentityKey().getPrivate());
            for (String field : signatureFields) {
                if (msgData.has(field)) {
                    sig.update(msgData.getString(field).getBytes(StandardCharsets.UTF_8));
                } else if (protocolData.containsKey(field)) {
                    sig.update(Objects.requireNonNull(protocolData.get(field)).getBytes(StandardCharsets.UTF_8));
                }
            }
            byte[] sigBytes = sig.sign();
            msgData.put(signatureField, B64.encode(sigBytes));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | JSONException | CryptoException e) {
            throw new ProtocolMessageException("Exception generating signature", e);
        }


    }

    /**
     * Decrypt a message and instantiates a new ProtocolMessage class of the appropriate type
     * @param encryptedMessageField field name containing encrypted message
     * @param messageClass class of plaintext message to be instantiated after decryption
     * @param protocolData map of protocol data
     * @return ProtocolMessage of type specified in messageClass instantied with decrypted data
     * @throws ProtocolMessageException
     */
    public ProtocolMessage decryptMessage(String encryptedMessageField, Class<?> messageClass, Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            EncryptedMessage encryptedMessage = new EncryptedMessage(getJSON(encryptedMessageField));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] cipherText = encryptedMessage.getCipher();
            GCMParameterSpec params = new GCMParameterSpec(128, encryptedMessage.getIV());
            byte[] key = B64.decode(protocolData.get(DERIVED_KEY));
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");


            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, params);
            byte[] plaintext = cipher.doFinal(cipherText);

            ProtocolMessage protoMessage = (ProtocolMessage) messageClass.newInstance();
            if (protoMessage == null) {
                throw new ProtocolMessageException("Cannot decode encrypted message");
            }
            if (!protoMessage.parse(new String(plaintext, StandardCharsets.UTF_8))) {
                throw new ProtocolMessageException("Protocol message fails validation");
            }
            return protoMessage;


        } catch (BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalAccessException | InstantiationException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ProtocolMessageException("Decryption failed", e);
        }
    }

    /**
     * Encrypts a message
     * TODO Check whether we need to pass in protocolMessage, isn't this always called from the appropriate subclass so we could just call this with out and use this
     * @param protocolMessage protocol message to encrypt
     * @param protocolData map of name value pairs
     * @return JSONObject containing EncryptedMessage
     * @throws ProtocolMessageException
     * @throws ProtocolErrorPreKeyException
     */
    public JSONObject encryptMessage(ProtocolMessage protocolMessage, Map<String, String> protocolData) throws ProtocolMessageException, ProtocolErrorPreKeyException {
        if (protocolData.get(DERIVED_KEY) == null && ProtocolMessage.class.isAssignableFrom(ErrorProtocolMessage.class)) {
            Log.d(TAG, "Error before key derivation");
            throw new ProtocolErrorPreKeyException("Cannot encrypt error message as the key is yet to be established");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKeySpec = CryptoUtils.getSecretKey(protocolData.get(DERIVED_KEY));
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] cipherText = cipher.doFinal(protocolMessage.getString().getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            return (new EncryptedMessage(cipherText, iv)).getDataObj();
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ProtocolMessageException("Encryption failed", e);
        }
    }

}
