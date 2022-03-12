package com.castellate.compendium.crypto;

import android.util.Base64;

public class B64 {
    public static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    public static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }
}
