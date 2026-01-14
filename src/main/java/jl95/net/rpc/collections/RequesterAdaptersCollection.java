package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.Requester;
import jl95.net.rpc.util.SerdesDefaults;

public class RequesterAdaptersCollection {

    private RequesterAdaptersCollection() {}

    public static Requester<byte[]   , Void     > asPostRequester      (Requester<byte[], byte[]> requester) {

        return requester.adapted(
            self::apply,
            x -> null
        );
    }
    public static Requester<Void     , byte[]   > asGetRequester       (Requester<byte[], byte[]> requester) {

        return requester.adapted(
            x -> new byte[0],
            self::apply
        );
    }
    public static Requester<String   , String   > asStringRequester    (Requester<byte[], byte[]> requester) {

        return requester.adapted(
            SerdesDefaults.stringToBytes,
            SerdesDefaults.stringFromBytes
        );
    }
    public static Requester<String   , Void     > asStringPostRequester(Requester<byte[], byte[]> requester) {

        return asPostRequester(requester).adapted(
            SerdesDefaults.stringToBytes,
            x -> x
        );
    }
    public static Requester<Void     , String   > asStringGetRequester (Requester<byte[], byte[]> requester) {

        return asGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.stringFromBytes
        );
    }
    public static Requester<JsonValue, JsonValue> asJsonRequester      (Requester<byte[], byte[]> requester) {

        return asStringRequester(requester).adapted(
            SerdesDefaults.jsonToString,
            SerdesDefaults.jsonFromString
        );
    }
    public static Requester<JsonValue, Void     > asJsonPostRequester  (Requester<byte[], byte[]> requester) {

        return asStringPostRequester(requester).adapted(
            SerdesDefaults.jsonToString,
            x -> x
        );
    }
    public static Requester<Void     , JsonValue> asJsonGetRequester   (Requester<byte[], byte[]> requester) {

        return asStringGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.jsonFromString
        );
    }
}
