package jl95.net.rpc.switched;

import static jl95.lang.SuperPowers.tuple;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonValue;

import jl95.lang.variadic.*;
import jl95.net.rpc.Responder;
import jl95.net.rpc.ResponderIf;
import jl95.net.rpc.util.TypedPayload;
import jl95.net.rpc.util.serdes.TypedPayloadJsonSerdes;
import jl95.net.io.Ios;
import jl95.net.io.SenderReceiverIf;
import jl95.util.*;

public class TypeSwitchedResponder implements TypeSwitchedResponderIf<JsonValue, JsonValue> {

    public static class DefaultNotSetException extends RuntimeException {}

    public static TypeSwitchedResponder fromSimpleRpc(ResponderIf<JsonValue, JsonValue> responder) {
        return new TypeSwitchedResponder(responder.adapted(
            TypedPayloadJsonSerdes::fromJson,
            TypedPayloadJsonSerdes::toJson
        ));
    }
    public static TypeSwitchedResponder fromSr       (SenderReceiverIf<byte[], byte[]> sr) {
        return fromSimpleRpc(Responder.fromSr(sr));
    }
    public static TypeSwitchedResponder fromIo       (Ios ios) {
        return fromSr(SenderReceiverIf.fromIo(ios));
    }

    private final ResponderIf<TypedPayload, TypedPayload>
        responder;
    private final Map<String, Function1<Tuple2<TypedPayload, Boolean>, TypedPayload>>
        callbacksCases   = new HashMap<>();
    private       Function1<Tuple2<TypedPayload, Boolean>, TypedPayload>
        callbacksDefault = payload -> { throw new DefaultNotSetException(); };

    private TypeSwitchedResponder(ResponderIf<TypedPayload, TypedPayload> responder) {this.responder = responder;}

    private Function1<Tuple2<TypedPayload, Boolean>, TypedPayload> makeCallback(Function1<Tuple2<JsonValue, Boolean>, JsonValue> responseFunction) {
        return tp -> {
            var r = responseFunction.apply(tp.payload);
            return tuple(new TypedPayload(tp.typeAlias, r.a1), r.a2);
        };
    }

    @Override
    public final void            addCaseWhile(String typeAlias,
                                              Function1<Tuple2<JsonValue, Boolean>, JsonValue> responseFunction) {
        callbacksCases.put(typeAlias, makeCallback(responseFunction));
    }
    @Override
    public final void            setDefaultCase(Function1<Tuple2<JsonValue, Boolean>, JsonValue> responseFunction) {
        callbacksDefault = makeCallback(responseFunction);
    }
    @Override
    public final void            removeCase(String typeAlias) {

        callbacksCases.remove(typeAlias);
    }
    @Override
    public final UVoidFuture   start     () {

        return responder.respondWhile(tp -> {
            var callback = callbacksCases.getOrDefault(tp.typeAlias, callbacksDefault);
            return callback.apply(tp);
        });
    }
    @Override
    public final UVoidFuture   stop      () {

        return responder.stop();
    }
    @Override
    public final Boolean         isRunning () {return responder.isRunning();}
    @Override
    public final ResponderIf<TypedPayload, TypedPayload> getBaseResponder() {return responder;}
}

