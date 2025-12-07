package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.RequesterIf;
import jl95.net.rpc.switched.TypedRequesterIf;
import jl95.net.rpc.util.SerdesDefaults;

public class RequesterAdaptersCollection {

    public static RequesterIf     <JsonValue,    Void>  asPostRequester         (RequesterIf     <JsonValue, JsonValue> requester) {

        return requester.adapted(
            self::apply,
            x -> null
        );
    }
    public static TypedRequesterIf<JsonValue,    Void>  asPostRequester         (TypedRequesterIf<JsonValue, JsonValue> requester) {
        return requester.adapted(
            self::apply,
            x -> null
        );
    }
    public static RequesterIf     <Void,      JsonValue>asGetRequester          (RequesterIf     <JsonValue, JsonValue> requester) {

        return requester.adapted(
            x -> JsonValue.NULL,
            self::apply
        );
    }
    public static TypedRequesterIf<Void,      JsonValue>asGetRequester          (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return requester.adapted(
            x -> JsonValue.NULL,
            self::apply
        );
    }
    public static RequesterIf     <String,    String>   asStringPostGetRequester(RequesterIf     <JsonValue, JsonValue> requester) {

        return requester.adapted(
            SerdesDefaults.stringToJson,
            SerdesDefaults.stringFromJson
        );
    }
    public static TypedRequesterIf<String,    String>   asStringPostGetRequester(TypedRequesterIf<JsonValue, JsonValue> requester) {

        return requester.adapted(
            SerdesDefaults.stringToJson,
            SerdesDefaults.stringFromJson
        );
    }
    public static RequesterIf     <String,    Void>     asStringPostRequester   (RequesterIf     <JsonValue, JsonValue> requester) {

        return asPostRequester(requester).adapted(
            SerdesDefaults.stringToJson,
            x -> x
        );
    }
    public static TypedRequesterIf<String,    Void>     asStringPostRequester   (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return asPostRequester(requester).adapted(
            SerdesDefaults.stringToJson,
            x -> x
        );
    }
    public static RequesterIf     <Void,      String>   asStringGetRequester    (RequesterIf     <JsonValue, JsonValue> requester) {

        return asGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.stringFromJson
        );
    }
    public static TypedRequesterIf<Void,      String>   asStringGetRequester    (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return asGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.stringFromJson
        );
    }
    public static RequesterIf     <byte[], byte[]>      asBytesPostGetRequester (RequesterIf     <JsonValue, JsonValue> requester) {

        return asStringPostGetRequester(requester).adapted(
            SerdesDefaults.bytesToString,
            SerdesDefaults.bytesFromString
        );
    }
    public static TypedRequesterIf<byte[], byte[]>      asBytesPostGetRequester (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return asStringPostGetRequester(requester).adapted(
            SerdesDefaults.bytesToString,
            SerdesDefaults.bytesFromString
        );
    }
    public static RequesterIf     <byte[], Void>        asBytesPostRequester    (RequesterIf     <JsonValue, JsonValue> requester) {

        return asStringPostRequester(requester).adapted(
            SerdesDefaults.bytesToString,
            x -> x
        );
    }
    public static TypedRequesterIf<byte[], Void>        asBytesPostRequester    (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return asStringPostRequester(requester).adapted(
            SerdesDefaults.bytesToString,
            x -> x
        );
    }
    public static RequesterIf     <Void, byte[]>        asBytesGetRequester     (RequesterIf     <JsonValue, JsonValue> requester) {

        return asStringGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.bytesFromString
        );
    }
    public static TypedRequesterIf<Void, byte[]>        asBytesGetRequester     (TypedRequesterIf<JsonValue, JsonValue> requester) {

        return asStringGetRequester(requester).adapted(
            x -> x,
            SerdesDefaults.bytesFromString
        );
    }
}
