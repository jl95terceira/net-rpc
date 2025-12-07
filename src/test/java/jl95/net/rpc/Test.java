package jl95.net.rpc;

import static jl95.lang.SuperPowers.*;

import java.util.concurrent.CompletableFuture;

import jl95.net.rpc.collections.RequesterAdaptersCollection;
import jl95.net.rpc.collections.ResponderAdaptersCollection;
import jl95.net.io.CloseableIos;

public class Test {

    CloseableIos ioAsServer;
    CloseableIos ioAsClient;
    RequesterIf<String, String> requester;
    ResponderIf<String, String> responder;

    @org.junit.Before
    public void setUp() throws Exception {
        var responderFuture = CompletableFuture.supplyAsync(() -> {
            ioAsServer = Util.getIoAsServer(jl95.net.io.util.Defaults.serverAddr);
            return ResponderAdaptersCollection.asStringPostGetResponder(Responder.fromIo(ioAsServer));
        }, (task) -> new Thread(task).start());
        sleep(50);
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = Util.getIoAsClient(jl95.net.io.util.Defaults.serverAddr);
            return RequesterAdaptersCollection.asStringPostGetRequester(Requester.fromIo(ioAsClient));
        }, (task) -> new Thread(task).start());
        requester = requesterFuture.get();
        responder = responderFuture.get();
    }
    @org.junit.After
    public void tearDown() {
        if (responder.isRunning()) responder.stop().await();
        if (ioAsClient != null) ioAsClient.close();
        if (ioAsServer != null) ioAsServer.close();
    }

    @org.junit.Test
    public void testStartStop() {
        responder.respond(self::apply).await();
        responder.stop ()           .await();
        responder.respond(self::apply).await();
        responder.stop ()           .await();
    }
    @org.junit.Test
    public void test() {
        responder.respond(msg -> "hello, " + msg).await();
        org.junit.Assert.assertEquals("hello, world", requester.apply("world"));
    }
    @org.junit.Test
    public void test2() { // to confirm that the server socket is closed correctly (in tearDown) - otherwise, an address binding error will happen
        responder.respond(msg -> "greetings, " + msg).await();
        org.junit.Assert.assertEquals("greetings, universe", requester.apply("universe"));
    }
    @org.junit.Test
    public void testTimeout() {
        var timeout = 1000;
        responder.respond(self::apply).await();
        org.junit.Assert.assertEquals("first", requester.apply("first"));
        responder.stop().await();
        responder.respond(msg -> {
            sleep(timeout + 500);
            return "";
        }).await();
        try {
            var options = new RequesterIf.SendOptions.Editable();
            options.responseTimeoutMs = constant(timeout);
            requester.apply("second (to time out)", options);
            org.junit.Assert.fail("response timeout exception must be raised");
        }
        catch (Requester.ResponseTimeoutException ex) {/* as expected */}
        responder.stop ()           .await();
        responder.respond(self::apply).await();
        org.junit.Assert.assertEquals("third", requester.apply("third"));
    }
}
