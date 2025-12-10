package jl95.net.rpc;

import static jl95.lang.SuperPowers.self;
import static jl95.lang.SuperPowers.sleep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jl95.net.io.CloseableIos;
import jl95.net.rpc.collections.RequesterAdaptersCollection;
import jl95.net.rpc.collections.ResponderAdaptersCollection;
import jl95.net.rpc.switched.TypeSwitchedResponder;
import jl95.net.rpc.switched.TypeSwitchedResponderIf;
import jl95.net.rpc.switched.TypedRequester;
import jl95.net.rpc.switched.TypedRequesterIf;

public class TestTypeSwitched {

    CloseableIos ioAsServer;
    CloseableIos ioAsClient;
    TypedRequesterIf<String, String> requester;
    TypeSwitchedResponderIf<String, String> responder;

    @org.junit.Before
    public void setUp() throws Exception {
        var requesterFuture = CompletableFuture.supplyAsync(() -> {
            ioAsServer = Util.getIoAsServer(jl95.net.io.util.Defaults.serverAddr);
            return RequesterAdaptersCollection.asStringPostGetRequester(TypedRequester.fromSimpleRpc(Requester.fromIo(ioAsServer)));
        }, (task) -> new Thread(task).start());
        sleep(50);
        var responderFuture = CompletableFuture.supplyAsync(() -> {
            ioAsClient = Util.getIoAsClient(jl95.net.io.util.Defaults.serverAddr);
            return ResponderAdaptersCollection.asStringPostGetResponder(TypeSwitchedResponder.fromIo(ioAsClient));
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
    public void test() throws ExecutionException, InterruptedException, TimeoutException {
        responder.addCase("hello"  , msg -> "hello, " + msg);
        responder.addCase("bye"    , msg -> "bye, "   + msg);
        responder.adapted(Integer::parseInt, Object::toString).addCase("answer" , i -> i.equals(42));
        responder.start().await();
        org.junit.Assert.assertEquals("hello, world", requester.getFunction("hello").apply("world").get(5, TimeUnit.SECONDS));
        var intRequester = requester.adapted((Integer i) -> i.toString(), Boolean::parseBoolean);
        org.junit.Assert.assertEquals(Boolean.FALSE , intRequester.getFunction("answer").apply(100).get(5, TimeUnit.SECONDS));
        org.junit.Assert.assertEquals(Boolean.TRUE  , intRequester.getFunction("answer").apply(42).get(5, TimeUnit.SECONDS));
        org.junit.Assert.assertEquals("bye, world"  , requester.getFunction("bye").apply("world").get(5, TimeUnit.SECONDS));
    }
    @org.junit.Test
    public void testStartStop() {
        org.junit.Assert.assertFalse(responder.isRunning());
        responder.start().await();
        org.junit.Assert.assertTrue (responder.isRunning());
        responder.stop().await();
        org.junit.Assert.assertFalse(responder.isRunning());
        responder.start().await();
        org.junit.Assert.assertTrue (responder.isRunning());
    }
}
