package jl95.net.rpc;

import jl95.net.io.IOStreamSupplier;
import jl95.net.io.SenderReceiver;
import jl95.net.io.managed.ManagedIOStreamSupplier;

public class BytesIOSRRequester extends IOSRRequester<byte[], byte[]> {

    public static BytesIOSRRequester of(SenderReceiver<byte[], byte[]> sr) {
        return new BytesIOSRRequester(sr);
    }
    public static BytesIOSRRequester of(IOStreamSupplier ios) {
        return of(SenderReceiver.fromIo(ios));
    }
    public static BytesIOSRRequester of(ManagedIOStreamSupplier ios) {
        return of(SenderReceiver.fromManagedIo(ios));
    }

    public BytesIOSRRequester(SenderReceiver<byte[],byte[]> sr,
                     int nrOfResponsesToWaitMax) {
        super(sr, nrOfResponsesToWaitMax);
    }
    public BytesIOSRRequester(SenderReceiver<byte[],byte[]> sr) {
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
