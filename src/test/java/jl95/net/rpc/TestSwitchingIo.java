package jl95.net.rpc;

import static jl95.lang.SuperPowers.I;
import static jl95.lang.SuperPowers.constant;
import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.sleep;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jl95.net.io.CloseableIos;
import jl95.net.io.managed.SwitchingRetriableClientIos;
import jl95.net.io.managed.SwitchingRetriableIos;
import jl95.net.rpc.collections.RequesterAdaptersCollection;
import jl95.net.rpc.collections.ResponderAdaptersCollection;
import org.junit.rules.Timeout;

public class TestSwitchingIo {

    public static InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1", 42421);
    public static InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1", 42422);

    SwitchingRetriableIos ioAsClient;
    CloseableIos ioAsServer1;
    CloseableIos ioAsServer2;
    RequesterIf<String, String> requester;
    ResponderIf<String, String> responder1;
    ResponderIf<String, String> responder2;

    @org.junit.Before
    public void setUp() throws Exception {
        var responder1Future = CompletableFuture.supplyAsync(() -> {
            ioAsServer1 = Util.getIoAsServer(addr1);
            return ResponderAdaptersCollection.asStringPostGetResponder(Responder.fromIo(ioAsServer1));
        }, (task) -> new Thread(task).start());
        var responder2Future = CompletableFuture.supplyAsync(() -> {
            ioAsServer2 = Util.getIoAsServer(addr2);
            return ResponderAdaptersCollection.asStringPostGetResponder(Responder.fromIo(ioAsServer2));
        }, (task) -> new Thread(task).start());
        sleep(50);
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = SwitchingRetriableClientIos.of(addr1, addr2);
            return RequesterAdaptersCollection.asStringPostGetRequester(Requester.fromManagedIo(ioAsClient));
        }, (task) -> new Thread(task).start());
        requester = requesterFuture.get();
        responder1 = responder1Future.get();
        responder2 = responder2Future.get();
    }
    @org.junit.After
    public void tearDown() {
        for (var responder: I(responder1, responder2)) {
            if (responder.isRunning()) {
                responder.ensureStopped();
            }
        }
        if (ioAsClient  != null) ioAsClient.close();
        for (var io: I(ioAsServer1, ioAsServer2)) {
            if (io != null) {
                io.close();
            }
        }
    }

    @org.junit.Test
    public void testStartStop() {
        responder1.respond(self::apply).get();
        responder1.stop ()             .get();
        responder1.respond(self::apply).get();
        responder1.stop ()             .get();
    }
    @org.junit.Test
    public void test() throws ExecutionException, InterruptedException, TimeoutException {
        responder1.respond(msg -> "hello, " + msg).get();
        org.junit.Assert.assertEquals("hello, world", requester.apply("world").get(5, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void test2() throws ExecutionException, InterruptedException, TimeoutException { // to confirm that the server socket is closed correctly (in tearDown) - otherwise, an address binding error will happen
        responder1.respond(msg -> "greetings, " + msg).get();
        org.junit.Assert.assertEquals("greetings, universe", requester.apply("universe").get(5, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void testTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        var timeout = 1000;
        responder1.respond(self::apply).get();
        org.junit.Assert.assertEquals("first", requester.apply("first").get(5, TimeUnit.SECONDS));
        responder1.stop().get();
        responder1.respond(msg -> {
            sleep(timeout + 500);
            return "";
        }).get();
        try {
            requester.apply("second (to time out)").get(timeout, TimeUnit.MILLISECONDS);
            org.junit.Assert.fail("response timeout exception must be raised");
        }
        catch (Exception ex) {/* as expected */}
        responder1.stop ()           .get();
        responder1.respond(self::apply).get();
        org.junit.Assert.assertEquals("third", requester.apply("third").get(5, TimeUnit.SECONDS));
    }
}
