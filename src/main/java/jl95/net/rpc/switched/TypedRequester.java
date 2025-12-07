package jl95.net.rpc.switched;

import javax.json.JsonValue;

import jl95.net.io.managed.ManagedIos;
import jl95.net.rpc.Requester;
import jl95.net.rpc.RequesterIf;
import jl95.net.rpc.util.TypedPayload;
import jl95.net.rpc.util.serdes.TypedPayloadJsonSerdes;
import jl95.net.io.Ios;
import jl95.net.io.SenderReceiverIf;

public class TypedRequester implements TypedRequesterIf<JsonValue, JsonValue> {

    public static TypedRequester fromSimpleRpc(RequesterIf<JsonValue, JsonValue> requester) {

        return new TypedRequester(requester.adapted(
            TypedPayloadJsonSerdes::toJson,
            TypedPayloadJsonSerdes::fromJson
        ));
    }
    public static TypedRequester fromSr       (SenderReceiverIf<byte[], byte[]> sr) {
        return fromSimpleRpc(Requester.fromSr(sr));
    }
    public static TypedRequester fromIo       (Ios ios) {

        return fromSr(SenderReceiverIf.fromIo(ios));
    }
    public static TypedRequester fromManagedIo(ManagedIos ios) {

        return fromSr(SenderReceiverIf.fromManagedIo(ios));
    }

    private final RequesterIf<TypedPayload, TypedPayload>
        requester;

    private TypedRequester(RequesterIf<TypedPayload, TypedPayload> requester) {this.requester = requester;}

    @Override
    public final RequesterIf<JsonValue, JsonValue> getFunction(String typeAlias) {

        return requester.adapted(a -> new TypedPayload(typeAlias, a),
                                 r -> r.payload);
    }
}
