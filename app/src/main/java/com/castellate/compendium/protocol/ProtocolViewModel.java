package com.castellate.compendium.protocol;

import androidx.lifecycle.MutableLiveData;

public interface ProtocolViewModel {
    public void postProtocolStatus(Protocol.STATUS status);
    public MutableLiveData<Protocol.STATUS> getProtocolStatus();

    public void postProtocolState(String state);
    public MutableLiveData<String> getProtocolState();
}
