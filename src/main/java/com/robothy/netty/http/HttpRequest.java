package com.robothy.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HttpRequest {

  @Builder.Default
  private Map<CharSequence, String> headers = new HashMap<>();

  @Builder.Default
  private Map<CharSequence, List<String>> params = new HashMap<>();

  private String path;

  private String uri;

  private HttpMethod method;

  private ByteBuf body;

  private HttpVersion httpVersion;

  /**
   * Get the header value by name.
   *
   * @param name header name.
   * @return the header value.
   */
  public Optional<String> header(CharSequence name) {
    return Optional.ofNullable(headers.get(name));
  }

  /**
   * Get the first value by the parameter name.
   *
   * @param name parameter name.
   * @return the first value of the parameter.
   */
  public Optional<String> parameter(String name) {
    if (params.containsKey(name) && params.get(name).size() > 0) {
      return Optional.ofNullable(params.get(name).get(0));
    }
    return Optional.empty();
  }

  /**
   * Get the parameter values by name. Include path parameters and query parameters.
   *
   * @param name parameter name.
   * @return the parameter values.
   */
  public Optional<List<String>> parameters(String name) {
    return Optional.ofNullable(params.get(name));
  }

}
