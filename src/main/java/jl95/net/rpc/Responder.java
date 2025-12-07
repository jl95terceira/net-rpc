package jl95.net.rpc;

import java.util.UUID;

import javax.json.JsonValue;

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
import jl95.util.*;

public class Responder implements ResponderIf<JsonValue, JsonValue> {

    public static class StartWhenAlreadyRunningException extends RuntimeException {}
    public static class StopWhenNotRunningException      extends RuntimeException {}

    public static Responder fromSr(SenderReceiverIf<byte[], byte[]> sr) {
        return new Responder(
            sr.getReceiver()
                .adaptedReceiver(SerdesDefaults.stringFromBytes)
                .adaptedReceiver(SerdesDefaults.jsonFromString)
                .adaptedReceiver(RequestJsonSerdes::fromJson),
            sr.getSender()
                .adaptedSender(SerdesDefaults.stringToBytes)
                .adaptedSender(SerdesDefaults.jsonToString)
                .adaptedSender(ResponseJsonSerdes::toJson)
        );
    }
    public static Responder fromIo(Ios ios) {

        return fromSr(SenderReceiverIf.fromIo(ios));
    }

    private final ReceiverIf<Request>  receiver;
    private final SenderIf  <Response> sender;

    private Responder(ReceiverIf<Request>  receiver,
                      SenderIf  <Response> sender) {
        this.receiver = receiver;
        this.sender   = sender;
    }

    @Override
    synchronized public VoidAwaitable respondWhile(Function1<Tuple2<JsonValue, Boolean>, JsonValue> responseFunction,
                                                     RespondOptions options) {

        if (isRunning()) {
            throw new StartWhenAlreadyRunningException();
        }
        return receiver.recvWhile(request -> {

            var requestObject  = request.payload;
            var responseObject = responseFunction.apply(requestObject);
            var response       = new Response();
            response.id        = UUID.randomUUID();
            response.requestId = request.id;
            response.payload   = responseObject.a1;
            try {
                sender.send(response);
            }
            catch (Exception ex) {
                return true; // continue receiving
            }
            return responseObject.a2;
        }, options);
    }
    @Override
    synchronized public VoidAwaitable stop() {

        if (!isRunning()) throw new StopWhenNotRunningException();
        return receiver.recvStop();
    }

    @Override
    public Boolean isRunning() {

        return receiver.isReceiving();
    }
}
