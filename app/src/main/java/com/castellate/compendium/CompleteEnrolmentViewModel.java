package com.castellate.compendium;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.ws.ProtocolViewModel;

public class CompleteEnrolmentViewModel extends ViewModel implements ProtocolViewModel {
    private final MutableLiveData<Protocol.STATUS> protocolStatus = new MutableLiveData<Protocol.STATUS>();
    private final MutableLiveData<String> protocolState = new MutableLiveData<String>();

    public void setProtocolStatus(Protocol.STATUS status){
        protocolStatus.setValue(status);
    }
    public MutableLiveData<Protocol.STATUS> getProtocolStatus() {
        return protocolStatus;
    }
    public void postProtocolStatus(Protocol.STATUS status){
        protocolStatus.postValue(status);
    }
    public void setProtocolState(String state){
        protocolState.setValue(state);
    }
    public void postProtocolState(String state){
        protocolState.postValue(state);
    }
    public MutableLiveData<String> getProtocolState() {
        return protocolState;
    }
}
