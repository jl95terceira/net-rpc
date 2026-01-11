package jl95.net.rpc;

import static jl95.lang.SuperPowers.strict;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

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

public class Requester implements RequesterIf<byte[], byte[]> {

    public static Requester fromSr(SenderReceiverIf<byte[], byte[]> sr) {;
        return new Requester(sr.getSender(), sr.getReceiver());
    }
    public static Requester fromIo(Ios ios) {

        return fromSr(SenderReceiverIf.fromIo(ios));
    }
    public static Requester fromManagedIo(ManagedIos ios) {

        return fromSr(SenderReceiverIf.fromManagedIo(ios));
    }

    private final SenderIf  <byte[]>  sender;
    private final ReceiverIf<byte[]> receiver;
    private final ThreadPoolExecutor   receiverTpe = new ScheduledThreadPoolExecutor(1);
    private final StrictMap<String, CompletableFuture<byte[]>> responseFuturesMap;

    private Requester(SenderIf  <byte[]>  sender,
                      ReceiverIf<byte[]> receiver,
                      int nrOfResponsesToWaitMax) {
        this.sender = sender;
        this.receiver = receiver;
        this.responseFuturesMap = strict(new LinkedHashMap<>() {
            @Override public boolean removeEldestEntry(Map.Entry<String ,CompletableFuture<byte[]>> eldestEntry) {
                return size() > nrOfResponsesToWaitMax;
            }
        });
    }
    private Requester(SenderIf  <byte[]>  sender,
                      ReceiverIf<byte[]> receiver) {
        this(sender, receiver, 10);
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

    @Override
    synchronized public final UFuture<byte[]> apply(byte[] payload) {

        var requestId = UUID.randomUUID().toString();
        var responseFuture = new CompletableFuture<byte[]>();
        responseFuturesMap.put(requestId, responseFuture);
        var request = new byte[36+payload.length];
        var requestIdAsBytes = StringUTF8ToBytes.get().apply(requestId);
        System.arraycopy(requestIdAsBytes, 0, request,  0, requestIdAsBytes.length);
        System.arraycopy(payload,          0, request, 36, payload         .length);
        sender.send(request);
        if (!receiver.isReceiving()) {
            startReceiving().get();
        }
        return UFuture.of(responseFuture);
    }

    @Override
    public final InputStream  getInputStream () { return receiver.getInputStream (); }
    @Override
    public final OutputStream getOutputStream() { return sender  .getOutputStream(); }
}
