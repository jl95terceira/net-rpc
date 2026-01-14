package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.Responder;
import jl95.net.rpc.util.SerdesDefaults;

public class ResponderAdaptersCollection {

    private ResponderAdaptersCollection() {}

    public static Responder<byte[]   , Void     > asPostResponder      (Responder<byte[], byte[]> responder) {

        return responder.adapted(
            self::apply,
            x -> new byte[0]
        );
    }
    public static Responder<Void     , byte[]   > asGetResponder       (Responder<byte[], byte[]> responder) {

        return responder.adapted(
            x -> null,
            self::apply
        );
    }
    public static Responder<String   , String   > asStringResponder    (Responder<byte[], byte[]> responder) {

        return responder.adapted(
            SerdesDefaults.stringFromBytes,
            SerdesDefaults.stringToBytes
        );
    }
    public static Responder<String   , Void     > asStringPostResponder(Responder<byte[], byte[]> responder) {

        return asPostResponder(responder).adapted(
            SerdesDefaults.stringFromBytes,
            x -> x
        );
    }
    public static Responder<Void     , String   > asStringGetResponder (Responder<byte[], byte[]> responder) {

        return asGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.stringToBytes
        );
    }
    public static Responder<JsonValue, JsonValue> asJsonResponder      (Responder<byte[], byte[]> responder) {

        return asStringResponder(responder).adapted(
            SerdesDefaults.jsonFromString,
            SerdesDefaults.jsonToString
        );
    }
    public static Responder<JsonValue, Void     > asJsonPostResponder  (Responder<byte[], byte[]> responder) {

        return asStringPostResponder(responder).adapted(
            SerdesDefaults.jsonFromString,
            x -> x
        );
    }
    public static Responder<Void     , JsonValue> asJsonGetResponder   (Responder<byte[], byte[]> responder) {

        return asStringGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.jsonToString
        );
    }
}
