package com.castellate.compendium.protocol.messages;

import java.util.Map;

public interface VerifySignature {
    String getSignature();
    String[] getSignatureFields();
    String getPublicKey(Map<String, String> protocolData) throws ProtocolMessageException;
}
