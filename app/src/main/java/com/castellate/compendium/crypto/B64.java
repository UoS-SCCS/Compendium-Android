package com.castellate.compendium.crypto;

import android.util.Base64;

public class B64 {
    public static final byte[] decode(String value) {
        //We have to have this horrible hack because the JSON library included with android escapes
        //forward slashes and therefore breaks Base64 strings contained within a JSONObject
        //¯\_(ツ)_/¯.
        //Log.d("B64","Received: " + value);
        //value = value.replaceAll("\\\\", "");
        //Log.d("B64","Escaped: " + value);
        return Base64.decode(value, Base64.NO_WRAP);
    }

    public static final String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }
}
