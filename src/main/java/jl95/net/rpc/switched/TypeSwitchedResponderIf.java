package jl95.net.rpc.switched;

import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.tuple;

import jl95.lang.*;
import jl95.lang.variadic.*;
import jl95.net.rpc.ResponderIf;
import jl95.net.rpc.util.TypedPayload;
import jl95.util.*;

public interface TypeSwitchedResponderIf<ABase, RBase> {

    void            addCaseWhile  (String typeAlias,
                                   Function1<Tuple2<RBase, Boolean>, ABase> responseFunction);
    void            removeCase    (String typeAlias);
    void            setDefaultCase(Function1<Tuple2<RBase, Boolean>, ABase> responseFunction);
    UVoidFuture   start         ();
    UVoidFuture   stop          ();
    Boolean         isRunning     ();
    ResponderIf<TypedPayload, TypedPayload> getBaseResponder();

    default void
    addCase        (String                   typeAlias,
                    Function1<RBase, ABase>  responseFunction) {
        addCaseWhile(typeAlias, (ABase a) -> tuple(responseFunction.apply(a), true));
    }
    default void
    addCaseBreak   (String                   typeAlias,
                    Function1<RBase, ABase>  responseFunction) {
        addCaseWhile(typeAlias, (ABase a) -> tuple(responseFunction.apply(a), false));
    }
    default <ABase2, RBase2> TypeSwitchedResponderIf<ABase2, RBase2>
    adapted        (Function1<ABase2, ABase> requestAdapter,
                    Function1<RBase, RBase2> responseAdapter) {
        return new TypeSwitchedResponderIf<>() {

            @Override
            public void addCaseWhile(String typeAlias, Function1<Tuple2<RBase2, Boolean>, ABase2> responseFunction) {
                TypeSwitchedResponderIf.this.addCaseWhile(typeAlias, a -> {
                    var r = responseFunction.apply(requestAdapter.apply(a));
                    return tuple(responseAdapter.apply(r.a1), r.a2);
                });
            }

            @Override
            public void removeCase(String typeAlias) {
                TypeSwitchedResponderIf.this.removeCase(typeAlias);
            }

            @Override
            public void setDefaultCase(Function1<Tuple2<RBase2, Boolean>, ABase2> responseFunction) { TypeSwitchedResponderIf.this.setDefaultCase(a -> {
                    var r = responseFunction.apply(requestAdapter.apply(a));
                    return tuple(responseAdapter.apply(r.a1), r.a2);
                }); }

            @Override
            public UVoidFuture start() {
                return TypeSwitchedResponderIf.this.start();
            }

            @Override
            public UVoidFuture stop() {
                return TypeSwitchedResponderIf.this.stop();
            }

            @Override
            public Boolean isRunning() {
                return TypeSwitchedResponderIf.this.isRunning();
            }

            @Override
            public ResponderIf<TypedPayload, TypedPayload> getBaseResponder() {
                return TypeSwitchedResponderIf.this.getBaseResponder();
            }
        };
    }
    default <ABase2>         TypeSwitchedResponderIf<ABase2, RBase>
    adaptedRequest (Function1<ABase2, ABase> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    default <RBase2>         TypeSwitchedResponderIf<ABase, RBase2>
    adaptedResponse(Function1<RBase, RBase2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}

