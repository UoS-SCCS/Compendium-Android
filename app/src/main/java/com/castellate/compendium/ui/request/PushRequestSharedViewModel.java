package com.castellate.compendium.ui.request;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

public class PushRequestSharedViewModel extends ViewModel {
    private final MutableLiveData<JSONObject> receivedMessage = new MutableLiveData<>();

    public void setMessage(JSONObject msg){
        Log.d("VIEW MODEL", msg.toString());
        receivedMessage.setValue(msg);
    }

    public MutableLiveData<JSONObject> getMessage() {
        return receivedMessage;
    }
}
