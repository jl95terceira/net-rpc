package jl95.net.rpc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jl95.lang.SuperPowers.*;
import static jl95.net.io.Util.getIoAsClient;
import static jl95.net.io.Util.getIoAsServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import jl95.lang.I;
import jl95.net.rpc.collections.RequesterAdaptersCollection;
import jl95.net.rpc.collections.ResponderAdaptersCollection;
import jl95.net.io.CloseableIOStreamSupplier;

public class Test {

    CloseableIOStreamSupplier ioAsServer;
    CloseableIOStreamSupplier ioAsClient;
    Requester<String, String> requester;
    Responder<String, String> responder;

    @org.junit.Before
    public void setUp() throws Exception {
        System.out.println("set up");
        var responderFuture = CompletableFuture.supplyAsync(() -> {
            ioAsServer = getIoAsServer(jl95.net.io.util.Defaults.serverAddr);
            return ResponderAdaptersCollection.asStringResponder(BytesIOSRResponder.of(ioAsServer))
                    .adaptedRequest((String s) -> {
                        System.out.println("Got request: "+s);
                        return s;
                    })
                    .adaptedResponse((String s) -> {
                        System.out.println("Responding: "+s);
                        return s;
                    });
        }, (task) -> new Thread(task).start());
        sleep(50);
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = getIoAsClient(jl95.net.io.util.Defaults.serverAddr);
            return RequesterAdaptersCollection.asStringRequester(BytesIOSRRequester.of(ioAsClient))
                    .adaptedRequest((String s) -> {
                        System.out.println("Requesting: "+s);
                        return s;
                    })
                    .adaptedResponse((String s) -> {
                        System.out.println("Got response: "+s);
                        return s;
                    });
        }, (task) -> new Thread(task).start());
        requester = requesterFuture.get();
        responder = responderFuture.get();
    }
    @org.junit.After
    public void tearDown() {
        System.out.println("tear down");
        if (responder.isRunning()) {
            responder.stop()
                    .get(3, SECONDS);
        }
        System.out.println("responder stopped");
        if (ioAsClient != null) ioAsClient.close();
        if (ioAsServer != null) ioAsServer.close();
    }

    public static void threaded(Runnable r) {
        new Thread(r).start();
    }

    @org.junit.Test
    public void testStartStop() {
        threaded(() -> responder.respond(self::apply));
        responder.waitRespondingStarted().get();
        Assert.assertTrue(responder.isRunning());
        responder.stop().get();
    }
    @org.junit.Test
    public void testStartStopTwice() {
        I.range(2).forEach(i -> testStartStop());
    }
    @org.junit.Test
    public void test() {
        threaded(() -> responder.respond(msg -> "hello, " + msg));
        responder.waitRespondingStarted().get();
        Assert.assertEquals("hello, world", requester.apply("world").get(3, SECONDS));
    }
    @org.junit.Test
    public void test2() { // to confirm that the server socket is closed correctly (in tearDown) - otherwise, an address binding error will happen
        threaded(() -> responder.respond(msg -> "greetings, " + msg));
        responder.waitRespondingStarted().get();
        Assert.assertEquals("greetings, universe", requester.apply("universe").get(3, SECONDS));
    }
    @org.junit.Test
    public void testWhile() {
        threaded(() -> responder.respondWhile(msg -> tuple("hello, " + msg, !msg.equals("STOP"))));
        responder.waitRespondingStarted().get();
        Assert.assertEquals("hello, world", requester.apply("world").get(3, SECONDS));
        Assert.assertTrue(responder.isRunning());
        Assert.assertEquals("hello, universe", requester.apply("universe").get(3, SECONDS));
        Assert.assertTrue(responder.isRunning());
        Assert.assertEquals("hello, STOP", requester.apply("STOP").get(3, SECONDS));
        responder.waitRespondingStopped().get(5, SECONDS);
        Assert.assertFalse(responder.isRunning());
    }
    @org.junit.Test
    public void testTimeout() {
        var timeout = 1000;
        threaded(() -> responder.respond(self::apply));
        responder.waitRespondingStarted().get();
        Assert.assertEquals("first", requester.apply("first").get(3, SECONDS));
        responder.stop().get();
        threaded(() -> responder.respond(msg -> {
            sleep(timeout + 500);
            return "";
        }));
        responder.waitRespondingStarted().get();
        try {
            requester.apply("second (to time out)").get(timeout, TimeUnit.MILLISECONDS);
            Assert.fail("response timeout exception must be raised");
        }
        catch (Exception ex) {/* as expected */}
        responder.stop().get();
        threaded(() -> responder.respond(self::apply));
        responder.waitRespondingStarted().get();
        Assert.assertEquals("third", requester.apply("third").get(3, SECONDS));
    }
}
