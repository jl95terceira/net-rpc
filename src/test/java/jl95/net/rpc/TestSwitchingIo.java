package jl95.net.rpc;

import static jl95.lang.SuperPowers.I;
import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.sleep;
import static jl95.lang.SuperPowers.tuple;
import static jl95.net.rpc.Test.threaded;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import jl95.lang.I;
import jl95.net.io.CloseableIOStreamSupplier;
import jl95.net.io.managed.SwitchingRetriableClientIOStream;
import jl95.net.io.managed.SwitchingRetriableIOStream;
import jl95.net.rpc.collections.RequesterAdaptersCollection;
import jl95.net.rpc.collections.ResponderAdaptersCollection;
import jl95.util.UFuture;
import jl95.util.UVoidFuture;

public class TestSwitchingIo {

    public static InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1", 42421);
    public static InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1", 42422);

    SwitchingRetriableIOStream ioAsClient;
    CloseableIOStreamSupplier ioAsServer1;
    CloseableIOStreamSupplier ioAsServer2;
    Requester<String, String> requester;
    Responder<String, String> responder1;
    Responder<String, String> responder2;

    @org.junit.Before
    public void setUp() throws Exception {
        var responder1Future = CompletableFuture.supplyAsync(() -> {
            ioAsServer1 = Util.getIoAsServer(addr1);
            return ResponderAdaptersCollection.asStringResponder(BytesIOSRResponder.of(ioAsServer1));
        }, (task) -> new Thread(task).start());
        var responder2Future = CompletableFuture.supplyAsync(() -> {
            ioAsServer2 = Util.getIoAsServer(addr2);
            return ResponderAdaptersCollection.asStringResponder(BytesIOSRResponder.of(ioAsServer2));
        }, (task) -> new Thread(task).start());
        sleep(50);
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = SwitchingRetriableClientIOStream.of(addr1, addr2);
            return RequesterAdaptersCollection.asStringRequester(BytesIOSRRequester.of(ioAsClient));
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
        for (var responder: I(responder1, responder2)) {
            threaded(() -> responder.respond(self::apply));
            responder.waitRespondingStarted().get();
            Assert.assertTrue(responder.isRunning());
            responder.stop().get();
        }
    }
    @org.junit.Test
    public void testStartStopTwice() {
        I.range(2).forEach(i -> testStartStop());
    }
    @org.junit.Test
    public void test() {
        threaded(() -> responder1.respond(msg -> "hello, " + msg));
        responder1.waitRespondingStarted().get();
        org.junit.Assert.assertEquals("hello, world", requester.apply("world").get(3, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void test2() { // to confirm that the server socket is closed correctly (in tearDown) - otherwise, an address binding error will happen
        threaded(() -> responder1.respond(msg -> "greetings, " + msg));
        responder1.waitRespondingStarted().get();
        org.junit.Assert.assertEquals("greetings, universe", requester.apply("universe").get(3, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void testTimeout() {
        var timeout = 1000;
        threaded(() -> responder1.respond(self::apply));
        responder1.waitRespondingStarted().get();
        org.junit.Assert.assertEquals("first", requester.apply("first").get(3, TimeUnit.SECONDS));
        responder1.stop().get();
        threaded(() -> responder1.respond(msg -> {
            sleep(timeout + 500);
            return "";
        }));
        responder1.waitRespondingStarted().get();
        try {
            requester.apply("second (to time out)").get(timeout, TimeUnit.MILLISECONDS);
            org.junit.Assert.fail("response timeout exception must be raised");
        }
        catch (Exception ex) {/* as expected */}
        responder1.stop().get();
        threaded(() -> responder1.respond(self::apply));
        responder1.waitRespondingStarted().get();
        org.junit.Assert.assertEquals("third", requester.apply("third").get(3, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void testSwitch() {
        for (var t: I(tuple("1", responder1),
                      tuple("2", responder2))) {
            var responder = t.a2;
            threaded(() -> responder.respond(s -> t.a1+":"+s));
            responder.waitRespondingStarted().get();
        }
        org.junit.Assert.assertEquals("1:test1", requester.apply("test1").get(3, TimeUnit.SECONDS));
        responder1.stop().get();
        ioAsServer1.close();
        // switch to responder 2
        org.junit.Assert.assertEquals("2:test2", requester.apply("test2").get(3, TimeUnit.SECONDS));
        var responder1Future = CompletableFuture.supplyAsync(() -> {
            ioAsServer1 = Util.getIoAsServer(addr1);
            return ResponderAdaptersCollection.asStringResponder(BytesIOSRResponder.of(ioAsServer1));
        }, (task) -> new Thread(task).start());
        responder1 = UFuture.of(responder1Future).get();
        threaded(() -> responder1.respond(s -> "foo:"+s));
        responder1.waitRespondingStarted().get();
        responder2.stop().get();
        ioAsServer2.close();
        // switch back to responder 1
        org.junit.Assert.assertEquals("foo:test3", requester.apply("test3").get(3, TimeUnit.SECONDS));
    }
}
