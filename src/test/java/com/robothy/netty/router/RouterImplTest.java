package com.robothy.netty.router;

import static org.junit.jupiter.api.Assertions.*;
import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RouterImplTest {

  @Test
  void match() throws IllegalAccessException, NoSuchFieldException {
    RouterImpl router = new RouterImpl();
    HttpRequestHandler listHandler = Mockito.mock(HttpRequestHandler.class);
    assertThrows(IllegalArgumentException.class, () -> router.route(HttpMethod.GET, "", listHandler));
    assertThrows(IllegalArgumentException.class, () -> router.route(HttpMethod.GET, "/a/{}", listHandler));
    router.route(new Route(HttpMethod.GET, "/list", listHandler));
    HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder();
    HttpRequest listRequest = requestBuilder
        .method(HttpMethod.GET)
        .uri("/list?id=1")
        .path("/list")
        .params(new HashMap<>())
        .headers(new HashMap<>())
        .build();
    assertEquals(listHandler, router.match(listRequest));

    HttpRequestHandler notFoundHandler = Mockito.mock(HttpRequestHandler.class);
    assertEquals(RouterImpl.DEFAULT_NOT_FOUND_HANDLER, router.match(requestBuilder.path("/list/a").build()));
    router.notFound(notFoundHandler);
    assertEquals(notFoundHandler, router.match(requestBuilder.path("/").build()));
    assertEquals(notFoundHandler, router.match(requestBuilder.method(HttpMethod.PUT).path("/list").build()));
    assertEquals(notFoundHandler, router.match(requestBuilder.method(HttpMethod.PUT).path("/list/").build()));

    HttpRequestHandler actionHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(HttpMethod.GET, "/{action}", actionHandler);
    assertEquals(notFoundHandler, router.match(requestBuilder.path("/").build()));
    HttpRequest actionRequest = requestBuilder.method(HttpMethod.GET).path("/read").build();
    assertEquals(actionHandler, router.match(actionRequest));
    assertEquals("read", actionRequest.getParams().get("action").get(0));
    assertEquals(listHandler, router.match(requestBuilder.path("/list").build()));

    HttpRequestHandler emptyPathHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(HttpMethod.POST, "//", emptyPathHandler);
    assertEquals(emptyPathHandler, router.match(requestBuilder.method(HttpMethod.POST).path("/").build()));

    HttpRequestHandler postContentHandler = Mockito.mock(HttpRequestHandler.class);
    HttpRequestHandler postAHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(HttpMethod.POST, "/{id}/content", postContentHandler);
    router.route(HttpMethod.POST, "/a/content", postAHandler);
    HttpRequest postContentRequest = requestBuilder.method(HttpMethod.POST).path("/123/content").build();
    assertEquals(postContentHandler, router.match(postContentRequest));
    assertEquals(postAHandler, router.match(requestBuilder.path("/a/content").build()));

    HttpRequestHandler paramRequestHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(new Route(HttpMethod.HEAD, "/a/content", paramRequestHandler)
        .paramMatcher(ps -> ps.containsKey("version")));
    Map<CharSequence, List<String>> parameters = new HashMap<>();
    parameters.put("version", Collections.emptyList());
    assertEquals(notFoundHandler, router.match(requestBuilder.method(HttpMethod.HEAD).path("/a/content").build()));
    assertEquals(paramRequestHandler, router.match(requestBuilder.params(parameters).build()));

    HttpRequestHandler headerRequestHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(new Route(HttpMethod.HEAD, "/a/content", headerRequestHandler)
        .headerMather(hs -> hs.containsKey("hello")));
    assertEquals(paramRequestHandler, router.match(requestBuilder.method(HttpMethod.HEAD).path("/a/content").build()));
    Map<CharSequence, String> headers = new HashMap<>();
    headers.put("hello", "");
    // The header matcher has higher priority.
    assertEquals(headerRequestHandler, router.match(requestBuilder.headers(headers).build()));

    HttpRequestHandler headerParamHandler = Mockito.mock(HttpRequestHandler.class);
    router.route(new Route(HttpMethod.HEAD, "/a/content", headerParamHandler)
        .headerMather(hs -> hs.containsKey("hello"))
        .paramMatcher(ps -> ps.containsKey("version")));
    assertEquals(headerParamHandler, router.match(requestBuilder.build()));

    router.staticResource(Paths.get("src"));
    Field staticResourceRequestHandlerField = RouterImpl.class.getDeclaredField("staticResourceRequestHandler");
    staticResourceRequestHandlerField.setAccessible(true);
    var staticResourceRequestHandler = (HttpRequestHandler) staticResourceRequestHandlerField.get(router);

    assertEquals(staticResourceRequestHandler, router.match(requestBuilder
        .method(HttpMethod.GET)
        .path("/test/java/com/robothy/netty/router/RouterImplTest.java").build()));

    assertEquals(notFoundHandler, router.match(requestBuilder
        .method(HttpMethod.POST)
        .path("/test/java/com/robothy/netty/router/RouterImplTest.java").build()));
  }

  @Test
  void testExceptionHandler() {
    RouterImpl router = new RouterImpl();
    assertNotNull(router.findExceptionHandler(RuntimeException.class));
    // Both found the default exception handler.
    assertEquals(router.findExceptionHandler(RuntimeException.class), router.findExceptionHandler(IOException.class));

    ExceptionHandler<RuntimeException> runtimeExceptionHandler = Mockito.mock(ExceptionHandler.class);
    router.exceptionHandler(RuntimeException.class, runtimeExceptionHandler);
    assertEquals(runtimeExceptionHandler, router.findExceptionHandler(RuntimeException.class));

    class SubRuntimeException extends RuntimeException {

    }
    // Not register handler for SubRuntimeException, should return the handler for RuntimeException.
    assertEquals(runtimeExceptionHandler, router.findExceptionHandler(SubRuntimeException.class));
    ExceptionHandler<SubRuntimeException> subRuntimeExceptionHandler = Mockito.mock(ExceptionHandler.class);
    router.exceptionHandler(SubRuntimeException.class, subRuntimeExceptionHandler);
    assertEquals(subRuntimeExceptionHandler, router.findExceptionHandler(SubRuntimeException.class));
  }

}