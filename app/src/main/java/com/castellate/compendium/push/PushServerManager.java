package com.castellate.compendium.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyInfo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.data.Config;
import com.castellate.compendium.data.Prefs;
import com.castellate.compendium.exceptions.CompendiumException;
import com.castellate.compendium.exceptions.StorageException;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class PushServerManager {
    private static final String TAG = "PushServerManager";
    private static final String REQ_FIREBASE_ID="fb_id";
    private static final String REQ_PUBLIC_KEY = "pub_key";
    private static final String RES_SUCCESS = "success";
    public PushServerManager() {
    }

    public static void checkRegistered(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Prefs.APP_SETTINGS, Context.MODE_PRIVATE);
        boolean registered = prefs.getBoolean("registered", false);
        Log.d(TAG, "Registered:" + registered);
        if (!registered) {
            getFirebaseToken(context);
        }

    }

    private static void getFirebaseToken(Context context) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            // Get new FCM registration token
            String token = task.getResult();
            try {
                PushServerManager.sendTokenToServer(token, context);
            } catch (CompendiumException e) {
                //TODO handle this better
                Log.d(TAG,"Exception sending token to server",e);

            }
        });
    }

    public static void sendTokenToServer(String token, Context context) throws CryptoException, StorageException {
        // creating a new variable for our request queue
        try {
            CompanionKeyManager ckm = new CompanionKeyManager();
            KeyPair kp = ckm.getOrCreateIdentityKey();
            KeyFactory factory;
            KeyInfo keyInfo;

            factory = KeyFactory.getInstance(kp.getPrivate().getAlgorithm(), CompanionKeyManager.KEYSTORE);
            keyInfo = factory.getKeySpec(kp.getPrivate(), KeyInfo.class);
            Log.d(TAG, "SecureHardware:" + keyInfo.isInsideSecureHardware());

            String pubKey = B64.encode(kp.getPublic().getEncoded());
            RequestQueue queue = Volley.newRequestQueue(context);
            String url = Config.getInstance().get(Config.WSS_REGISTER);
            JSONObject data = new JSONObject();
            data.put(REQ_FIREBASE_ID, token);
            data.put(REQ_PUBLIC_KEY, pubKey);


            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, response -> {
                try {

                    if (response.optBoolean(RES_SUCCESS, false)) {
                        SharedPreferences prefs = context.getSharedPreferences(Prefs.APP_SETTINGS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor myEdit = prefs.edit();
                        myEdit.putBoolean(Prefs.REGISTERED, true);
                        myEdit.apply();

                    }
                    Log.d(TAG, "RegisterResult: " + response.getBoolean(RES_SUCCESS));
                } catch (JSONException e) {
                    //TODO we need to better handle this
                    Log.d(TAG,"JSON Exception in result",e);

                }
            }, error -> {
                //TODO we need to better handle this
                Log.e(TAG, error.getMessage());
            });
            // below line is to make
            // a json object request.
            queue.add(request);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | JSONException e) {
            throw new CryptoException("Exception sending token to server", e);
        }
    }
}
