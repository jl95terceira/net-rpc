package jl95.net.rpc.collections;

import static jl95.lang.SuperPowers.self;

import javax.json.JsonValue;

import jl95.net.rpc.ResponderIf;
import jl95.net.rpc.switched.TypeSwitchedResponderIf;
import jl95.net.rpc.util.SerdesDefaults;

public class ResponderAdaptersCollection {

    public static ResponderIf            <JsonValue,    Void>   asPostResponder         (ResponderIf            <JsonValue, JsonValue> responder) {

        return responder.adapted(
            self::apply,
            x -> JsonValue.NULL
        );
    }
    public static TypeSwitchedResponderIf<JsonValue,    Void>   asPostResponder         (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return responder.adapted(
            self::apply,
            x -> JsonValue.NULL
        );
    }
    public static ResponderIf            <Void,      JsonValue> asGetResponder          (ResponderIf            <JsonValue, JsonValue> responder) {

        return responder.adapted(
            x -> null,
            self::apply
        );
    }
    public static TypeSwitchedResponderIf<Void,      JsonValue> asGetResponder          (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return responder.adapted(
            x -> null,
            self::apply
        );
    }
    public static ResponderIf            <String,    String>    asStringPostGetResponder(ResponderIf            <JsonValue, JsonValue> responder) {

        return responder.adapted(
            SerdesDefaults.stringFromJson,
            SerdesDefaults.stringToJson
        );
    }
    public static TypeSwitchedResponderIf<String,    String>    asStringPostGetResponder(TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return responder.adapted(
            SerdesDefaults.stringFromJson,
            SerdesDefaults.stringToJson
        );
    }
    public static ResponderIf            <String,    Void>      asStringPostResponder   (ResponderIf            <JsonValue, JsonValue> responder) {

        return asPostResponder(responder).adapted(
            SerdesDefaults.stringFromJson,
            x -> x
        );
    }
    public static TypeSwitchedResponderIf<String,    Void>      asStringPostResponder   (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return asPostResponder(responder).adapted(
            SerdesDefaults.stringFromJson,
            x -> x
        );
    }
    public static ResponderIf            <Void,      String>    asStringGetResponder    (ResponderIf            <JsonValue, JsonValue> responder) {

        return asGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.stringToJson
        );
    }
    public static TypeSwitchedResponderIf<Void,      String>    asStringGetResponder    (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return asGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.stringToJson
        );
    }
    public static ResponderIf            <byte[], byte[]>       asBytesPostGetResponder (ResponderIf            <JsonValue, JsonValue> responder) {

        return asStringPostGetResponder(responder).adapted(
            SerdesDefaults.bytesFromString,
            SerdesDefaults.bytesToString
        );
    }
    public static TypeSwitchedResponderIf<byte[], byte[]>       asBytesPostGetResponder (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return asStringPostGetResponder(responder).adapted(
            SerdesDefaults.bytesFromString,
            SerdesDefaults.bytesToString
        );
    }
    public static ResponderIf            <byte[], Void>         asBytesPostResponder    (ResponderIf            <JsonValue, JsonValue> responder) {

        return asStringPostResponder(responder).adapted(
            SerdesDefaults.bytesFromString,
            x -> x
        );
    }
    public static TypeSwitchedResponderIf<byte[], Void>         asBytesPostResponder    (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return asStringPostResponder(responder).adapted(
            SerdesDefaults.bytesFromString,
            x -> x
        );
    }
    public static ResponderIf            <Void, byte[]>         asBytesGetResponder     (ResponderIf            <JsonValue, JsonValue> responder) {

        return asStringGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.bytesToString
        );
    }
    public static TypeSwitchedResponderIf<Void, byte[]>         asBytesGetResponder     (TypeSwitchedResponderIf<JsonValue, JsonValue> responder) {

        return asStringGetResponder(responder).adapted(
            x -> x,
            SerdesDefaults.bytesToString
        );
    }
}
