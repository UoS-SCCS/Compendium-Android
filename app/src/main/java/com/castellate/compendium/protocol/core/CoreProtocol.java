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

package com.castellate.compendium.protocol.core;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.core.req.CoreRequestProtocolMessage;
import com.castellate.compendium.protocol.core.res.CoreResponseProtocolMessage;
import com.castellate.compendium.protocol.enrol.InitWSSProtocolMessage;
import com.castellate.compendium.protocol.enrol.InitWSSRespProtocolMessage;
import com.castellate.compendium.protocol.messages.EmptyProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessage;
import com.castellate.compendium.protocol.messages.ProtocolMessageException;

public class CoreProtocol extends Protocol {

    public enum STATE {
        INIT_KEY_REQ,
        INIT_WSS,
        INIT_WSS_RESP,
        INIT_KEY_RESP,
        KEY_CONFIRM_REQ,
        EMPTY_DUMMY,
        CORE_REQ,
        CORE_RESP,
        FINISHED {
            @Override
            public STATE next() {
                return values()[0];
            }
        };
        public STATE next() {
            // No bounds checking required here, because the last instance overrides
            return values()[ordinal() + 1];
        }
    }
    private STATE state = STATE.INIT_KEY_REQ;
    private static final Class<ProtocolMessage>[] MESSAGES = new Class[]{CoreKeyReqProtocolMessage.class, InitWSSProtocolMessage.class, InitWSSRespProtocolMessage.class, CoreKeyRespProtocolMessage.class, ConfirmKeyProtocolMessage.class, EmptyProtocolMessage.class,CoreRequestProtocolMessage.class, CoreResponseProtocolMessage.class};
    public CoreProtocol(){
        super();
    }
    @Override
    public Class<? extends ProtocolMessage>[] getMessages() {
        return CoreProtocol.MESSAGES;
    }
    @Override
    public int getStateOrdinal() {
        return state.ordinal();
    }

    @Override
    public boolean isFinished() {
        return state == STATE.FINISHED;
    }

    @Override
    public String getProtocolStateString() {
        return state.name();
    }


    @Override
    public boolean processIncomingMessage(ProtocolMessage protoMessage) throws ProtocolMessageException {
        return protoMessage.processMessage(this.protocolData);
    }



    @Override
    public boolean advancedStateTriggerUI() {
        state=state.next();
        if(model!=null){
            model.postProtocolState(state.name());
        }
        return state == STATE.CORE_RESP || state == STATE.FINISHED;
    }

    @Override
    public int getTotalStates() {
        //We don't count the finished state
        return STATE.values().length-1;
    }


}
