package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.tuple;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Tuple2;
import jl95.net.io.Receiver;
import jl95.util.*;

public interface Responder<A, R> {

    interface RespondOptions extends Receiver.RecvOptions {

        class Editable extends Receiver.RecvOptions.Editable implements RespondOptions {}
        static RespondOptions defaults() { return new Editable(); }
    }

    void        respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction,
                             RespondOptions options);
    UVoidFuture waitRespondingStarted();
    UVoidFuture stop        ();
    UVoidFuture waitRespondingStopped();
    boolean     isRunning   ();

    default void        respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction) {

        respondWhile(responseFunction, RespondOptions.defaults());
    }
    default void        respond     (Function1<R, A> responseFunction,
                                     RespondOptions options) {
        respondWhile(request -> tuple(responseFunction.apply(request), true), options);
    }
    default void        respond     (Function1<R, A> responseFunction) {
        respond(responseFunction, RespondOptions.defaults());
    }
    default void        respondOnce (Function1<R, A> responseFunction,
                                     RespondOptions options) {
        respondWhile(request -> tuple(responseFunction.apply(request), false), options);
    }
    default void        respondOnce (Function1<R, A> responseFunction) {
        respondOnce(responseFunction, RespondOptions.defaults());
    }
    default void        ensureStopped() {
        try {
            stop().get();
        }
        catch (IllegalStateException ex) {/* no problem */}
    }
    default <A2, R2> Responder<A2, R2> adapted(Function1<A2, A> requestAdapter,
                                               Function1<R, R2> responseAdapter) {
        return new Responder<>() {

            @Override public void        respondWhile(Function1<Tuple2<R2, Boolean>, A2> responseFunction,
                                                      RespondOptions                     options) {
                Responder.this.respondWhile(request -> {
                    var adaptedRequest = requestAdapter.apply(request);
                    var response = responseFunction.apply(adaptedRequest);
                    return tuple(responseAdapter.apply(response.a1), response.a2);
                }, options);
            }
            @Override public UVoidFuture waitRespondingStarted() {
                return Responder.this.waitRespondingStarted();
            }
            @Override public UVoidFuture stop        () {
                return Responder.this.stop();
            }
            @Override public UVoidFuture waitRespondingStopped() {
                return Responder.this.waitRespondingStopped();
            }
            @Override public boolean     isRunning   () {
                return Responder.this.isRunning();
            }
        };
    }
    default <A2> Responder<A2, R> adaptedRequest (Function1<A2, A> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <R2> Responder<A, R2> adaptedResponse(Function1<R, R2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
