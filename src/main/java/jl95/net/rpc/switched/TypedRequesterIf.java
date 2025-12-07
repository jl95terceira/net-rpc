package jl95.net.rpc.switched;

import static jl95.lang.SuperPowers.self;

import jl95.lang.variadic.Function1;
import jl95.net.rpc.RequesterIf;

public interface TypedRequesterIf<A, R> {

    RequesterIf<A, R> getFunction(String typeAlias);

    default <A2, R2> TypedRequesterIf<A2, R2> adapted        (Function1<A, A2> requestAdapter,
                                                              Function1<R2, R> responderAdapter) {
        return typeAlias -> getFunction(typeAlias).adapted(requestAdapter, responderAdapter);
    }
    default <A2>     TypedRequesterIf<A2, R>  adaptedRequest (Function1<A, A2> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <R2>     TypedRequesterIf<A, R2>  adaptedResponse(Function1<R2, R> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
