package jl95.net.rpc;

import jl95.net.io.IOStreamSupplier;
import jl95.net.io.SenderReceiver;
import jl95.net.io.managed.ManagedIOStreamSupplier;

public class BytesIOSRResponder extends IOSRResponder<byte[], byte[]> {

    public static BytesIOSRResponder of(SenderReceiver<byte[], byte[]> sr) {
        return new BytesIOSRResponder(sr);
    }
    public static BytesIOSRResponder of(IOStreamSupplier ios) {
        return of(SenderReceiver.fromIo(ios));
    }
    public static BytesIOSRResponder of(ManagedIOStreamSupplier ios) {
        return of(SenderReceiver.fromManagedIo(ios));
    }

    public BytesIOSRResponder(SenderReceiver<byte[],byte[]> sr) {
        super(sr);
    }

    @Override protected byte[] deserialize(byte[] requestData) {
        return requestData;
    }

    @Override protected byte[] serialize(byte[] responseData) {
        return responseData;
    }
}
