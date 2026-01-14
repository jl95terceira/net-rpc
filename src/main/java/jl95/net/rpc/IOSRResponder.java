package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Tuple2;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;
import jl95.net.io.SenderReceiver;
import jl95.net.io.managed.ManagedIOStreamSupplier;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.UVoidFuture;

public abstract class IOSRResponder<A,R> implements Responder<A, R> {

    public static class StartWhenAlreadyRunningException extends RuntimeException {}
    public static class StopWhenNotRunningException      extends RuntimeException {}

    public static <A, R, C extends IOSRResponder<A, R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, SenderReceiver<byte[], byte[]> sr) {
        return constructor.apply(sr);
    }
    public static <A, R, C extends IOSRResponder<A, R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, IOStreamSupplier ios) {

        return of(constructor, SenderReceiver.fromIo(ios));
    }
    public static <A, R, C extends IOSRResponder<A, R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, ManagedIOStreamSupplier ios) {

        return of(constructor, SenderReceiver.fromManagedIo(ios));
    }

    private final Receiver<byte[]> receiver;
    private final Sender  <byte[]> sender;
    private final ThreadPoolExecutor receiverTpe;

    private IOSRResponder(Receiver<byte[]> receiver,
                          ThreadPoolExecutor receiverTpe,
                          Sender  <byte[]> sender) {
        this.receiver = receiver;
        this.receiverTpe = receiverTpe;
        this.sender   = sender;
    }
    public IOSRResponder(SenderReceiver<byte[],byte[]> sr) {
        this(sr.getReceiver(), new ScheduledThreadPoolExecutor(1), sr.getSender());
    }
    @Deprecated
    public IOSRResponder(Receiver<byte[]> receiver,
                         Sender  <byte[]> sender) {
        this(SenderReceiver.ofConstant(sender,receiver));
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

    public <A2, R2> Responder<A2, R2> adapted        (Function1<A2, A> requestAdapter,
                                                        Function1<R, R2> responseAdapter) {
        return new IOSRResponder<>(receiver, receiverTpe, sender) {
            @Override protected A2 deserialize(byte[] requestData) {
                return requestAdapter.apply(IOSRResponder.this.deserialize(requestData));
            }
            @Override protected byte[] serialize(R2 responseData) {
                return IOSRResponder.this.serialize(responseAdapter.apply(responseData));
            }
        };
    }
    public <A2> Responder<A2, R> adaptedRequest (Function1<A2, A> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    public <R2> Responder<A, R2> adaptedResponse(Function1<R, R2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
