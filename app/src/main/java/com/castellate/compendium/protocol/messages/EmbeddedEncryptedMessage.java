package com.castellate.compendium.protocol.messages;

public interface EmbeddedEncryptedMessage {
    String getEncryptedMsgField();
    Class<?> getEncryptedMessageClass();
}
