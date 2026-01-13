package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.tuple;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Tuple2;
import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.UVoidFuture;

public abstract class GenericResponder<A,R> implements ResponderIf<A, R> {

    public static class StartWhenAlreadyRunningException extends RuntimeException {}
    public static class StopWhenNotRunningException      extends RuntimeException {}

    public static <A,R,C extends GenericResponder<A,R>> C of(Function1<C,SenderReceiverIf<byte[], byte[]>> constructor, SenderReceiverIf<byte[], byte[]> sr) {
        return constructor.apply(sr);
    }
    public static <A,R,C extends GenericResponder<A,R>> C of(Function1<C,SenderReceiverIf<byte[], byte[]>> constructor, Ios ios) {

        return of(constructor, SenderReceiverIf.fromIo(ios));
    }
    public static <A,R,C extends GenericResponder<A,R>> C of(Function1<C,SenderReceiverIf<byte[], byte[]>> constructor, ManagedIos ios) {

        return of(constructor, SenderReceiverIf.fromManagedIo(ios));
    }

    private final ReceiverIf<byte[]> receiver;
    private final ThreadPoolExecutor receiverTpe;
    private final SenderIf  <byte[]> sender;

    private GenericResponder(ReceiverIf<byte[]> receiver,
                             ThreadPoolExecutor receiverTpe,
                             SenderIf  <byte[]> sender) {
        this.receiver = receiver;
        this.receiverTpe = receiverTpe;
        this.sender   = sender;
    }
    public GenericResponder(SenderReceiverIf<byte[],byte[]> sr) {
        this(sr.getReceiver(), new ScheduledThreadPoolExecutor(1), sr.getSender());
    }
    @Deprecated
    public GenericResponder(ReceiverIf<byte[]> receiver,
                            SenderIf  <byte[]> sender) {
        this(SenderReceiverIf.ofConstant(sender,receiver));
    }

    protected abstract A      deserialize(byte[] requestData);
    protected abstract byte[] serialize  (R      responseData);

    @Override
    synchronized public UVoidFuture respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction,
                                                 RespondOptions options) {

        if (isRunning()) {
            throw new StartWhenAlreadyRunningException();
        }
        receiverTpe.execute(() -> receiver.recvWhile(request -> {

            var requestIdAsBytes = new byte[36];
            //var id = StringUTF8FromBytes.get().apply(idAsBytes);
            var requestPayload = new byte[request.length-36];
            System.arraycopy(request,  0, requestIdAsBytes, 0,                36);
            System.arraycopy(request, 36, requestPayload,   0, request.length-36);
            var object = responseFunction.apply(deserialize(requestPayload));
            var responsePayload = serialize(object.a1);
            try {
                var response = new byte[72+responsePayload.length];
                var responseIdAsBytes = StringUTF8ToBytes.get().apply(UUID.randomUUID().toString());
                System.arraycopy(responseIdAsBytes, 0, response,  0,  36);
                System.arraycopy(requestIdAsBytes,  0, response,  36, 36);
                System.arraycopy(responsePayload,   0, response,  72, responsePayload.length);
                sender.send(response);
            }
            catch (Exception ex) {
                return true; // continue receiving
            }
            return object.a2;
        }, options));
        return receiver.recvWaitStarted();
    }
    @Override
    synchronized public UVoidFuture stop() {

        if (!isRunning()) throw new StopWhenNotRunningException();
        return receiver.recvStop();
    }

    @Override
    public Boolean isRunning() {

        return receiver.isReceiving();
    }

    public <A2, R2> ResponderIf<A2, R2> adapted        (Function1<A2, A> requestAdapter,
                                                        Function1<R, R2> responseAdapter) {
        return new GenericResponder<>(receiver, receiverTpe, sender) {
            @Override protected A2 deserialize(byte[] requestData) {
                return requestAdapter.apply(GenericResponder.this.deserialize(requestData));
            }
            @Override protected byte[] serialize(R2 responseData) {
                return GenericResponder.this.serialize(responseAdapter.apply(responseData));
            }
        };
    }
    public <A2>     ResponderIf<A2, R>  adaptedRequest (Function1<A2, A> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    public <R2>     ResponderIf<A, R2>  adaptedResponse(Function1<R, R2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
