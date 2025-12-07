package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.tuple;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Tuple2;
import jl95.net.io.ReceiverIf;
import jl95.util.*;

public interface ResponderIf<A, R> {

    interface RespondOptions extends ReceiverIf.RecvOptions {

        class Editable extends ReceiverIf.RecvOptions.Editable implements RespondOptions {}
        static RespondOptions defaults() { return new Editable(); }
    }

    VoidAwaitable respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction,
                                 RespondOptions                   options);
    VoidAwaitable stop        ();
    Boolean       isRunning   ();

    default VoidAwaitable respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction) {

        return respondWhile(responseFunction, RespondOptions.defaults());
    }
    default VoidAwaitable respond     (Function1<R, A> responseFunction,
                                         RespondOptions options) {
        return respondWhile(request -> tuple(responseFunction.apply(request), true), options);
    }
    default VoidAwaitable respond     (Function1<R, A> responseFunction) {
        return respond(responseFunction, RespondOptions.defaults());
    }
    default VoidAwaitable respondOnce (Function1<R, A> responseFunction,
                                         RespondOptions options) {
        return respondWhile(request -> tuple(responseFunction.apply(request), false), options);
    }
    default VoidAwaitable respondOnce (Function1<R, A> responseFunction) {
        return respondOnce(responseFunction, RespondOptions.defaults());
    }
    default void          ensureStopped() {
        try {
            stop().await();
        }
        catch (Responder.StopWhenNotRunningException ex) {
            return;
        }
    }
    default <A2, R2> ResponderIf<A2, R2> adapted        (Function1<A2, A> requestAdapter,
                                                         Function1<R, R2> responseAdapter) {
        return new ResponderIf<>() {

            @Override public VoidAwaitable respondWhile(Function1<Tuple2<R2, Boolean>, A2> responseFunction,
                                                          RespondOptions                     options) {
                return ResponderIf.this.respondWhile(request -> {
                    var adaptedRequest = requestAdapter.apply(request);
                    var response = responseFunction.apply(adaptedRequest);
                    return tuple(responseAdapter.apply(response.a1), response.a2);
                }, options);
            }
            @Override public VoidAwaitable stop        () {
                return ResponderIf.this.stop();
            }
            @Override public Boolean         isRunning   () {
                return ResponderIf.this.isRunning();
            }
        };
    }
    default <A2>     ResponderIf<A2, R>  adaptedRequest (Function1<A2, A> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <R2>     ResponderIf<A, R2>  adaptedResponse(Function1<R, R2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
