package jl95.net.rpc;

import static jl95.lang.SuperPowers.mapped;
import static jl95.lang.SuperPowers.self;

import java.io.InputStream;
import java.io.OutputStream;

import jl95.lang.variadic.*;
import jl95.util.UFuture;

public interface Requester<A, R> extends Function1<UFuture<R>,A> {

    default <A2, R2> Requester<A2, R2> adapted        (Function1<A, A2> requestAdapter,
                                                         Function1<R2, R> responseAdapter) {
        return requestObject -> {
            var adaptedRequestObject = requestAdapter.apply(requestObject);
            var responseObject = Requester.this.apply(adaptedRequestObject);
            return mapped(responseAdapter, responseObject);
        };
    }
    default <A2> Requester<A2, R> adaptedRequest (Function1<A, A2> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <R2> Requester<A, R2> adaptedResponse(Function1<R2, R> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
