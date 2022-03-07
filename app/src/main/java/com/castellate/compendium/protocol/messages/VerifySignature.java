package com.castellate.compendium.protocol.messages;

import java.util.Map;

public interface VerifySignature {
    public String getSignature();
    public String[] getSignatureFields();
    public String getPublicKey(Map<String,String> protocolData);
}
