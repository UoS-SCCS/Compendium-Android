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

package com.castellate.compendium.ui.request;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolViewModel;

public class RequestViewModel extends ViewModel implements ProtocolViewModel {
    private final MutableLiveData<Protocol.STATUS> protocolStatus = new MutableLiveData<>();
    private final MutableLiveData<String> protocolState = new MutableLiveData<>();

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
