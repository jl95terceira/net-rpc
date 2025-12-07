package jl95.net.rpc;

import static jl95.lang.SuperPowers.uncheck;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.JsonValue;

import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.net.rpc.util.Request;
import jl95.net.rpc.util.Response;
import jl95.net.rpc.util.SerdesDefaults;
import jl95.util.*;

public class Requester implements RequesterIf<JsonValue, JsonValue> {

    private enum         ResponseExceptionalStatus {
        FAIL_TIMEOUT;
    }
    private static class ResponseStatusAndData {
        public ResponseExceptionalStatus status   = null;
        public Response response;
    }

    public static class ResponseTimeoutException extends RuntimeException {}

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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Requester(SenderIf  <Request>  sender,
                      ReceiverIf<Response> receiver) {this.sender = sender; this.receiver = receiver;}

    @Override
    synchronized public final JsonValue apply(JsonValue payload, SendOptions options) {

        var request     = new Request();
        request.id      = UUID.randomUUID();
        request.payload = payload;
        var responseFuture = new CompletableFuture<ResponseStatusAndData>();
        while (true) {
            sender.send(request);
            var responseSync = new Object();
            var ioErrorFuture  = new CompletableFuture<Boolean>();
            receiver.recvWhile(response -> {
                synchronized (responseSync) {
                    if (responseFuture.isDone()) /* oof, just timed out */ {
                        ioErrorFuture.complete(false);
                        return false;
                    }
                    try {
                        var rsd = new ResponseStatusAndData();
                        if (!response.requestId.equals(request.id)) {
                            options.onOutOfSync();
                            return true; // discard response (old, out of sync), wait for next
                        }
                        else {
                            rsd.response = response;
                        }
                        responseFuture.complete(rsd);
                        ioErrorFuture.complete(false);
                        return false;
                    }
                    catch (Exception ex) {
                        ioErrorFuture.complete(true);
                        return false;
                    }
                }
            });
            scheduler.schedule(() -> {
                synchronized (responseSync) {
                    if (responseFuture.isDone()) return;
                    // not completed - set failed (by time-out)
                    receiver.recvStop().await();
                    var rsd = new ResponseStatusAndData();
                    rsd.status = ResponseExceptionalStatus.FAIL_TIMEOUT;
                    responseFuture.complete(rsd);
                    ioErrorFuture.complete(false);
                }
            }, options.getResponseTimeoutMs(), TimeUnit.MILLISECONDS);
            var ioErrorOccurred = uncheck(() -> ioErrorFuture.get());
            if (!ioErrorOccurred) {
                break;
            }
            // if error occurred, retry send request and receive response
        }
        var rsd = uncheck(() -> responseFuture.get());
        if (rsd.status == ResponseExceptionalStatus.FAIL_TIMEOUT) {
            throw new ResponseTimeoutException();
        }
        receiver.ensureStopped(); // to prevent races between consecutive requests (could cause illegal receiver state exceptions)
        return rsd.response.payload;
    }

    @Override
    public final InputStream  getInputStream () { return receiver.getInputStream (); }
    @Override
    public final OutputStream getOutputStream() { return sender  .getOutputStream(); }
}
