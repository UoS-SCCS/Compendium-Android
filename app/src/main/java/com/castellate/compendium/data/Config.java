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
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.castellate.compendium.R;
import com.castellate.compendium.exceptions.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Config class singleton that is initialised using the Initialize pattern
 */
public final class Config implements Initializer<Config> {
    public static final String WSS_REGISTER = "wss_register_url";
    public static final String WSS_SERVER = "wss_url";
    private static final String TAG = "Config";
    private static final Config _instance = new Config();
    private final Properties properties = new Properties();
    private volatile boolean isInitialised = false;

    /**
     * Protected constructor
     */
    private Config() {

    }

    /**
     * Get the config instance
     * @return
     */
    public static Config getInstance() {
        return _instance;
    }

    /**
     * Is the config object initialised
     * @return
     */
    public boolean isInitialised() {
        return this.isInitialised;
    }

    /**
     * Gets a string value from the config
     * @param name field to get
     * @return value of field
     * @throws StorageException if the property doesn't exist or config is not initialised
     */
    public String get(String name) throws StorageException {
        if (!isInitialised()) {
            throw new StorageException("Config is uninitialised");
        }
        String ret = properties.getProperty(name);
        if (ret == null) {
            throw new StorageException("Missing configuration value");
        }
        return ret;
    }

    /**
     * Initialise the config file reading the data from a properties file
     * @param context context to use for reading the file
     * @throws StorageException
     */
    public synchronized void initialise(Context context) throws StorageException {
        Resources resources = context.getResources();
        try {
            InputStream rawResource = resources.openRawResource(R.raw.config);
            properties.load(rawResource);
            this.isInitialised = true;
            Log.d(TAG, "Config file successfully loaded");
        } catch (Resources.NotFoundException | IOException e) {
            throw new StorageException("Exception loading config", e);
        }
    }


    /**
     * Initializes and a component given the application {@link Context}
     *
     * @param context The application context.
     */
    @NonNull
    @Override
    public Config create(@NonNull Context context) {
        Config config = Config.getInstance();
        try {
            config.initialise(context);
        } catch (StorageException e) {
            Log.e(TAG, "Exception initialising config", e);
        }
        return config;
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
}

