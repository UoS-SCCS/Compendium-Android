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

package com.castellate.compendium.protocol.messages;

/**
 * Constants used by protocols and classes references the protocols. Generally if the field
 * is defined here it is used outside of just the message class, for example, by a class accessing
 * the protocol data
 */
public class Constants {
    public static final String ADR_PC = "adr_pc";
    public static final String PC_PUBLIC_KEY = "pc_public_key";
    public static final String ADR_CD = "adr_cd";
    public static final String ID_CD = "id_cd";
    public static final String CD_PUBLIC_KEY = "cd_public_key";
    public static final String DERIVED_KEY = "derived_key";
    public static final String HASH_CD_PUBLIC_KEY = "hash_cd_public_key";
    public static final String HASH_PC_PUBLIC_KEY = "hash_pc_public_key";
    public static final String APP_ID = "app_id";

    public static final String TYPE_PUT_GET = "PUT_GET";
    public static final String TYPE_REG_SIGN = "REG_SIGN";
}
