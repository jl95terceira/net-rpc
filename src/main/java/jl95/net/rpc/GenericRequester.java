package jl95.net.rpc;

import static jl95.lang.SuperPowers.constant;
import static jl95.lang.SuperPowers.mapped;
import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.strict;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.Function1;
import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.StrictMap;
import jl95.util.UFuture;
import jl95.util.UVoidFuture;

public abstract class GenericRequester<A,R> implements RequesterIf<A,R> {

    public static <A,R,C extends GenericRequester<A,R>> C of(Function1<C, SenderReceiverIf<byte[], byte[]>> constructor, SenderReceiverIf<byte[],byte[]> sr) {
        return constructor.apply(sr);
    }
    public static <A,R,C extends GenericRequester<A,R>> C of(Function1<C, SenderReceiverIf<byte[], byte[]>> constructor, Ios ios) {

        return of(constructor, SenderReceiverIf.fromIo(ios));
    }
    public static <A,R,C extends GenericRequester<A,R>> C of(Function1<C, SenderReceiverIf<byte[], byte[]>> constructor, ManagedIos ios) {

        return of(constructor, SenderReceiverIf.fromManagedIo(ios));
    }

    private final SenderIf  <byte[]> sender;
    private final ReceiverIf<byte[]> receiver;
    private final ThreadPoolExecutor receiverTpe;
    private final StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap;

    private GenericRequester(SenderIf  <byte[]> sender,
                             ReceiverIf<byte[]> receiver,
                             ThreadPoolExecutor receiverTpe,
                             StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap) {
        this.sender = sender;
        this.receiver = receiver;
        this.receiverTpe = receiverTpe;
        this.responseFuturesMap = responseFuturesMap;
    }
    public GenericRequester(SenderReceiverIf<byte[],byte[]> sr,
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
    public GenericRequester(SenderReceiverIf<byte[],byte[]> sr) {
        this(sr, 10);
    }
    @Deprecated
    public GenericRequester(SenderIf  <byte[]>  sender,
                            ReceiverIf<byte[]> receiver,
                            int nrOfResponsesToWaitMax) {
        this(SenderReceiverIf.ofConstant(sender, receiver), nrOfResponsesToWaitMax);
    }
    @Deprecated
    public GenericRequester(SenderIf  <byte[]>  sender,
                      ReceiverIf<byte[]> receiver) {
        this(SenderReceiverIf.ofConstant(sender, receiver));
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
    public final InputStream  getInputStream () { return receiver.getInputStream (); }
    @Override
    public final OutputStream getOutputStream() { return sender  .getOutputStream(); }

    @Override
    public <A2, R2> GenericRequester<A2, R2> adapted        (Function1<A, A2> requestAdapter,
                                                              Function1<R2, R> responseAdapter) {
        return new GenericRequester<>(sender, receiver, receiverTpe, responseFuturesMap) {
            @Override protected byte[] serialize(A2 data) {
                return GenericRequester.this.serialize(requestAdapter.apply(data));
            }
            @Override protected R2 deserialize(byte[] data) {
                return responseAdapter.apply(GenericRequester.this.deserialize(data));
            }
        };
    }
    @Override
    public <A2>     GenericRequester<A2, R>  adaptedRequest (Function1<A, A2> requestAdapter) {

        return adapted(requestAdapter, self::apply);
    }
    @Override
    public <R2>     GenericRequester<A, R2>  adaptedResponse(Function1<R2, R> responseAdapter) {

        return adapted(self::apply, responseAdapter);
    }
}
