package jl95.net.rpc.util;

import java.util.Base64;

import javax.json.JsonValue;

import jl95.lang.variadic.Function1;
import jl95.net.rpc.util.serdes.RequestJsonSerdes;
import jl95.net.rpc.util.serdes.ResponseJsonSerdes;
import jl95.serdes.JsonFromString;
import jl95.serdes.JsonToString;
import jl95.serdes.StringFromJson;
import jl95.serdes.StringToJson;
import jl95.serdes.StringUTF8FromBytes;
import jl95.serdes.StringUTF8ToBytes;

public class SerdesDefaults {

    public static final Function1<byte[], String>
                                    stringToBytes            = StringUTF8ToBytes.get();
    public static final Function1<String, byte[]>
                                    stringFromBytes          = StringUTF8FromBytes.get();
    public static final Function1<JsonValue, String>
                                    stringToJson             = StringToJson.get();
    public static final Function1<String, JsonValue>
                                    stringFromJson           = StringFromJson.get();
    public static final Function1<String, JsonValue>
                                    jsonToString             = JsonToString.get();
    public static final Function1<JsonValue, String>
                                    jsonFromString           = JsonFromString.get();
    public static final Function1<byte[], JsonValue>
                                    jsonToBytes              = json   -> stringToBytes.call
                                                                        (jsonToString .call(json));
    public static final Function1<JsonValue, byte[]>
                                    jsonFromBytes            = serial -> jsonFromString .call
                                                                        (stringFromBytes.call(serial));
    public static final Function1<String, byte[]>
                                    bytesToString            = Base64.getEncoder()::encodeToString;
    public static final Function1<byte[], String>
                                    bytesFromString          = Base64.getDecoder()::decode;
    public static final Function1<byte[], Request>
                                    requestToBytes           = r -> jsonToBytes.apply(RequestJsonSerdes.toJson(r));
    public static final Function1<Request, byte[]>
                                    requestFromBytes         = b -> RequestJsonSerdes.fromJson(jsonFromBytes.apply(b));
    public static final Function1<byte[], Response>
                                    responseToBytes          = r -> jsonToBytes.apply(ResponseJsonSerdes.toJson(r));
    public static final Function1<Response, byte[]>
                                    responseFromBytes        = b -> ResponseJsonSerdes.fromJson(jsonFromBytes.apply(b));
}
