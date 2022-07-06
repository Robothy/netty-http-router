package com.robothy.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HttpRequest {

  private Map<CharSequence, String> headers;

  private Map<CharSequence, List<String>> params;

  private String path;

  private String uri;

  private HttpMethod method;

  private ByteBuf body;

  private HttpVersion httpVersion;

  public String header(CharSequence name) {
    return headers.get(name);
  }

  public String parameter(String name) {
    if (params == null || params.size() == 0) {
      return null;
    }
    return params.get(name).get(0);
  }

  public List<String> parameters(String name) {
    return params.get(name);
  }

}
