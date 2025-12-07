package jl95.net.rpc.util;

import static jl95.lang.SuperPowers.*;

import javax.json.JsonValue;

import jl95.lang.*;
import jl95.lang.variadic.*;
import jl95.util.*;

public class TypedPayload extends NamedDataClass {

    public String typeAlias;
    public JsonValue payload;

    public TypedPayload(String typeAlias, JsonValue object) {
        this.typeAlias = typeAlias;
        this.payload = object;
    }
    public TypedPayload() {this(null, null);}

    @Override
    protected Iterable<Tuple2<String, ?>> namedData() {
        return I(
            tuple("typeAlias", typeAlias),
            tuple("object", payload)
        );
    }
}
