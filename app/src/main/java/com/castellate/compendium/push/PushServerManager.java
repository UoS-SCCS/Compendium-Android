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

/**
 * Push server manager that manages interactions with the push server, like registering the
 * devices ID.
 *
 * This might expand in time to also register public key of approved senders
 */
public class PushServerManager {
    private static final String TAG = "PushServerManager";
    private static final String REQ_FIREBASE_ID="fb_id";
    private static final String REQ_PUBLIC_KEY = "pub_key";
    private static final String RES_SUCCESS = "success";
    public PushServerManager() {
    }

    /**
     * Checks if we are registered by checking the preferences file. This should be all that is
     * required because any change in ID will trigger the notification service and call an update.
     * However, this does not handle the situation of the resetting of the PushServer and loss of
     * the ID map. That shouldn't happen often but does happen during development. As such, at the
     * moment this always sends its token to the PushServer on start.
     *
     * TODO remove forced update call to PushServer once stable. Consider alternative way of checking push server is up-to-date
     *
     * @param context
     */
    public static void checkRegistered(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Prefs.APP_SETTINGS, Context.MODE_PRIVATE);
        boolean registered = prefs.getBoolean("registered", false);
        registered = false;
        Log.d(TAG, "Registered:" + registered);
        if (!registered) {
            getFirebaseToken(context);
        }

    }

    /**
     * Request the firebase ID token, then when it is received send it to the PushServer
     * @param context context to make the request in
     */
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

    /**
     * Performs the actual sending operation using Volley to construct and send a request.
     * @param token firebase ID
     * @param context context to run in
     * @throws CryptoException
     * @throws StorageException
     */
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
