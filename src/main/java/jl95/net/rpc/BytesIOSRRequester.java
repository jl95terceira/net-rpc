package jl95.net.rpc;

import jl95.net.io.BytesIStreamReceiver;
import jl95.net.io.BytesOStreamSender;
import jl95.net.io.IOStreamSupplier;
import jl95.net.io.ManagedIOStream;
import jl95.net.io.Receiver;
import jl95.net.io.Sender;

public class BytesIOSRRequester extends IOSRRequester<byte[], byte[]> {

    public static BytesIOSRRequester of(Sender<byte[]> sender, Receiver<byte[]> receiver) {
        return new BytesIOSRRequester(sender, receiver);
    }
    public static BytesIOSRRequester of(IOStreamSupplier ios) {
        return of(BytesOStreamSender.of(ios.getOutputStream()), BytesIStreamReceiver.of(ios.getInputStream()));
    }
    public static BytesIOSRRequester of(ManagedIOStream mios) {
        return of(BytesOStreamSender.of(mios.output()), BytesIStreamReceiver.of(mios.input()));
    }

    public BytesIOSRRequester(Sender<byte[]> sender, Receiver<byte[]> receiver,
                              int nrOfResponsesToWaitMax) {
        super(sender, receiver, nrOfResponsesToWaitMax);
    }
    public BytesIOSRRequester(Sender<byte[]> sender,Receiver<byte[]> receiver) {
        super(sender, receiver);
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
