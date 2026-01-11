package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.RequesterIf;
import jl95.net.rpc.util.SerdesDefaults;

public class RequesterAdaptersCollection {

    public static RequesterIf     <byte[]   , Void     > asPostRequester         (RequesterIf     <byte[], byte[]> requester) {

        return requester.adapted(
            self::apply,
            x -> null
        );
    }
    public static RequesterIf     <Void     , byte[]   > asGetRequester          (RequesterIf     <byte[], byte[]> requester) {

        return requester.adapted(
            x -> new byte[0],
            self::apply
        );
    }
    public static RequesterIf     <String   , String   > asStringPostGetRequester(RequesterIf     <byte[], byte[]> requester) {

        return requester.adapted(
            SerdesDefaults.stringToBytes,
            SerdesDefaults.stringFromBytes
        );
    }
    public static RequesterIf     <String   , Void     > asStringPostRequester   (RequesterIf     <byte[], byte[]> requester) {

        return asPostRequester(requester).adapted(
            SerdesDefaults.stringToBytes,
            x -> x
        );
    }
    public static RequesterIf     <Void     , String   > asStringGetRequester    (RequesterIf     <byte[], byte[]> requester) {

        return asGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.stringFromBytes
        );
    }
    public static RequesterIf     <JsonValue, JsonValue> asJsonPostGetRequester  (RequesterIf     <byte[], byte[]> requester) {

        return asStringPostGetRequester(requester).adapted(
            SerdesDefaults.jsonToString,
            SerdesDefaults.jsonFromString
        );
    }
    public static RequesterIf     <JsonValue, Void     > asJsonPostRequester     (RequesterIf     <byte[], byte[]> requester) {

        return asStringPostRequester(requester).adapted(
            SerdesDefaults.jsonToString,
            x -> x
        );
    }
    public static RequesterIf     <Void     , JsonValue> asJsonGetRequester      (RequesterIf     <byte[], byte[]> requester) {

        return asStringGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.jsonFromString
        );
    }
}
