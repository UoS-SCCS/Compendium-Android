package com.castellate.compendium.ui.enrol;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel  extends ViewModel {
    private final MutableLiveData<String> receivedMessage = new MutableLiveData<String>();

    public void setMessage(String msg){
        Log.d("VIEW MODEL", msg);
        receivedMessage.setValue(msg);
    }

    public MutableLiveData<String> getMessage() {
        return receivedMessage;
    }
}
