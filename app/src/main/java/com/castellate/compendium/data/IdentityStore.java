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

package com.castellate.compendium.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.ui.apps.AppItem;
import com.castellate.compendium.ui.keys.KeyItem;

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
import java.util.Iterator;
import java.util.List;

/**
 * Identity store to store received public key. Note, unlike the Python implementation, this does
 * not store any private credentials, which are all stored within the AndroidKeyStore.
 *
 * The data within the IdentityStore is stored in a JSON file that is written to the apps
 * data folder, which should be protected by the OS from unauthorised access.
 */
public class IdentityStore implements Initializer<IdentityStore> {
    private static final String FILE_NAME = "identityStore.json";
    private static final String NAME_IDX = "names";
    private static final String KEY_NAME_IDX = "key-names";
    private static final String KEYS = "keys";
    private static final String NAME = "name";
    private static final String APPS = "apps";
    private static final String TAG = "IdentityStore";
    private static final IdentityStore instance = new IdentityStore();
    private File dataFile;
    private volatile JSONObject data;
    private volatile boolean initialised = false;

    private IdentityStore() {

    }

    /**
     * Get an instance of IdentityStore
     * @return IdentityStore instance
     */
    public static IdentityStore getInstance() {
        return instance;
    }

    /**
     * Checks if the IdentityStore is initialised
     * @return true if it is, false if not
     */
    public boolean isInitialised() {
        return this.initialised;
    }

    private void checkInitialised() throws StorageException {
        if (!this.initialised) {
            throw new StorageException("Storage not initialised");
        }
    }

    /**
     * Reset the identity store by deleting all stored data
     * @throws StorageException
     */
    private synchronized  void resetStore()throws StorageException{
        createStructure();
    }

    /**
     * Create the basic empty structure of the identity store JSON
     * @throws StorageException
     */
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

    /**
     * Initialise by loading the underlying data in the JSON file
     * @param storageDirectory director where file is stored
     * @throws StorageException
     */
    public synchronized void init(File storageDirectory) throws StorageException {
        if (this.initialised) {
            return;
        }
        dataFile = new File(storageDirectory, FILE_NAME);
        this.initialised = true;
        if (!dataFile.exists()) {
            createStructure();
        }
        load();

    }

