package com.castellate.compendium.protocol;

import androidx.lifecycle.MutableLiveData;

public interface ProtocolViewModel {
    void postProtocolStatus(Protocol.STATUS status);
    MutableLiveData<Protocol.STATUS> getProtocolStatus();

    void postProtocolState(String state);
    MutableLiveData<String> getProtocolState();
}
