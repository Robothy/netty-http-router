package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Getter;

@Getter
public class Route {

  private HttpMethod method;

  private String path;

  private HttpRequestHandler handler;

  private Function<Map<CharSequence, String>, Boolean> headerMatcher;

  private Function<Map<CharSequence, List<String>>, Boolean> paramMatcher;

  private String trimPath;

  private Route() {

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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    Route propHolder = new Route();

    public Builder method(HttpMethod method) {
      propHolder.method = method;
      return this;
    }

    public Builder path(String path) {
      propHolder.path = path;
      return this;
    }

    public Builder headerMatcher(Function<Map<CharSequence, String>, Boolean> headerMatcher) {
      propHolder.headerMatcher = headerMatcher;
      return this;
    }

    public Builder paramMatcher(Function<Map<CharSequence, List<String>>, Boolean> paramMatcher) {
      propHolder.paramMatcher = paramMatcher;
      return this;
    }

    public Builder handler(HttpRequestHandler handler) {
      propHolder.handler = handler;
      return this;
    }

    private String trimPath(String path) {
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

    public Route build() {
      Objects.requireNonNull(propHolder.method, "'method' is required.");
      Objects.requireNonNull(propHolder.path, "'path' is required.");
      Objects.requireNonNull(propHolder.handler, "'handler'' is required.");
      Route route = new Route();
      route.method = propHolder.method;
      route.path = propHolder.path;
      route.handler = propHolder.handler;
      route.paramMatcher = propHolder.paramMatcher;
      route.headerMatcher = propHolder.headerMatcher;
      route.trimPath = trimPath(route.path);
      return route;
    }

  }

}