    /**
     * Loads the data from the file
     * @throws StorageException
     */
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
            Log.d(TAG, "data:" + this.data.toString());
        } catch (IOException | JSONException e) {
            throw new StorageException("Exception writing JSON data", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException in finally closing reader", e);
            }
        }

    }

    /**
     * Save the data to the file
     * @throws StorageException
     */
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
                    Log.e(TAG, "IOException in finally whilst writing JSON to file", e);
                }
            }
        }


    }

    /**
     * Store a public key against a name and Public Key ID. This will calculate the Public Key ID
     * from the key itself
     * @param name name of the device
     * @param key public key to store
     * @throws StorageException
     */
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

    /**
     * Store a public key against a name, first converting the Base64 string into a public key
     * @param name device name
     * @param key public key as a Base64 string
     * @throws StorageException
     */
    public void storePublicIdentity(String name, String key) throws StorageException {
        checkInitialised();
        try {
            String keyId = CryptoUtils.getPublicKeyId(CryptoUtils.getPublicKey(key));
            storePublicIdentity(name, key, keyId);
        } catch (CryptoException e) {
            throw new StorageException("Exception storing public key", e);
        }
    }

    /**
     * Store the public key against the name and key id
     * @param name device name
     * @param key public key as Base64 string
     * @param keyId public key ID
     * @throws StorageException
     */
    public synchronized void storePublicIdentity(String name, String key, String keyId) throws StorageException {
        checkInitialised();
        try {
            data.getJSONObject(KEYS).put(keyId, key);
            data.getJSONObject(NAME_IDX).put(name, keyId);
            data.getJSONObject(KEY_NAME_IDX).put(keyId, name);
            this.store();
        } catch (JSONException e) {
            throw new StorageException("Exception adding key to JSON", e);
        }
    }

    /**
     * Get a public key from the name
     * @param name name to look up
     * @return Base64 encoded public key or null if it doesn't exist
     * @throws StorageException
     */
    public String getPublicIdentityByName(String name) throws StorageException {
        checkInitialised();
        try {
            if (this.data.getJSONObject(NAME_IDX).has(name)) {
                String keyId = this.data.getJSONObject(NAME_IDX).getString(name);
                return getPublicIdentityById(keyId);
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new StorageException("Exception reading key by name", e);
        }
    }

    /**
     * Gets the name from the KeyID
     * @param keyId key ID to look up
     * @return name of associated device of null if it doesn't exist
     * @throws StorageException
     */
    public String getNameByKeyID(String keyId) throws StorageException {
        checkInitialised();
        try {
            if (this.data.getJSONObject(KEY_NAME_IDX).has(keyId)) {
                return this.data.getJSONObject(KEY_NAME_IDX).getString(keyId);
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new StorageException("Exception reading key by id", e);
        }
    }

    /**
     * Get the public key from the key id
     * @param keyId key ID to look up
     * @return Base64 encoded public key or null if it doesn't exist
     * @throws StorageException
     */
    public String getPublicIdentityById(String keyId) throws StorageException {
        checkInitialised();
        try {
            if (this.data.getJSONObject(KEYS).has(keyId)) {
                return this.data.getJSONObject(KEYS).getString(keyId);
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new StorageException("Exception reading key by id", e);
        }
    }

    /**
     * Get a keys name the AppIDs associated with a Key ID
     * @param keyId key ID to look up
     * @return list of AppItems associated with the Key ID
     * @throws StorageException
     */
    public List<AppItem> getKeyNameAppEntries(String keyId) throws StorageException {
        checkInitialised();
        try {

            List<AppItem> appEntries = new ArrayList<>();
            JSONObject appsIdx =this.getKeyApps(keyId);
            Iterator<String> itr = appsIdx.keys();
            while(itr.hasNext()){
                String name = itr.next();
                AppItem keyItem = new AppItem(name,appsIdx.getString(name));
                appEntries.add(keyItem);
            }
            return appEntries;
        } catch (JSONException e) {
            throw new StorageException("Exception getting names", e);
        }
    }

    /**
     * Get the list of names stored
     * @return List of KeyItems
     * @throws StorageException
     */
    public List<KeyItem> getKeyNameEntries() throws StorageException {
        checkInitialised();
        try {
            List<KeyItem> keyEntries = new ArrayList<>();
            JSONObject namesIdx =this.data.getJSONObject(NAME_IDX);
            Iterator<String> itr = namesIdx.keys();
            while(itr.hasNext()){
                String name = itr.next();
                KeyItem keyItem = new KeyItem(name,namesIdx.getString(name));
                keyEntries.add(keyItem);
            }
            return keyEntries;
        } catch (JSONException e) {
            throw new StorageException("Exception getting names", e);
        }
    }

    /**
     * Get the names of the keys stored
     * @return List of names
     * @throws StorageException
     */
    public List<String> getKeyNames() throws StorageException {
        checkInitialised();
        try {

            JSONArray names = this.data.getJSONObject(NAME_IDX).names();
            if (names != null) {
                return convertJSONArrayToList(names);
            }
            return new ArrayList<>();
        } catch (JSONException e) {
            throw new StorageException("Exception getting names", e);
        }
    }

    /**
     * Get the key IDs stored
     * @return
     * @throws StorageException
     */
    public List<String> getKeyIds() throws StorageException {
        checkInitialised();
        try {
            JSONArray names = this.data.getJSONObject(KEYS).names();
            if (names != null) {
                return convertJSONArrayToList(names);
            }
            return new ArrayList<>();
        } catch (JSONException e) {
            throw new StorageException("Exception getting id", e);
        }
    }

    /**
     * Convert a JSONArray to a Java List
     * @param arr JSONArray to convert
     * @return List of strings that were in the JSON Array
     * @throws JSONException
     */
    private List<String> convertJSONArrayToList(JSONArray arr) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getString(i));
        }
        return list;
    }

    /**
     * Get the device name
     * @return name of device
     * @throws StorageException
     */
    public String getName() throws StorageException {
        checkInitialised();
        try {
            return this.data.getString(NAME);
        } catch (JSONException e) {
            throw new StorageException("Exception getting name", e);
        }
    }

    /**
     * Set the device name
     * @param name name of the device
     * @throws StorageException
     */
    public void setName(String name) throws StorageException {
        checkInitialised();
        try {
            this.data.put(NAME, name);
            this.store();
        } catch (JSONException e) {
            throw new StorageException("Exception setting name", e);
        }
    }


    /**
     * Do we have a public key for this particular key ID
     * @param keyId key ID to check
     * @return true if we do, false it not
     * @throws StorageException
     */
    public boolean hasPublicIdentity(String keyId) throws StorageException {
        checkInitialised();
        try {
            return this.data.getJSONObject(KEYS).has(keyId);
        } catch (JSONException e) {
            throw new StorageException("Exception setting name", e);
        }
    }

    /**
     * Gets a JSONObject from the passed in JSONObject, used as an internal method to better
     * handle exception
     * @param obj JSONObject to try to retrieve a JSONObject from
     * @param key field name to retrieve JSONObject from
     * @return JSONObject
     * @throws StorageException
     */
    private JSONObject getJsonObject(JSONObject obj, String key) throws StorageException {
        try {
            JSONObject result = obj.getJSONObject(key);
            if (result == null) {
                throw new StorageException("JSONObject is null");
            }
            return result;
        } catch (JSONException e) {
            throw new StorageException("Exception getting JSONObject", e);
        }

    }

    /**
     * Get the APPs associated with this device
     * @return JSONObject of apps
     * @throws StorageException
     */
    public JSONObject getApps() throws StorageException {
        return getJsonObject(this.data, APPS);
    }

    /**
     * Check if an app exists
     * @param keyId keyID to check under
     * @param appId appID to check
     * @return true if it does, false it not
     * @throws StorageException
     */
    public boolean appExists(String keyId, String appId) throws StorageException {
        JSONObject apps = getApps();
        if (apps.has(keyId)) {
            JSONObject pcApps = getJsonObject(apps, keyId);
            return pcApps.has(appId);
        }

        return false;
    }

    /**
     * Get the app type
     * @param keyId key ID the appID is under
     * @param appId appID to check
     * @return app type
     * @throws StorageException
     */
    public String getAppType(String keyId, String appId) throws StorageException {
        try {
            JSONObject apps = getApps();
            if (apps.has(keyId)) {
                JSONObject pcApps = getJsonObject(apps, keyId);
                String result = pcApps.getString(appId);

                if (result == null) {
                    throw new StorageException("AppID is missing");
                }
                return result;
            }
        } catch (JSONException e) {
            throw new StorageException("JSONException checking app type", e);
        }
        throw new StorageException("Missing keyId or appId");
    }

    /**
     * Get the apps associated with a particular KeyID
     * @param keyId key ID to get
     * @return JSONObject of associated apps
     * @throws StorageException
     */
    public JSONObject getKeyApps(String keyId) throws StorageException {
        JSONObject apps = getApps();
        return getJsonObject(apps, keyId);
    }

    /**
     * Add an appID to the store
     * @param keyId KeyID to store the app under
     * @param appId AppID to store
     * @param type type of the app
     * @throws StorageException
     */
    public void addApp(String keyId, String appId, String type) throws StorageException {

        try {
            JSONObject apps = getApps();
            if (!apps.has(keyId)) {
                apps.put(keyId, new JSONObject());
            }
            JSONObject keysApps = getKeyApps(keyId);
            if (keysApps.has(appId)) {
                throw new StorageException("App already exists");
            }
            keysApps.put(appId, type);
            this.store();
        } catch (JSONException e) {
            throw new StorageException("Exception adding new app", e);
        }
    }

    /**
     * Initializes and a component given the application {@link Context}
     *
     * @param context The application context.
     */
    @NonNull
    @Override
    public IdentityStore create(@NonNull Context context) {
        IdentityStore idStore = IdentityStore.getInstance();
        try {
            idStore.init(context.getFilesDir());
        } catch (StorageException e) {
            Log.d(TAG, "Exception initialising IdentityStore");
        }
        return idStore;

    }

    /**
     * @return A list of dependencies that this {@link Initializer} depends on. This is
     * used to determine initialization order of {@link Initializer}s.
     * <br/>
     * For e.g. if a {@link Initializer} `B` defines another
     * {@link Initializer} `A` as its dependency, then `A` gets initialized before `B`.
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return new ArrayList<>();
    }

    /**
     * Cleanup an unused app after optimistically creating it during key generation, i.e. when a
     * user rejects an app request
     * @param keyId keyID to app is under
     * @param appId AppID to remove
     * @throws StorageException
     */
    public void cleanUpUnusedApp(String keyId, String appId) throws StorageException {
        JSONObject apps = getApps();
        if (!apps.has(keyId)) {
            return;
        }
        JSONObject keysApps = getKeyApps(keyId);
        if (keysApps.has(appId)) {
            keysApps.remove(appId);
        }
        this.store();

    }

    /**
     * Reset the store deleting all data
     * @throws StorageException
     */
    public void reset() throws StorageException {
        resetStore();
    }
}
