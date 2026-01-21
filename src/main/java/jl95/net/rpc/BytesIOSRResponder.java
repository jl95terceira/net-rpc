package jl95.net.rpc;

import jl95.net.io.BytesIStreamReceiver;
import jl95.net.io.BytesOStreamSender;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.ManagedIOStream;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;

public class BytesIOSRResponder extends IOSRResponder<byte[], byte[]> {

    public static BytesIOSRResponder of(Receiver<byte[]> receiver, Sender<byte[]> sender) {
        return new BytesIOSRResponder(receiver, sender);
    }
    public static BytesIOSRResponder of(IOStreamSupplier ios) {
        return of(BytesIStreamReceiver.of(ios.getInputStream()), BytesOStreamSender.of(ios.getOutputStream()));
    }
    public static BytesIOSRResponder of(ManagedIOStream mios) {
        return of(BytesIStreamReceiver.of(mios.input()), BytesOStreamSender.of(mios.output()));
    }

    public BytesIOSRResponder(Receiver<byte[]> receiver,
                              Sender  <byte[]> sender) {
        super(receiver, sender);
    }

    @Override protected byte[] deserialize(byte[] requestData) {
        return requestData;
    }

    @Override protected byte[] serialize(byte[] responseData) {
        return responseData;
    }
}
