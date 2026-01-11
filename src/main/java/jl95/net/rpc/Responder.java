package jl95.net.rpc;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.*;
import jl95.net.rpc.util.serdes.RequestJsonSerdes;
import jl95.net.rpc.util.serdes.ResponseJsonSerdes;
import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.rpc.util.Request;
import jl95.net.rpc.util.Response;
import jl95.net.rpc.util.SerdesDefaults;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.*;

public class Responder implements ResponderIf<byte[], byte[]> {

    public static class StartWhenAlreadyRunningException extends RuntimeException {}
    public static class StopWhenNotRunningException      extends RuntimeException {}

    public static Responder fromSr(SenderReceiverIf<byte[], byte[]> sr) {
        return new Responder(sr.getReceiver(), sr.getSender());
    }
    public static Responder fromIo(Ios ios) {

        return fromSr(SenderReceiverIf.fromIo(ios));
    }

    private final ReceiverIf<byte[]> receiver;
    private final ThreadPoolExecutor receiverTpe = new ScheduledThreadPoolExecutor(1);
    private final SenderIf  <byte[]> sender;

    private Responder(ReceiverIf<byte[]> receiver,
                      SenderIf  <byte[]> sender) {
        this.receiver = receiver;
        this.sender   = sender;
    }

    @Override
    synchronized public UVoidFuture respondWhile(Function1<Tuple2<byte[], Boolean>, byte[]> responseFunction,
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
            var object = responseFunction.apply(requestPayload);
            var responsePayload = object.a1;
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
}
