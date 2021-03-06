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

/**
 * Crypto Utils for common operations
 */
public class CryptoUtils {
    private static final String PEM_HEAD = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_TAIL = "-----END PUBLIC KEY-----";
    private static final String HASH_ALG="SHA256";
    private static final String KEY_ALG="ECDH";
    private static final byte[] HKDF_INFO="STS Handshake data".getBytes(StandardCharsets.UTF_8);
    private static final int HKDF_KEY_SIZE=32;

    /**
     * Get Public Key identifier from a Public Key String, first loading the public key and then
     * calculating the SHA256 of the public key bytes
     * @param publicKeyString public key to calculate ID for
     * @return String of hex encoded public key ID bytes
     * @throws CryptoException
     */
    public static String getPublicKeyId(String publicKeyString) throws CryptoException {
        PublicKey publicKey = CryptoUtils.getPublicKey(publicKeyString);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Exception converting key to hex id",e);
        }
        digest.update(publicKey.getEncoded());
        return convertToHex(digest.digest());
    }

    /**
     * Get the public Key ID from the specified key
     * @param publicKey Public Key to calculate ID for
     * @return Hex encoded public key bytes
     * @throws CryptoException
     */
    public static String getPublicKeyId(PublicKey publicKey) throws CryptoException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Exception converting key to hex id",e);
        }
        digest.update(publicKey.getEncoded());
        return convertToHex(digest.digest());
    }

    /**
     * Convert bytes to hex
     * @param bytes bytes to be converted
     * @return hex string of bytes
     */
    public static String convertToHex(byte[] bytes){
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            buffer.append(Character.forDigit((aByte >> 4) & 0xF, 16));
            buffer.append(Character.forDigit((aByte & 0xF), 16));
        }
        return buffer.toString();
    }

    /**
     * Load a string represetation of a public key into a ECPublicKey. This supports both
     * PEM and DER Base64 encoded string
     * @param encodedKey Base64 encoded public key
     * @return ECPublicKey object
     * @throws CryptoException
     */
    public static ECPublicKey getPublicKey(String encodedKey) throws CryptoException {
        if(encodedKey==null){
            throw new CryptoException("Encoded key is null");
        }
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

    /**
     * Encoded a Public key to a Base64 String
     * @param publicKey public key to be encoded
     * @return Base64 encoding of the public key
     */
    public static String encodePublicKey(PublicKey publicKey) {
        return B64.encode(publicKey.getEncoded());
    }

    /**
     * Loads a SecretKey from a Base64 encoding
     * @param base64EncodedKey Base64 encoded secret key
     * @return SecretKey
     */
    public static SecretKeySpec getSecretKey(String base64EncodedKey){
        return new SecretKeySpec(B64.decode(base64EncodedKey), "AES");
    }

    /**
     * Generate an ephemeral EC Key Pair
     * @return KeyPair
     * @throws CryptoException
     */
    public static KeyPair generateEphemeralKeys() throws CryptoException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Exception generating EC Keypair", e);
        }
    }

    /**
     * Perform the ECDH and generate a shared secret
     * @param kp Key Pair of this device
     * @param otherPartyPublicKey Key Pair of other device
     * @return bytes containing shared secret
     * @throws CryptoException
     */
    public static byte[] performECDH(KeyPair kp, PublicKey otherPartyPublicKey) throws CryptoException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance(KEY_ALG);
            ka.init(kp.getPrivate());
            ka.doPhase(otherPartyPublicKey, true);
            return ka.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException("Exception performing ECDH", e);
        }
    }

    /**
     * Derive and AES key from the shared secret use an HMACSHA256 based HKDF
     * @param sharedSecret bytes of shared secret from DH
     * @return derived key as Base64 encoded value
     * @throws CryptoException
     */
    public static String deriveKey(byte[] sharedSecret) throws CryptoException{
        try {
            byte[] derived = HMACSHA256Hkdf.computeHkdf(sharedSecret, null, HKDF_INFO, HKDF_KEY_SIZE);
            return B64.encode(derived);
        }catch (GeneralSecurityException e){
            throw new CryptoException("Exception deriving key", e);
        }
    }
}
