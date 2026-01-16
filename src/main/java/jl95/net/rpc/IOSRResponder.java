package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Function2;
import jl95.lang.variadic.Tuple2;
import jl95.net.io.BytesIStreamReceiver;
import jl95.net.io.BytesOStreamSender;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;
import jl95.net.io.managed.ManagedIOStreamSupplier;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.UVoidFuture;

public abstract class IOSRResponder<A,R> implements Responder<A, R> {

    public static <A, R, C extends IOSRResponder<A, R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, Sender<byte[]> sender, Receiver<byte[]> receiver) {
        return constructor.apply(sender, receiver);
    }
    public static <A, R, C extends IOSRResponder<A, R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, IOStreamSupplier ios) {

        return of(constructor, BytesOStreamSender.of(ios.getOutputStream()), BytesIStreamReceiver.of(ios.getInputStream()));
    }
    public static <A, R, C extends IOSRResponder<A, R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, ManagedIOStreamSupplier mios) {

        return of(constructor, BytesOStreamSender.of(mios), BytesIStreamReceiver.of(mios));
    }

    private final Receiver<byte[]> receiver;
    private final Sender  <byte[]> sender;
    private final AtomicBoolean toStop;

    private IOSRResponder(Receiver<byte[]> receiver,
                          Sender  <byte[]> sender,
                          AtomicBoolean toStop) {
        this.receiver = receiver;
        this.sender   = sender;
        this.toStop   = toStop;
    }

    public IOSRResponder(Receiver<byte[]> receiver,
                         Sender  <byte[]> sender) {
        this(receiver, sender, new AtomicBoolean(false));
    }

    protected abstract A      deserialize(byte[] requestData);
    protected abstract byte[] serialize  (R      responseData);

    @Override
    public synchronized void respondWhile(Function1<Tuple2<R, Boolean>, A> responseFunction,
                                          RespondOptions options) {

        if (isRunning()) {
            throw new IllegalStateException();
        }
        toStop.set(false);
        receiver.recvWhile(request -> {
            var requestIdAsBytes = new byte[36];
            //var id = StringUTF8FromBytes.get().apply(idAsBytes);
            var requestPayload = new byte[request.length-36];
            System.arraycopy(request,  0, requestIdAsBytes, 0,                36);
            System.arraycopy(request, 36, requestPayload,   0, request.length-36);
            var responseAndToContinue = responseFunction.apply(deserialize(requestPayload));
            var responsePayload = serialize(responseAndToContinue.a1);
            var response = new byte[72+responsePayload.length];
            var responseIdAsBytes = StringUTF8ToBytes.get().apply(UUID.randomUUID().toString());
            System.arraycopy(responseIdAsBytes, 0, response,  0,  36);
            System.arraycopy(requestIdAsBytes,  0, response,  36, 36);
            System.arraycopy(responsePayload,   0, response,  72, responsePayload.length);
            sender.send(response);
            return responseAndToContinue.a2;
        }, options);
    }
    @Override
    public UVoidFuture waitRespondingStarted() {

        return receiver.recvWaitStarted();
    }
    @Override
    public UVoidFuture stop() {

        if (!isRunning()) {
            throw new IllegalStateException();
        }
        return receiver.recvStop();
    }
    @Override
    public UVoidFuture waitRespondingStopped() {
        return receiver.recvWaitStopped();
    }

    @Override
    public boolean isRunning() {

        return receiver.isReceiving();
    }

    @Override
    public <A2, R2> Responder<A2, R2> adapted(Function1<A2, A> requestAdapter,
                                              Function1<R, R2> responseAdapter) {
        return new IOSRResponder<>(receiver, sender, toStop) {
            @Override protected A2 deserialize(byte[] requestData) {
                return requestAdapter.apply(IOSRResponder.this.deserialize(requestData));
            }
            @Override protected byte[] serialize(R2 responseData) {
                return IOSRResponder.this.serialize(responseAdapter.apply(responseData));
            }
        };
    }
    @Override
    public <A2> Responder<A2, R> adaptedRequest (Function1<A2, A> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    @Override
    public <R2> Responder<A, R2> adaptedResponse(Function1<R, R2> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
    @Override
    public void close() {
        ensureStopped();
        receiver.close();
        sender.close();
    }
}
