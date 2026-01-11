package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.ResponderIf;
import jl95.net.rpc.util.SerdesDefaults;

public class ResponderAdaptersCollection {

    public static ResponderIf<byte[]   , Void     > asPostResponder         (ResponderIf<byte[], byte[]> responder) {

        return responder.adapted(
            self::apply,
            x -> new byte[0]
        );
    }
    public static ResponderIf<Void     , byte[]   > asGetResponder          (ResponderIf<byte[], byte[]> responder) {

        return responder.adapted(
            x -> null,
            self::apply
        );
    }
    public static ResponderIf<String   , String   > asStringPostGetResponder(ResponderIf<byte[], byte[]> responder) {

        return responder.adapted(
            SerdesDefaults.stringFromBytes,
            SerdesDefaults.stringToBytes
        );
    }
    public static ResponderIf<String   , Void     > asStringPostResponder   (ResponderIf<byte[], byte[]> responder) {

        return asPostResponder(responder).adapted(
            SerdesDefaults.stringFromBytes,
            x -> x
        );
    }
    public static ResponderIf<Void     , String   > asStringGetResponder    (ResponderIf<byte[], byte[]> responder) {

        return asGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.stringToBytes
        );
    }
    public static ResponderIf<JsonValue, JsonValue> asJsonPostGetResponder  (ResponderIf<byte[], byte[]> responder) {

        return asStringPostGetResponder(responder).adapted(
            SerdesDefaults.jsonFromString,
            SerdesDefaults.jsonToString
        );
    }
    public static ResponderIf<JsonValue, Void     > asJsonPostResponder     (ResponderIf<byte[], byte[]> responder) {

        return asStringPostResponder(responder).adapted(
            SerdesDefaults.jsonFromString,
            x -> x
        );
    }
    public static ResponderIf<Void     , JsonValue> asJsonGetResponder      (ResponderIf<byte[], byte[]> responder) {

        return asStringGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.jsonToString
        );
    }
}
