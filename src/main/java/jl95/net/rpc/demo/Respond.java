package jl95.net.rpc.demo;

import jl95.net.rpc.BytesIOSRResponder;
import jl95.net.rpc.collections.ResponderAdaptersCollection;

import java.net.InetSocketAddress;

import static jl95.lang.SuperPowers.tuple;
import static jl95.net.Util.getSocketByAccept;
import static jl95.net.io.Util.getIoFromSocket;

public class Respond {
    public static void main(String[] args) throws Exception {
        var sock = getSocketByAccept(new InetSocketAddress("127.0.0.1", 4243));
        System.out.println("Accepted");
        var responder = ResponderAdaptersCollection
                .asStringResponder(BytesIOSRResponder
                        .of(getIoFromSocket(sock)));
        responder.respond(request -> {
            System.out.println("<<< " + request);
            var response = "\""+request.replace("\"","\\\"")+"\"" + "has "+request.length()+" characters";
            System.out.println(">>> " + response);
            return response;
        });
        responder.close();
        System.out.println("Done.");
    }
}
