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

public class CompanionKeyManager {
    private static final String identityKey = "CompanionDeviceIdentity";
    private static final String CURVE = "secp256r1";
    public static final String KEYSTORE = "AndroidKeyStore";
    private static final int KEY_SIZE = 256;
    private static final int TAG_LENGTH = 128;
    private static final String SYMMETRIC_ALG = "AES/GCM/NoPadding";
    private static final String SIGNATURE_ALG="SHA256withECDSA";
    private final KeyStore keystore;

    public CompanionKeyManager() throws CryptoException {

        try {
            this.keystore = KeyStore.getInstance(KEYSTORE);
            this.keystore.load(null);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new CryptoException("Exception creating CompanionKeyManager", e);
        }
    }

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

    public PublicKey getPublicSigningKey(String keyId) throws CryptoException {
        KeyPair key = getOrCreateSigningKeyPair(keyId);
        return key.getPublic();
    }

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
    public boolean isNewKey(String keyId)throws CryptoException{
        try {
            return !keystore.containsAlias(keyId);
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception checking if key exists",e);
        }
    }
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

    public void deleteKey(String keyId) throws CryptoException{
        try {
            keystore.deleteEntry(keyId);
        } catch (KeyStoreException e) {
            throw new CryptoException("Exception cleaning up unused key",e);
        }
    }
    public void cleanUpUnusedKey(String keyId) throws CryptoException{
        deleteKey(keyId);
    }

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
