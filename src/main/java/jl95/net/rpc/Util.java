package jl95.net.rpc;

import static jl95.lang.SuperPowers.uncheck;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jl95.net.Server;
import jl95.net.io.CloseableIos;
import jl95.net.io.util.Defaults;
import jl95.util.*;

public class Util {

        public static CloseableIos getIoFromSocket(Socket            socket) {
            return new CloseableIos() {
                @Override public InputStream getInputStream() { return uncheck(socket::getInputStream); }
                @Override public OutputStream getOutputStream() { return uncheck(socket::getOutputStream); }
                @Override public void         close () {
                    if (!socket.isClosed()) {
                        uncheck(socket::close);
                    }
                }
            };
        }
        public static CloseableIos getIoAsClient  (InetSocketAddress addr) {
            var socket = new Socket();
            uncheck(() -> socket.connect(addr));
            return getIoFromSocket(socket);
        }
        public static CloseableIos getIoAsServer  (InetSocketAddress addr,
                                         Optional<Integer> clientConnectionTimeoutMs) {
            var clientSocketFuture = new CompletableFuture<Socket>();
            var server = Server.fromSocket(jl95.net.Util.getSimpleServerSocket(addr, Defaults.acceptTimeoutMs));
            server.setAcceptCb((self, socket) -> {
                clientSocketFuture.complete(socket);
            });
            server.start();
            var clientSocket = uncheck(() -> clientConnectionTimeoutMs.isPresent()
                                           ? clientSocketFuture.get(clientConnectionTimeoutMs.get(), TimeUnit.MILLISECONDS)
                                           : clientSocketFuture.get());
            server.stop().await(); // stop server right away - no need to accept more connections
            uncheck(server.getSocket()::close); // release bind address
            return getIoFromSocket(clientSocket);
        }
        public static CloseableIos getIoAsServer  (InetSocketAddress addr) { return getIoAsServer(addr, Optional.empty()); }
}
