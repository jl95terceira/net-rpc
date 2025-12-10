package jl95.net.rpc;

import static jl95.lang.SuperPowers.mapped;
import static jl95.lang.SuperPowers.self;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;

import jl95.lang.variadic.*;

public interface RequesterIf<A, R> {

    Future<R> apply(A requestObject);
    InputStream  getInputStream ();
    OutputStream getOutputStream();

    default <A2, R2> RequesterIf<A2, R2> adapted        (Function1<A, A2> requestAdapter,
                                                         Function1<R2, R> responseAdapter) {
        return new RequesterIf<>() {

            @Override public Future<R2>   apply          (A2 requestObject) {
                var adaptedRequestObject = requestAdapter.apply(requestObject);
                var responseObject = RequesterIf.this.apply(adaptedRequestObject);
                return mapped(responseAdapter, responseObject);
            }
            @Override public InputStream  getInputStream () {
                return RequesterIf.this.getInputStream();
            }
            @Override public OutputStream getOutputStream() {
                return RequesterIf.this.getOutputStream();
            }
        };
    }
    default <A2>     RequesterIf<A2, R>  adaptedRequest (Function1<A, A2> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <R2>     RequesterIf<A, R2>  adaptedResponse(Function1<R2, R> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
