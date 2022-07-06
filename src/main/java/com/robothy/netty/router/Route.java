package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Getter;

@Getter
class Route {

  private final HttpMethod method;

  private final String path;

  private final HttpRequestHandler handler;

  private Function<Map<CharSequence, String>, Boolean> headerMatcher;

  private Function<Map<CharSequence, List<String>>, Boolean> paramMatcher;

  private final String trimPath;

  public Route(HttpMethod method, String path, HttpRequestHandler handler) {
    this.method = method;
    this.path = path;
    this.handler = handler;
    this.trimPath = trimPath();
  }

  public Route headerMather(Function<Map<CharSequence, String>, Boolean> headerMatcher) {
    this.headerMatcher = headerMatcher;
    return this;
  }

  public Route paramMatcher(Function<Map<CharSequence, List<String>>, Boolean> paramMatcher) {
    this.paramMatcher = paramMatcher;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Route route = (Route) o;
    return method.equals(route.method) && trimPath.equals(route.trimPath) &&
        Objects.equals(headerMatcher, route.headerMatcher) && Objects.equals(paramMatcher, route.paramMatcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, trimPath, headerMatcher, paramMatcher);
  }

  private String trimPath() {
    String[] segments = path.split("/");
    StringBuilder result = new StringBuilder();
    for (String seg : segments) {
      if (seg.startsWith("{") && seg.endsWith("}")) {
        result.append("/{}");
      } else {
        result.append('/').append(seg);
      }
    }
    return result.toString();
  }
}
