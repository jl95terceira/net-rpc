package jl95.net.rpc.demo;

import static jl95.net.Util.getSocketByConnect;
import static jl95.net.io.Util.getIoFromSocket;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import jl95.net.rpc.BytesIOSRRequester;
import jl95.net.rpc.IOSRRequester;
import jl95.net.rpc.collections.RequesterAdaptersCollection;

public class Request {
    public static void main(String[] args) throws Exception {
        var sock = getSocketByConnect(new InetSocketAddress("127.0.0.1", 4243));
        var requester = RequesterAdaptersCollection
                .asStringRequester(BytesIOSRRequester
                        .of(getIoFromSocket(sock)));
        var scanner = new Scanner(System.in);
        var nEmpty = 0;
        while (true) {
            System.out.print(">>> ");
            var msg = scanner.nextLine();
            if (msg.isEmpty()) {
                nEmpty++;
                if (nEmpty >= 2) {
                    break;
                }
                continue;
            }
            nEmpty = 0;
            String response;
            try {
                response = requester.apply(msg).get(3, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                continue;
            }
            System.out.println("<<< " + response);
        }
        if (requester instanceof IOSRRequester<?,?> iosrRequester) {
            if (iosrRequester.isAcceptingResponses()) {
                iosrRequester.acceptResponses(false).get();
            }
        }
        requester.close();
        System.out.println("Done.");
    }
}
