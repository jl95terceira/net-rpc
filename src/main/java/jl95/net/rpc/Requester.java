package jl95.net.rpc;

import static jl95.lang.SuperPowers.uncheck;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import javax.json.JsonValue;

import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.net.rpc.util.Request;
import jl95.net.rpc.util.Response;
import jl95.net.rpc.util.SerdesDefaults;

public class Requester implements RequesterIf<JsonValue, JsonValue> {

    public static Requester fromSr(SenderReceiverIf<byte[], byte[]> sr) {;
        return new Requester(sr.getSender()  .adaptedSender  (SerdesDefaults.requestToBytes),
                             sr.getReceiver().adaptedReceiver(SerdesDefaults.responseFromBytes));
    }
    public static Requester fromIo(Ios ios) {

        return fromSr(SenderReceiverIf.fromIo(ios));
    }
    public static Requester fromManagedIo(ManagedIos ios) {

        return fromSr(SenderReceiverIf.fromManagedIo(ios));
    }

    private final SenderIf  <Request>      sender;
    private final ReceiverIf<Response>     receiver;
    private final Map<UUID, CompletableFuture<JsonValue>> responseFuturesMap;

    private Requester(SenderIf  <Request>  sender,
                      ReceiverIf<Response> receiver,
                      int nrOfResponsesToWaitMax) {
        this.sender = sender;
        this.receiver = receiver;
        this.responseFuturesMap = new LinkedHashMap<>() {
            @Override public boolean removeEldestEntry(Map.Entry<UUID ,CompletableFuture<JsonValue>> eldestEntry) {
                return size() > nrOfResponsesToWaitMax;
            }
        };
    }
    private Requester(SenderIf  <Request>  sender,
                      ReceiverIf<Response> receiver) {
        this(sender, receiver, 42);
    }

    private void startReceiving() {
        receiver.recvWhile(response -> {
            if (responseFuturesMap.containsKey(response.requestId)) {
                responseFuturesMap.get(response.requestId).complete(response.payload);
                responseFuturesMap.remove(response.requestId);
            }
            return !responseFuturesMap.isEmpty();
        });
    }

    @Override
    synchronized public final Future<JsonValue> apply(JsonValue payload) {

        var request     = new Request();
        request.id      = UUID.randomUUID();
        request.payload = payload;
        var responseFuture = new CompletableFuture<JsonValue>();
        responseFuturesMap.put(request.id, responseFuture);
        sender.send(request);
        if (!receiver.isReceiving()) {
            startReceiving();
        }
        return responseFuture;
    }

    @Override
    public final InputStream  getInputStream () { return receiver.getInputStream (); }
    @Override
    public final OutputStream getOutputStream() { return sender  .getOutputStream(); }
}
