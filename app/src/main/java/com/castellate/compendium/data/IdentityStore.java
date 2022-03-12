package com.castellate.compendium.data;

import android.util.Log;

import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.exceptions.StorageException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;

public class IdentityStore {
    private static final String FILE_NAME = "identityStore.json";
    private static final String NAME_IDX = "names";
    private static final String KEY_NAME_IDX = "key-names";
    private static final String KEYS = "keys";
    private static final String NAME = "name";
    private static final String APPS = "apps";
    private File dataFile;
    private volatile JSONObject data;
    private static final String TAG = "IdentityStore";
    private static final IdentityStore instance = new IdentityStore();
    private volatile boolean initialised = false;
    public static IdentityStore getInstance() {
        return instance;
    }

    private IdentityStore() {

    }

    public boolean isInitialised(){
        return this.initialised;
    }
    private void checkInitialised() throws StorageException {
        if(!this.initialised){
            throw new StorageException("Storage not initialised");
        }
    }
    private synchronized void createStructure() throws StorageException {
        data = new JSONObject();
        try {
            data.put(NAME_IDX, new JSONObject());
            data.put(KEYS, new JSONObject());
            data.put(KEY_NAME_IDX, new JSONObject());
            data.put(NAME, "");
            data.put(APPS, new JSONObject());
            store();
        } catch (JSONException e) {
            throw new StorageException("Exception creating JSON Structure", e);
        }
    }

    public synchronized void init(File storageDirectory) throws StorageException {
        if(this.initialised){
            return;
        }
        dataFile = new File(storageDirectory, FILE_NAME);
        this.initialised = true;
        if (!dataFile.exists()) {
            createStructure();
        }
        load();

    }

    private synchronized void load() throws StorageException {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(this.dataFile));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
            this.data = new JSONObject(buffer.toString());
            Log.d(TAG,"data:" + this.data.toString());
        } catch (IOException | JSONException e) {
            throw new StorageException("Exception writing JSON data", e);
        } finally {
            try {
                if(br!=null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG,"IOException in finally closing reader",e);
            }
        }

    }

    private synchronized void store() throws StorageException {
        checkInitialised();
        FileWriter fw = null;
        try {
            fw = new FileWriter(this.dataFile);
            fw.write(this.data.toString());
        } catch (IOException e) {
            throw new StorageException("Exception writing JSON to file", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    Log.e(TAG,"IOException in finally whilst writing JSON to file",e);
                }
            }
        }


    }

    public void storePublicIdentity(String name, ECPublicKey key) throws StorageException {
        checkInitialised();
        try {
            String keyId = CryptoUtils.getPublicKeyId(key);
            String keyStr = CryptoUtils.encodePublicKey(key);
            storePublicIdentity(name, keyStr, keyId);
        } catch (CryptoException e) {
            throw new StorageException("Exception storing public key", e);
        }
    }

    public void storePublicIdentity(String name, String key) throws StorageException {
        checkInitialised();
        try {
            String keyId = CryptoUtils.getPublicKeyId(CryptoUtils.getPublicKey(key));
            storePublicIdentity(name, key, keyId);
        } catch (CryptoException e) {
            throw new StorageException("Exception storing public key", e);
        }
    }

    public synchronized void storePublicIdentity(String name, String key, String keyId) throws StorageException {
        checkInitialised();
        try {
            data.getJSONObject(KEYS).put(keyId, key);
            data.getJSONObject(NAME_IDX).put(name, keyId);
            data.getJSONObject(KEY_NAME_IDX).put(keyId,name);
            this.store();
        }catch(JSONException e){
            throw new StorageException("Exception adding key to JSON",e);
        }
    }
    public String getPublicIdentityByName(String name) throws StorageException{
        checkInitialised();
        try {
            if (this.data.getJSONObject(NAME_IDX).has(name)) {
                String keyId = this.data.getJSONObject(NAME_IDX).getString(name);
                return getPublicIdentityById(keyId);
            }else {
                return null;
            }
        }catch(JSONException e){
            throw new StorageException("Exception reading key by name",e);
        }
    }
    public String getNameByKeyID(String keyId) throws StorageException{
        checkInitialised();
        try {
            if (this.data.getJSONObject(KEY_NAME_IDX).has(keyId)) {
                return this.data.getJSONObject(KEY_NAME_IDX).getString(keyId);
            } else {
                return null;
            }
        }catch(JSONException e){
            throw new StorageException("Exception reading key by id",e);
        }
    }
    public String getPublicIdentityById(String keyId) throws StorageException{
        checkInitialised();
        try {
            if (this.data.getJSONObject(KEYS).has(keyId)) {
                return this.data.getJSONObject(KEYS).getString(keyId);
            } else {
                return null;
            }
        }catch(JSONException e){
            throw new StorageException("Exception reading key by id",e);
        }
    }
    public List<String> getKeyNames() throws StorageException {
        checkInitialised();
        try {

            JSONArray names = this.data.getJSONObject(NAME_IDX).names();
            if(names!=null) {
                return convertJSONArrayToList(names);
            }
            return new ArrayList<>();
        } catch (JSONException e) {
            throw new StorageException("Exception getting names",e);
        }
    }
    public List<String> getKeyIds() throws StorageException {
        checkInitialised();
        try {
            JSONArray names = this.data.getJSONObject(KEYS).names();
            if(names!=null) {
                return convertJSONArrayToList(names);
            }
            return new ArrayList<>();
        } catch (JSONException e) {
            throw new StorageException("Exception getting id",e);
        }
    }
    private List<String> convertJSONArrayToList(JSONArray arr) throws JSONException {
        List<String> list = new ArrayList<>();
        for(int i=0;i<arr.length();i++){
            list.add(arr.getString(i));
        }
        return list;
    }
    public String getName() throws StorageException {
        checkInitialised();
        try {
            return this.data.getString(NAME);
        }catch(JSONException e){
            throw new StorageException("Exception getting name",e);
        }
    }
    public void setName(String name) throws StorageException{
        checkInitialised();
        try {
            this.data.put(NAME, name);
            this.store();
        }catch(JSONException e){
            throw new StorageException("Exception setting name",e);
        }
    }


    public boolean hasPublicIdentity(String keyId)throws StorageException {
        checkInitialised();
        try {
            return this.data.getJSONObject(KEYS).has(keyId);
        }catch(JSONException e){
            throw new StorageException("Exception setting name",e);
        }

    }
}
