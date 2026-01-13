package jl95.net.rpc;

import static jl95.lang.SuperPowers.strict;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import jl95.net.io.Ios;
import jl95.net.io.ReceiverIf;
import jl95.net.io.SenderIf;
import jl95.net.io.SenderReceiverIf;
import jl95.net.io.managed.ManagedIos;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;
import jl95.util.StrictMap;
import jl95.util.UFuture;
import jl95.util.UVoidFuture;

public class Requester extends GenericRequester<byte[], byte[]> {

    public static Requester of(SenderReceiverIf<byte[], byte[]> sr) {
        return new Requester(sr);
    }
    public static Requester of(Ios ios) {
        return of(SenderReceiverIf.fromIo(ios));
    }
    public static Requester of(ManagedIos ios) {
        return of(SenderReceiverIf.fromManagedIo(ios));
    }

    public Requester(SenderReceiverIf<byte[],byte[]> sr,
                     int nrOfResponsesToWaitMax) {
        super(sr, nrOfResponsesToWaitMax);
    }
    public Requester(SenderReceiverIf<byte[],byte[]> sr) {
        super(sr);
    }

    @Override
    protected byte[] serialize(byte[] requestData) {
        return requestData;
    }

    @Override
    protected byte[] deserialize(byte[] responseData) {
        return responseData;
    }

}
