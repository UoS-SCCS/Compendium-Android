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
    private KeyStore keystore;

    public CompanionKeyManager() {
        try {
            this.keystore = KeyStore.getInstance("AndroidKeyStore");
            this.keystore.load(null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public synchronized KeyPair getOrCreateIdentityKey() {
        try {
            if (keystore.containsAlias(identityKey)) {
                PrivateKeyEntry privateKey = (PrivateKeyEntry) keystore.getEntry(identityKey, null);
                KeyPair kp = new KeyPair(privateKey.getCertificate().getPublicKey(), privateKey.getPrivateKey());
                return kp;
            } else {
                final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(identityKey, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256).setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")).setUnlockedDeviceRequired(true).build();
                keyPairGenerator.initialize(keyGenParameterSpec);

                return keyPairGenerator.generateKeyPair();
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PublicKey getPublicSigningKey(String keyId){
            KeyPair key = getOrCreateSigningKeyPair(keyId);
            return key.getPublic();
    }
    public Signature getSignatureObject(String keyId){
        try {
            KeyPair kp = getOrCreateSigningKeyPair(keyId);
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(kp.getPrivate());
            return sig;
        }catch(InvalidKeyException | NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }
    public Cipher getEncryptionCipher(String keyId){
        try {
            SecretKey key = getOrCreateSecretKey(keyId);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        }catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e){
            e.printStackTrace();
        }
        return null;
    }

    public Cipher getDecryptionCipher(String keyId, byte[] iv){
        try {
            SecretKey key = getOrCreateSecretKey(keyId);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return cipher;
        }catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e){
            e.printStackTrace();
        }
        return null;
    }
    private KeyPair getOrCreateSigningKeyPair(String keyId) {
        //TODO handle someone request an incorrect key
        try {
            if (keystore.containsAlias(keyId)) {
                PrivateKeyEntry privateKey = (PrivateKeyEntry) keystore.getEntry(keyId, null);
                KeyPair kp = new KeyPair(privateKey.getCertificate().getPublicKey(), privateKey.getPrivateKey());
                return kp;
            }
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(keyId,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setUserAuthenticationRequired(true).build();
            keyPairGenerator.initialize(keyGenParameterSpec);

            return keyPairGenerator.generateKeyPair();
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void testAddKey(){
        this.getOrCreateSecretKey("TestKey");
    }
    public void testDelKey(){
        try {
            this.keystore.deleteEntry("TestKey");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

    }
    public List<String> getKeyList(){
        List<String> list = new ArrayList<>();

        try {
        Enumeration<String> en = keystore.aliases();

        while(en.hasMoreElements()){
            list.add(en.nextElement());
        }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return list;
    }
    private SecretKey getOrCreateSecretKey(String keyId) {
        try {
            if (keystore.containsAlias(keyId)) {
                return (SecretKey) keystore.getKey(keyId, null);
            }
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(keyId,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true).build();
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGen.init(spec);
            return keyGen.generateKey();
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
}
