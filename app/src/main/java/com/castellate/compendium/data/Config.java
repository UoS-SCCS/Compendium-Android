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

public final class Config implements Initializer<Config> {
    private static final String TAG = "Config";
    public static final String WSS_REGISTER="wss_register_url";
    private static final Config _instance = new Config();
    private volatile boolean isInitialised = false;
    private final Properties properties = new Properties();

    private Config() {

    }

    public static Config getInstance() {
        return _instance;
   }

    public boolean isInitialised() {
        return this.isInitialised;
    }

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

    public synchronized void initialise(Context context) throws StorageException {
        Resources resources = context.getResources();
        try {
            InputStream rawResource = resources.openRawResource(R.raw.config);
            properties.load(rawResource);
            this.isInitialised = true;
            Log.d(TAG,"Config file successfully loaded");
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
            Log.e(TAG,"Exception initialising config",e);
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

