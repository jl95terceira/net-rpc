package jl95.net.rpc.util.serdes;

import static jl95.lang.SuperPowers.*;

import javax.json.Json;
import javax.json.JsonValue;

import jl95.net.rpc.util.TypedPayload;
import jl95.net.rpc.util.SerdesDefaults;

public class TypedPayloadJsonSerdes {

    enum Id {
        TYPE_ALIAS("typeAlias"),
        OBJECT    ("object");
        final String value;
        Id(String value) {this.value = value;}
    }

    public static JsonValue    toJson  (TypedPayload tp) {
        var job = Json.createObjectBuilder();
        for (var t: I(
            tuple(Id.TYPE_ALIAS, method((String i) -> job.add(i, tp.typeAlias))),
            tuple(Id.OBJECT    , method((String i) -> job.add(i, tp.payload)))
        )) {
            t.a2.accept(t.a1.value);
        }
        return job.build();
    }
    public static TypedPayload fromJson(JsonValue    json) {
        var jo = json.asJsonObject();
        var typedObject = new TypedPayload();
        for (var t: I(
            tuple(Id.TYPE_ALIAS, method((String i) -> { typedObject.typeAlias = jo.getString(i); })),
            tuple(Id.OBJECT    , method((String i) -> { typedObject.payload   = jo.get(i); }))
        )) {
            t.a2.accept(t.a1.value);
        }
        return typedObject;
    }
}
