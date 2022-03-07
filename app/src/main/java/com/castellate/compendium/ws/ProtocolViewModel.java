package com.castellate.compendium.ws;

import androidx.lifecycle.MutableLiveData;

import com.castellate.compendium.protocol.Protocol;

public interface ProtocolViewModel {
    public void postProtocolStatus(Protocol.STATUS status);
    public MutableLiveData<Protocol.STATUS> getProtocolStatus();

    public void postProtocolState(String state);
    public MutableLiveData<String> getProtocolState();
}
