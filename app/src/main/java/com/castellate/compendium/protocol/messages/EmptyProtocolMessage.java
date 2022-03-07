package com.castellate.compendium.protocol.messages;

import com.castellate.compendium.protocol.enrol.InitKeyReqProtocolMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class EmptyProtocolMessage extends ProtocolMessage {

    public EmptyProtocolMessage(){
        super();
    }
    @Override
    public boolean parse(String data)  {
        try {
            return parse(new JSONObject((data)));
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public Class<?> getClassObj() {
        return InitKeyReqProtocolMessage.class;
    }

    @Override
    public String[] getAllFields() {
        return Fields.ALL_FIELDS;
    }

    @Override
    public boolean parse(JSONObject data) {
        this.validate(Fields.ALL_FIELDS);
        return false;
    }

    public static final class Fields {

        private Fields() {
            // restrict instantiation
        }

        public static final String[] ALL_FIELDS = new String[0];
    }
}