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

package com.castellate.compendium.ws;

import com.google.common.collect.ImmutableMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class WSMessages {
    public static final String MSG_TYPE = "type";
    public static final Map<String, String[]> TYPE_FIELDS = ImmutableMap.of(MsgTypes.INIT, InitMsg.ALL_FIELDS, MsgTypes.INITRESP, InitRespMsg.ALL_FIELDS, MsgTypes.DELIVER, DeliverMsg.ALL_FIELDS, MsgTypes.ROUTE, RouteMsg.ALL_FIELDS, MsgTypes.ERROR, ErrorMsg.ALL_FIELDS);

    public static JSONObject parse(String message){

        try {
            JSONObject msg = new JSONObject(message);
            if(!msg.has(MSG_TYPE) || !TYPE_FIELDS.containsKey(msg.getString(MSG_TYPE))){
                return null;
            }
            if(!WSMessages.validate(msg,TYPE_FIELDS.get(msg.getString(MSG_TYPE)))){
                return null;
            }
            return msg;

        } catch (JSONException e) {
            return null;
        }
    }
    public static JSONObject createInitMsg(){
        try {
            JSONObject msg = new JSONObject();
            msg.put(MSG_TYPE, MsgTypes.INIT);
            return msg;
        }catch(JSONException e){
            return new JSONObject();
        }
    }
    public static JSONObject createRoute(String adr, JSONObject content){
        try {
            JSONObject msg = new JSONObject();
            msg.put(MSG_TYPE, MsgTypes.ROUTE);
            msg.put(RouteMsg.ADR, adr);
            msg.put(RouteMsg.MSG, content);
            return msg;
        }catch(JSONException e){
            return new JSONObject();
        }
    }

    private static boolean validate(JSONObject msg, String[] fields){
        if(fields ==null){
            return false;
        }
        Set<String> allFields = new HashSet<>();
        for(String field :fields){
            if(!msg.has(field)){
                return false;
            }
            allFields.add(field);
        }
        Iterator<String> itr = msg.keys();
        while(itr.hasNext()){
            if(!allFields.contains(itr.next())){
                return false;
            }
        }
        return true;
    }



    public static final class MsgTypes {

        public static final String INIT = "INIT";
        public static final String INITRESP = "INITRESP";
        public static final String ROUTE = "ROUTE";
        public static final String DELIVER = "DELIVER";
        public static final String ERROR = "ERR";
        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE, INIT, INITRESP, ROUTE, DELIVER, ERROR};

        private MsgTypes() {
            // restrict instantiation
        }

    }

    public static final class InitMsg {

        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE};

        private InitMsg() {
            // restrict instantiation
        }
    }

    public static final class InitRespMsg {

        public static final String ADR = "EpheWssAddr";
        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE, ADR};
        private InitRespMsg() {
            // restrict instantiation
        }
    }

    public static final class RouteMsg {

        public static final String ADR = "EpheWssAddr";
        public static final String MSG = "msg";
        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE, ADR, MSG};
        private RouteMsg() {
            // restrict instantiation
        }
    }

    public static final class DeliverMsg {

        public static final String MSG = "msg";
        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE, MSG};
        private DeliverMsg() {
            // restrict instantiation
        }
    }

    public static final class ErrorMsg {

        public static final String CODE = "errCode";
        public static final String MSG = "msg";
        public static final String[] ALL_FIELDS = new String[]{MSG_TYPE, MSG};
        private ErrorMsg() {
            // restrict instantiation
        }
    }


}
