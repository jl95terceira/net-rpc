package jl95.net.rpc;

import static jl95.lang.SuperPowers.mapped;
import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.strict;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Function2;
import jl95.lang.variadic.Method1;
import jl95.net.io.BytesIStreamReceiver;
import jl95.net.io.BytesOStreamSender;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.ManagedIOStream;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.StrictMap;
import jl95.util.UFuture;
import jl95.util.UVoidFuture;

public abstract class IOSRRequester<A,R> implements Requester<A,R> {

    public static <A,R,C extends IOSRRequester<A,R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, Sender<byte[]> sender, Receiver<byte[]> receiver) {
        return constructor.apply(sender, receiver);
    }
    public static <A,R,C extends IOSRRequester<A,R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, IOStreamSupplier ios) {

        return of(constructor, BytesOStreamSender.of(ios.getOutputStream()), BytesIStreamReceiver.of(ios.getInputStream()));
    }
    public static <A,R,C extends IOSRRequester<A,R>> C of(Function2<C, Sender<byte[]>, Receiver<byte[]>> constructor, ManagedIOStream mios) {

        return of(constructor, BytesOStreamSender.of(mios.output()), BytesIStreamReceiver.of(mios.input()));
    }

    private final Sender  <byte[]> sender;
    private final Receiver<byte[]> receiver;
    private final AtomicReference<Method1<Runnable>> receiverExecutor;
    private final StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap;
    private final AtomicBoolean autoAcceptResponses;

    protected IOSRRequester(Sender  <byte[]> sender,
                          Receiver<byte[]> receiver,
                          AtomicReference<Method1<Runnable>> receiverExecutor,
                          StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap,
                          AtomicBoolean autoAcceptResponses) {
        this.sender = sender;
        this.receiver = receiver;
        this.receiverExecutor = receiverExecutor;
        this.responseFuturesMap = responseFuturesMap;
        this.autoAcceptResponses = autoAcceptResponses;
    }

    public IOSRRequester(Sender<byte[]> sender, Receiver<byte[]> receiver,
                         int nrOfResponsesToWaitMax) {
        this(sender,
             receiver,
             new AtomicReference<>(null),
             strict(new LinkedHashMap<>() {
                 @Override
                 public boolean removeEldestEntry(Map.Entry<String, CompletableFuture<byte[]>> eldestEntry) {
                     return size() > nrOfResponsesToWaitMax;
                 }
             }),
             new AtomicBoolean(true));
    }
    public IOSRRequester(Sender<byte[]> sender, Receiver<byte[]> receiver) {
        this(sender, receiver, 10);
    }

    public void setResponseAcceptanceExecutor(Method1<Runnable> executor) {
        receiverExecutor.set(executor);
    }
    public boolean isAcceptingResponses() {
        return receiver.isReceiving();
    }
    public UVoidFuture acceptResponses(boolean toAccept) {
        if (toAccept == isAcceptingResponses()) {
            throw new IllegalStateException();
        }
        if (toAccept) {
            if (receiverExecutor.get() == null) {
                receiverExecutor.set(new ScheduledThreadPoolExecutor(1)::execute);
            }
            receiverExecutor.get().accept(() -> receiver.recvWhile(response -> {
                //var idAsBytes = new byte[36];
                //var id = StringUTF8FromBytes.get().apply(idAsBytes);
                var requestIdAsBytes = new byte[36];
                var payload = new byte[response.length-72];
                //System.arraycopy(response,  0, idAsBytes,        0,                 36);
                System.arraycopy(response, 36, requestIdAsBytes, 0,                 36);
                System.arraycopy(response, 72, payload,          0, response.length-72);
                var requestId = StringUTF8FromBytes.get().apply(requestIdAsBytes);
                if (responseFuturesMap.containsKey(requestId)) {
                    responseFuturesMap.get(requestId).complete(payload);
                    responseFuturesMap.remove(requestId);
                }
                return !autoAcceptResponses.get() || !responseFuturesMap.isEmpty();
            }));
            return receiver.recvWaitStarted();
        }
        else {
            return receiver.recvStop();
        }
    }
    public void setAutoAcceptResponses(boolean value) {
        autoAcceptResponses.set(value);
    }

    protected abstract byte[] serialize  (A      requestData);
    protected abstract R      deserialize(byte[] responseData);

    @Override
    public synchronized UFuture<R> apply(A data) {

        var payload = serialize(data);
        var requestId = UUID.randomUUID().toString();
        var responseFuture = new CompletableFuture<byte[]>();
        responseFuturesMap.put(requestId, responseFuture);
        var request = new byte[36+payload.length];
        var requestIdAsBytes = StringUTF8ToBytes.get().apply(requestId);
        System.arraycopy(requestIdAsBytes, 0, request,  0, requestIdAsBytes.length);
        System.arraycopy(payload,          0, request, 36, payload         .length);
        if (autoAcceptResponses.get() && !isAcceptingResponses()) {
            acceptResponses(true).get();
        }
        sender.send(request);
        return mapped(this::deserialize, UFuture.of(responseFuture));
    }

    @Override
    public <A2, R2> IOSRRequester<A2, R2> adapted(Function1<A, A2> requestAdapter,
                                                  Function1<R2, R> responseAdapter) {
        return new IOSRRequester<>(sender, receiver, receiverExecutor, responseFuturesMap, autoAcceptResponses) {
            @Override protected byte[] serialize(A2 data) {
                return IOSRRequester.this.serialize(requestAdapter.apply(data));
            }
            @Override protected R2 deserialize(byte[] data) {
                return responseAdapter.apply(IOSRRequester.this.deserialize(data));
            }
        };
    }
    @Override
    public <A2> IOSRRequester<A2, R> adaptedRequest (Function1<A, A2> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    @Override
    public <R2> IOSRRequester<A, R2> adaptedResponse(Function1<R2, R> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }

    @Override
    public void close() {
        sender.close();
        if (isAcceptingResponses()) {
            acceptResponses(false).get();
        }
        receiver.close();
    }
}
