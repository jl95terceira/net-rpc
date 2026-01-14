package jl95.net.rpc;

import static jl95.lang.SuperPowers.mapped;
import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.strict;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.Function1;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;
import jl95.net.io.SenderReceiver;
import jl95.net.io.managed.ManagedIOStreamSupplier;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.StrictMap;
import jl95.util.UFuture;
import jl95.util.UVoidFuture;

public abstract class IOSRRequester<A,R> implements Requester<A,R> {

    public static <A,R,C extends IOSRRequester<A,R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, SenderReceiver<byte[],byte[]> sr) {
        return constructor.apply(sr);
    }
    public static <A,R,C extends IOSRRequester<A,R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, IOStreamSupplier ios) {

        return of(constructor, SenderReceiver.fromIo(ios));
    }
    public static <A,R,C extends IOSRRequester<A,R>> C of(Function1<C, SenderReceiver<byte[], byte[]>> constructor, ManagedIOStreamSupplier ios) {

        return of(constructor, SenderReceiver.fromManagedIo(ios));
    }

    private final Sender  <byte[]> sender;
    private final Receiver<byte[]> receiver;
    private final ThreadPoolExecutor receiverTpe;
    private final StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap;

    private IOSRRequester(Sender  <byte[]> sender,
                          Receiver<byte[]> receiver,
                          ThreadPoolExecutor receiverTpe,
                          StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap) {
        this.sender = sender;
        this.receiver = receiver;
        this.receiverTpe = receiverTpe;
        this.responseFuturesMap = responseFuturesMap;
    }
    public IOSRRequester(SenderReceiver<byte[],byte[]> sr,
                         int nrOfResponsesToWaitMax) {
        this(sr.getSender(),
             sr.getReceiver(),
             new ScheduledThreadPoolExecutor(1),
             strict(new LinkedHashMap<>() {
                 @Override
                 public boolean removeEldestEntry(Map.Entry<String, CompletableFuture<byte[]>> eldestEntry) {
                     return size() > nrOfResponsesToWaitMax;
                 }
             }));
    }
    public IOSRRequester(SenderReceiver<byte[],byte[]> sr) {
        this(sr, 10);
    }
    @Deprecated
    public IOSRRequester(Sender  <byte[]>  sender,
                         Receiver<byte[]> receiver,
                         int nrOfResponsesToWaitMax) {
        this(SenderReceiver.ofConstant(sender, receiver), nrOfResponsesToWaitMax);
    }
    @Deprecated
    public IOSRRequester(Sender  <byte[]>  sender,
                         Receiver<byte[]> receiver) {
        this(SenderReceiver.ofConstant(sender, receiver));
    }

    private UVoidFuture startReceiving() {
        receiverTpe.execute(() -> receiver.recvWhile(response -> {
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
            return !responseFuturesMap.isEmpty();
        }));
        return receiver.recvWaitStarted();
    }

    protected abstract byte[] serialize  (A      requestData);
    protected abstract R      deserialize(byte[] responseData);

    @Override
    synchronized public final UFuture<R> apply(A data) {

        var payload = serialize(data);
        var requestId = UUID.randomUUID().toString();
        var responseFuture = new CompletableFuture<byte[]>();
        responseFuturesMap.put(requestId, responseFuture);
        var request = new byte[36+payload.length];
        var requestIdAsBytes = StringUTF8ToBytes.get().apply(requestId);
        System.arraycopy(requestIdAsBytes, 0, request,  0, requestIdAsBytes.length);
        System.arraycopy(payload,          0, request, 36, payload         .length);
        if (!receiver.isReceiving()) {
            startReceiving().get();
        }
        sender.send(request);
        return mapped(this::deserialize, UFuture.of(responseFuture));
    }

    @Override
    public <A2, R2> IOSRRequester<A2, R2> adapted        (Function1<A, A2> requestAdapter,
                                                              Function1<R2, R> responseAdapter) {
        return new IOSRRequester<>(sender, receiver, receiverTpe, responseFuturesMap) {
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
}
