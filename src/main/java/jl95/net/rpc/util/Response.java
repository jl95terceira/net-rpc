package jl95.net.rpc.util;

import java.util.UUID;

import javax.json.JsonValue;

public class Response {

    public UUID      id;
    public UUID      requestId;
    public JsonValue payload;
}
