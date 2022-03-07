package com.castellate.compendium.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    public PushServerManager(Context context) {


    }
    public static void checkRegistered(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", context.MODE_PRIVATE);
        boolean registered = prefs.getBoolean("registered", false);
        Log.d(TAG, "Registered:" + registered);
        if (!registered) {
            getFirebaseToken(context);
        }

    }
    private static void getFirebaseToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();
                    PushServerManager.sendTokenToServer(token, context);
                }
            });
    }

    public static void sendTokenToServer(String token, Context context) {
        // creating a new variable for our request queue
        CompanionKeyManager ckm = new CompanionKeyManager();
        KeyPair kp = ckm.getOrCreateIdentityKey();
        KeyFactory factory = null;
        KeyInfo keyInfo;
        try {
            factory = KeyFactory.getInstance(kp.getPrivate().getAlgorithm(), "AndroidKeyStore");
            keyInfo = factory.getKeySpec(kp.getPrivate(), KeyInfo.class);
            Log.d(TAG,"SecureHardware:" + keyInfo.isInsideSecureHardware());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        String pubKey = B64.encode(kp.getPublic().getEncoded());
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://10.0.2.2:5000/register";
        // on below line we are calling a string
        // request method to post the data to our API
        // in this we are calling a post method.
        JSONObject data = new JSONObject();
        try {
            data.put("fb_id", token);
            data.put("pub_key", pubKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    if (response.optBoolean("success", false)) {
                        SharedPreferences prefs = context.getSharedPreferences("AppSettings", context.MODE_PRIVATE);
                        SharedPreferences.Editor myEdit = prefs.edit();
                        myEdit.putBoolean("registered", true);
                        myEdit.commit();

                    }
                    Log.d(TAG, "RegisterResult: " + response.getBoolean("success"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.getMessage());
            }
        });
        // below line is to make
        // a json object request.
        queue.add(request);
    }
}
