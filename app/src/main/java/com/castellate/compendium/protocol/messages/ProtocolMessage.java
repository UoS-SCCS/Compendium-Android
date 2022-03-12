package com.castellate.compendium.protocol.messages;

import static com.castellate.compendium.protocol.messages.Constants.ADR_PC;
import static com.castellate.compendium.protocol.messages.Constants.DERIVED_KEY;

import android.util.Log;

import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
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
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class ProtocolMessage {
    private static final String TAG = "ProtocolMessage";
    protected JSONObject msgData =new JSONObject();

    public abstract Class<?> getClassObj(Map<String,String> protocolData);
    public abstract String[] getAllFields();


    public String getWebSocketMsg(Map<String, String> protocolData) {
        return WSMessages.createRoute(protocolData.get(ADR_PC), msgData).toString();
    }

    public boolean processMessage(Map<String, String> protocolData) throws ProtocolMessageException {
        if (VerifySignature.class.isAssignableFrom(getClassObj(protocolData))) {
            VerifySignature verifySig = (VerifySignature) this;
            verifySig.getPublicKey(protocolData);
            boolean verified = this.verifySignature(verifySig.getSignature(), verifySig.getPublicKey(protocolData), verifySig.getSignatureFields(), protocolData);
            if (!verified) {
                Log.d(TAG,"Signature verification failed");
                return false;
            }
        }
        if (StoreProtocolData.class.isAssignableFrom(getClassObj(protocolData))) {
            addToProtocolData(protocolData, ((StoreProtocolData) this).getStoreFields());
        }

        return true;
    }

    public boolean processSubMessages(Map<String, String> protocolData) throws ProtocolMessageException{
        if (EmbeddedEncryptedMessage.class.isAssignableFrom(getClassObj(protocolData))) {
            EmbeddedEncryptedMessage embedded = ((EmbeddedEncryptedMessage) this);
            ProtocolMessage subMessage = null;
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
            }

        }

        if (SignMessage.class.isAssignableFrom(getClassObj(protocolData))) {
            signMessage(((SignMessage) this).getSignatureField(), ((SignMessage) this).getSignatureFields(), protocolData);
        }

        return true;
    }

    public String getString() {
        return this.msgData.toString();
    }

    public JSONObject getDataObj() {
        return this.msgData;
    }

    public String get(String field) {
        return msgData.optString(field, "");
    }

    public JSONObject getJSON(String field) {
        return msgData.optJSONObject(field);
    }
    public boolean parse(String data) {
        try {
            return parse(new JSONObject(data));
        } catch (JSONException e) {
            return false;
        }
    }
    public boolean parse(JSONObject data) {
        msgData=data;
        return this.validate(getAllFields());

    }
    protected void addFromProtocolData(Map<String, String> protocolData, String... fields) throws ProtocolMessageException {
        for (String field : fields) {
            try {
                msgData.put(field, protocolData.get(field));
            } catch (JSONException e) {
                throw new ProtocolMessageException("Missing data trying to be loaded", e);
            }
        }
    }

    protected void addToProtocolData(Map<String, String> protocolData, String... fields) {
        for (String field : fields) {
            if(field.contains(":")){
                String[] rename = field.split(":");
                protocolData.put(rename[1], get(rename[0]));
            }else {
                protocolData.put(field, get(field));
            }
        }
    }

    protected boolean validate(String[] fields) {
        Set<String> allFields = new HashSet<String>();
        for (String field : fields) {
            if (!msgData.has(field)) {
                Log.d(TAG,"Missing field :" + field);
                return false;
            }
            allFields.add(field);
        }
        Iterator<String> itr = msgData.keys();
        while (itr.hasNext()) {
            String field = itr.next();
            if (!allFields.contains(field)) {
                Log.d(TAG,"Missing field :" + field);
                return false;
            }
        }
        return true;
    }

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
                    sig.update(protocolData.get(field).getBytes(StandardCharsets.UTF_8));
                }
            }
            return sig.verify(signatureBytes);
        } catch (CryptoException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | JSONException e) {
            return false;
        }


    }

    public void signMessage(String signatureField, String[] signatureFields, Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            CompanionKeyManager ckm = new CompanionKeyManager();
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(ckm.getOrCreateIdentityKey().getPrivate());
            for (String field : signatureFields) {
                if (msgData.has(field)) {
                    sig.update(msgData.getString(field).getBytes(StandardCharsets.UTF_8));
                } else if (protocolData.containsKey(field)) {
                    sig.update(protocolData.get(field).getBytes(StandardCharsets.UTF_8));
                }
            }
            byte[] sigBytes = sig.sign();
            msgData.put(signatureField, B64.encode(sigBytes));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | JSONException | CryptoException e) {
            throw new ProtocolMessageException("Exception generating signature",e);
        }


    }

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

    public JSONObject encryptMessage(ProtocolMessage protocolMessage, Map<String, String> protocolData) throws ProtocolMessageException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKeySpec = CryptoUtils.getSecretKey(protocolData.get(DERIVED_KEY));
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] cipherText = cipher.doFinal(protocolMessage.getString().getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            return (new EncryptedMessage(cipherText, iv)).getDataObj();
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException |  NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ProtocolMessageException("Encryption failed", e);
        }
    }

}
