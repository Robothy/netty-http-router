package com.robothy.netty.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SpringWebRouterTest {

  @MethodSource("pathMatchCases")
  @ParameterizedTest
  public void pathMatch(String pattern, String path, boolean isMatch) {
    SpringWebRouter router = new SpringWebRouter();
    HttpRequestHandler handler = (request, response) -> {};
    router.route(HttpMethod.GET, pattern, handler);
    HttpRequest request = HttpRequest.builder()
        .method(HttpMethod.GET)
        .path(path)
        .build();
    assertEquals(isMatch, !Router.DEFAULT_NOT_FOUND_HANDLER.equals(router.match(request)));
  }

  static Stream<Arguments> pathMatchCases() {
    return Stream.of(
        arguments("/a/{b}/c", "/a/b", false),
        arguments("/a/{b}/c", "/a/b/c", true),
        arguments("/a/{*b}", "/a/b/c", true),
        arguments("/a/{b:[a-z]{1,}}/c", "/a/b/c", true), // Regex
        arguments("/a/{b:[a-z]{1,}}/c", "/a/A/c", false),
        arguments("/a/{b:[a-z]{1,}}/c", "/a/123/c", false)
    );
  }

  @Test
  public void headerMatherHasHigherPriorityThenParameterMatcher() {
    SpringWebRouter router = new SpringWebRouter();

    HttpRequestHandler handler1 = (request, response) -> {};
    HttpRequestHandler handler2 = (request, response) -> {};

    router.route(Route.builder()
            .method(HttpMethod.GET)
            .path("/a")
            .headerMatcher(header -> header.containsKey("X-Info"))
            .handler(handler1)
        .build());

    router.route(Route.builder()
            .method(HttpMethod.GET)
            .path("/a")
            .paramMatcher(params -> params.containsKey("info"))
            .handler(handler2)
        .build());

    HttpRequestHandler matchedHandler = router.match(HttpRequest.builder()
        .method(HttpMethod.GET)
        .path("/a")
        .params(Map.of("info", Collections.emptyList()))
        .headers(Map.of("X-Info", ""))
        .build());
    assertEquals(handler1, matchedHandler);
  }



}