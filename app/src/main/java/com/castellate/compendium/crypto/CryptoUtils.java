package com.castellate.compendium.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String PEM_HEAD = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_TAIL = "-----END PUBLIC KEY-----";

    public static String getPublicKeyId(PublicKey publicKey) throws CryptoException {
        byte[] derKey = publicKey.getEncoded();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Exception converting key to hex id",e);
        }
        digest.update(publicKey.getEncoded());
        return convertToHex(digest.digest());
    }

    public static String convertToHex(byte[] bytes){
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i < bytes.length; i++){
            buffer.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
            buffer.append(Character.forDigit((bytes[i] & 0xF), 16));
        }
        return buffer.toString();
    }
    public static ECPublicKey getPublicKey(String encodedKey) throws CryptoException {
        try {
            String derKey = encodedKey;
            if (encodedKey.startsWith(PEM_HEAD)) {
                derKey = encodedKey.replace(PEM_HEAD, "").replaceAll(System.lineSeparator(), "").replace(PEM_TAIL, "");
            }
            byte[] pubKeyBytes = B64.decode(derKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            return (ECPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException("Exception loading Public Key", e);
        }
    }

    public static String encodePublicKey(PublicKey publicKey) {
        return B64.encode(publicKey.getEncoded());
    }
    public static SecretKeySpec getSecretKey(String base64EncodedKey){
        return new SecretKeySpec(B64.decode(base64EncodedKey), "AES");
    }
    public static KeyPair generateEphemeralKeys() throws CryptoException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Exception generating EC Keypair", e);
        }
    }

    public static byte[] performECDH(KeyPair kp, PublicKey otherPartyPublicKey) throws CryptoException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
            ka.doPhase(otherPartyPublicKey, true);
            return ka.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException("Exception performing ECDH", e);
        }
    }
    public static String deriveKey(byte[] sharedSecret) throws CryptoException{
        try {
            byte[] derived = HMACSHA256Hkdf.computeHkdf(sharedSecret, null, "STS Handshake data".getBytes(StandardCharsets.UTF_8), 32);
            return B64.encode(derived);
        }catch (GeneralSecurityException e){
            throw new CryptoException("Exception deriving key", e);
        }
    }
}
