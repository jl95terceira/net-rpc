package jl95.net.rpc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jl95.lang.SuperPowers.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            return ResponderAdaptersCollection.asStringResponder(Responder.fromIo(ioAsServer));
        }, (task) -> new Thread(task).start());
        sleep(50);
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = Util.getIoAsClient(jl95.net.io.util.Defaults.serverAddr);
            return RequesterAdaptersCollection.asStringRequester(Requester.fromIo(ioAsClient));
        }, (task) -> new Thread(task).start());
        requester = requesterFuture.get();
        responder = responderFuture.get();
    }
    @org.junit.After
    public void tearDown() {
        if (responder.isRunning()) responder.stop().get();
        if (ioAsClient != null) ioAsClient.close();
        if (ioAsServer != null) ioAsServer.close();
    }

    @org.junit.Test
    public void testStartStop() {
        responder.respond(self::apply).get();
        responder.stop ()           .get();
        responder.respond(self::apply).get();
        responder.stop ()           .get();
    }
    @org.junit.Test
    public void test() throws ExecutionException, InterruptedException, TimeoutException {
        responder.respond(msg -> "hello, " + msg).get();
        org.junit.Assert.assertEquals("hello, world", requester.apply("world").get(5, SECONDS));
    }
    @org.junit.Test
    public void test2() throws ExecutionException, InterruptedException, TimeoutException { // to confirm that the server socket is closed correctly (in tearDown) - otherwise, an address binding error will happen
        responder.respond(msg -> "greetings, " + msg).get();
        org.junit.Assert.assertEquals("greetings, universe", requester.apply("universe").get(5, SECONDS));
    }
    @org.junit.Test
    public void testTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        var timeout = 1000;
        responder.respond(self::apply).get();
        org.junit.Assert.assertEquals("first", requester.apply("first").get(5, SECONDS));
        responder.stop().get();
        responder.respond(msg -> {
            sleep(timeout + 500);
            return "";
        }).get();
        try {
            requester.apply("second (to time out)").get(timeout, TimeUnit.MILLISECONDS);
            org.junit.Assert.fail("response timeout exception must be raised");
        }
        catch (Exception ex) {/* as expected */}
        responder.stop ()           .get();
        responder.respond(self::apply).get();
        org.junit.Assert.assertEquals("third", requester.apply("third").get(5, SECONDS));
    }
}
