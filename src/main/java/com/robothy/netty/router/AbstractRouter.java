package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractRouter implements Router {


  private final Map<Class<? extends Throwable>, ExceptionHandler<?>> exceptionHandlerMap;

  {
    exceptionHandlerMap = new HashMap<>();
    exceptionHandlerMap.put(Throwable.class, (cause, request, response) -> {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(out);
      cause.printStackTrace(printStream);
      response.write(out.toString())
          .putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.TEXT_PLAIN)
          .status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    });
  }

  private HttpRequestHandler notFoundHandler = DEFAULT_NOT_FOUND_HANDLER;

  private StaticResourceMatcher staticResourceMatcher
      = StaticResourceMatcher.create("classpath:static");

  @Override
  public Router notFound(HttpRequestHandler handler) {
    this.notFoundHandler = handler;
    return this;
  }

  @Override
  public Router staticResource(String rootPath) {
    this.staticResourceMatcher = StaticResourceMatcher.create(rootPath);
    return this;
  }

  protected HttpRequestHandler notFoundHandler() {
    return this.notFoundHandler;
  }

  protected StaticResourceMatcher staticResourceMatcher() {
    return this.staticResourceMatcher;
  }

  @Override
  public <T extends Throwable> Router exceptionHandler(Class<T> exceptionType, ExceptionHandler<T> handler) {
    Objects.requireNonNull(handler, "The exception handler shouldn't be null.");
    exceptionHandlerMap.put(exceptionType, handler);
    return this;
  }

  @Override
  public ExceptionHandler<Throwable> findExceptionHandler(Class<? extends Throwable> exceptionType) {
    ExceptionHandler<? extends Throwable> exceptionHandler = null;
    Class<?> type = exceptionType;
    while ( (exceptionHandler = exceptionHandlerMap.get(type)) == null) {
      type = type.getSuperclass();
    }
    //noinspection unchecked
    return (ExceptionHandler<Throwable>) exceptionHandler;
  }


}
