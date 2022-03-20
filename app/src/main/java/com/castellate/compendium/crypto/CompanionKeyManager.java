/*
 *
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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Key Manager for the companion device. Handles access to the keys in the AndroidKeyStore and
 * the storing of public keys received from PCs
 */
public class CompanionKeyManager {
    private static final String identityKey = "CompanionDeviceIdentity";
    private static final String CURVE = "secp256r1";
    public static final String KEYSTORE = "AndroidKeyStore";
    private static final int KEY_SIZE = 256;
    private static final int TAG_LENGTH = 128;
    private static final String SYMMETRIC_ALG = "AES/GCM/NoPadding";
    private static final String SIGNATURE_ALG="SHA256withECDSA";
    private final KeyStore keystore;

    /**
     * Create a new Companion Key Manager
     * @throws CryptoException
     */
    public CompanionKeyManager() throws CryptoException {

        try {
            this.keystore = KeyStore.getInstance(KEYSTORE);
            this.keystore.load(null);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new CryptoException("Exception creating CompanionKeyManager", e);
        }
    }

    /**
     * Gets or create the identity key  for this device. This key pair is stored in the
     * AndroidKeyStore but is not protected by a biometric. This is to allow communications
     * to be established without a biometric check
     *
     * @return Generated or retrieved KeyPair
     * @throws CryptoException
     */
    public synchronized KeyPair getOrCreateIdentityKey() throws CryptoException{
        try {
            if (keystore.containsAlias(identityKey)) {
                PrivateKeyEntry privateKey = (PrivateKeyEntry) keystore.getEntry(identityKey, null);
                return new KeyPair(privateKey.getCertificate().getPublicKey(), privateKey.getPrivateKey());
            } else {
                final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE);
                final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(identityKey, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256).setAlgorithmParameterSpec(new ECGenParameterSpec(CURVE)).setUnlockedDeviceRequired(true).build();
                keyPairGenerator.initialize(keyGenParameterSpec);

                return keyPairGenerator.generateKeyPair();
            }
        } catch (KeyStoreException | NoSuchAlgorithmException |NoSuchProviderException|InvalidAlgorithmParameterException| UnrecoverableEntryException e) {
            throw new CryptoException("Exception getting or creating identity key",e);
        }
    }

    /**
     * Get a signing key used for user verification, creating it if it doesn't exist
     * @param keyId ID of the key
     * @return Created or retrieved Public Key
     * @throws CryptoException
     */
    public PublicKey getPublicSigningKey(String keyId) throws CryptoException {
        KeyPair key = getOrCreateSigningKeyPair(keyId);
        return key.getPublic();
    }

    /**
     * Gets a signature object with the specified Key ID. This is the needed because the
     * Key is protected by biometric authentication, so direct access is not permitted. Instead
     * a Signature object has to be created and then passed to the biometric prompt to be able
     * to undertake the signing operation
     * @param keyId ID of the key
     * @return Initialised Signature object for Signing with specified key
     * @throws CryptoException
     */
    public Signature getSignatureObject(String keyId) throws CryptoException {
        try {
            KeyPair kp = getOrCreateSigningKeyPair(keyId);
            Signature sig = Signature.getInstance(SIGNATURE_ALG);
            sig.initSign(kp.getPrivate());
            return sig;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoException("Exception getting signature object",e);
        }
    }

    /**
     * Gets a Cipher object for encryption with the specified Key ID. This is the needed because the
     * Key is protected by biometric authentication, so direct access is not permitted. Instead
     * a Cipher object has to be created and then passed to the biometric prompt to be able
     * to undertake the signing operation.
     * @param keyId ID of the key
     * @return Initialised Cipher object with the key ready for encryption
     * @throws CryptoException
     */
    public Cipher getEncryptionCipher(String keyId) throws CryptoException {
        try {
            SecretKey key = getOrCreateSecretKey(keyId);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new CryptoException("Exception getting encryption cipher",e);
        }

    }

    /**
     * Gets a Cipher object for decryption with the specified Key ID. This is the needed because the
     * Key is protected by biometric authentication, so direct access is not permitted. Instead
     * a Cipher object has to be created and then passed to the biometric prompt to be able
     * to undertake the signing operation.
     * @param keyId ID of the key
     * @param iv IV used during encryption
     * @return Initialised Cipher object with the key ready for decryption
     * @throws CryptoException
     */
    public Cipher getDecryptionCipher(String keyId, byte[] iv) throws CryptoException {
        try {
            SecretKey key = getOrCreateSecretKey(keyId);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALG);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            return cipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new CryptoException("Exception getting decryption cipher",e);
        }

    }

    /**
     * Gets or creates a biometrically protected signing key with the specified key ID
     * @param keyId unique ID of the key
     * @return KeyPair of retrieved or created key pair
     * @throws CryptoException
     */
    private KeyPair getOrCreateSigningKeyPair(String keyId) throws CryptoException {
        //TODO handle someone request an incorrect key
        try {
            if (keystore.containsAlias(keyId)) {
                PrivateKeyEntry privateKey = (PrivateKeyEntry) keystore.getEntry(keyId, null);
                return new KeyPair(privateKey.getCertificate().getPublicKey(), privateKey.getPrivateKey());
            }
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE);
            final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(keyId, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256).setAlgorithmParameterSpec(new ECGenParameterSpec(CURVE)).setUserAuthenticationRequired(true).build();
            keyPairGenerator.initialize(keyGenParameterSpec);

            return keyPairGenerator.generateKeyPair();
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | UnrecoverableEntryException e) {
            throw new CryptoException("Exception getting or creating key pair",e);
        }
    }


    /**
     * Get a list of all the key IDs, note this retrieves all Key IDs in the AndroidKeyStore
     * including the identity key
     * @return List of keyID strings
     * @throws CryptoException
     */
    public List<String> getKeyList() throws CryptoException {
        List<String> list = new ArrayList<>();

        try {
            Enumeration<String> en = keystore.aliases();

            while (en.hasMoreElements()) {
                list.add(en.nextElement());
            }
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception getting key list", e);
        }
        return list;
    }

    /**
     * Checks if the specified KeyId is new by checking if it currently exists in the keystore
     * @param keyId key ID to check
     * @return true if the key is new, false if not
     * @throws CryptoException
     */
    public boolean isNewKey(String keyId)throws CryptoException{
        try {
            return !keystore.containsAlias(keyId);
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception checking if key exists",e);
        }
    }

    /**
     * Gets or creates a new symmetric key protected by a biometric
     * @param keyId Key ID
     * @return Created or retrieved SecretKey
     * @throws CryptoException
     */
    private SecretKey getOrCreateSecretKey(String keyId) throws CryptoException {
        try {
            if (keystore.containsAlias(keyId)) {
                return (SecretKey) keystore.getKey(keyId, null);
            }
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(keyId, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(KEY_SIZE).setUserAuthenticationRequired(true).build();
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
            keyGen.init(spec);
            return keyGen.generateKey();
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | UnrecoverableKeyException e) {
            throw new CryptoException("Exception getting or creating secret key", e);
        }
    }

    /**
     * Deletes the specified key from the KeyStore
     * @param keyId Key ID to delete
     * @throws CryptoException
     */
    public void deleteKey(String keyId) throws CryptoException{
        try {
            keystore.deleteEntry(keyId);
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception cleaning up unused key",e);
        }
    }

    /**
     * Deletes a key that may have been optimistically created during registration
     * @param keyId ID of the key to be removed
     * @throws CryptoException
     */
    public void cleanUpUnusedKey(String keyId) throws CryptoException{
        deleteKey(keyId);
    }

    /**
     * Reset the keystore by deleting all entries, including the identity key
     * @throws CryptoException
     */
    public void reset() throws CryptoException{
        try {
            Enumeration<String> en = keystore.aliases();
            while (en.hasMoreElements()) {
                keystore.deleteEntry(en.nextElement());
            }
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception resetting device",e);
        }
    }
}
