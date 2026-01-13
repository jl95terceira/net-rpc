package jl95.net.rpc;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import jl95.lang.variadic.*;
import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.*;

public class Responder extends GenericResponder<byte[], byte[]> {

    public static Responder of(SenderReceiverIf<byte[], byte[]> sr) {
        return new Responder(sr);
    }
    public static Responder of(Ios ios) {
        return of(SenderReceiverIf.fromIo(ios));
    }
    public static Responder of(ManagedIos ios) {
        return of(SenderReceiverIf.fromManagedIo(ios));
    }

    public Responder(SenderReceiverIf<byte[],byte[]> sr) {
        super(sr);
    }

    @Override protected byte[] deserialize(byte[] requestData) {
        return requestData;
    }

    @Override protected byte[] serialize(byte[] responseData) {
        return responseData;
    }
}
