package com.robothy.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class HttpResponse {

  private final Map<String, String> headers = new HashMap<>();

  private HttpResponseStatus status;

  private final CompositeByteBuf body = Unpooled.compositeBuffer();

  public HttpResponse write(String content) {
    if (content != null) {
      write(content.getBytes(StandardCharsets.UTF_8));
    }
    return this;
  }

  public HttpResponse write(byte[] bytes) {
    body.addComponent(true, Unpooled.copiedBuffer(bytes));
    return this;
  }

  public HttpResponse write(ByteBuf buf) {
    body.addComponent(true, buf);
    return this;
  }

  public HttpResponse status(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  public HttpResponse putHeader(String key, Object value) {
    headers.put(key, String.valueOf(value));
    return this;
  }

}
