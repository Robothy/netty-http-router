package com.robothy.netty.router;


import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;

/**
 * A Router is a S3RequestHandler container. The route path must starts with a '/'.
 *
 * "/", "//", "" matches route("/")
 * "/a" matches route("/a") firstly, then match "/{param}" and pass 'a' as value of `param`.
 *
 */
public interface Router {

  /**
   * Create a default Router instance.
   */
  static Router router() {
    return new RouterImpl();
  }

  /**
   * Register a handler for the request that matched rule.
   *
   * @param rule match conditions
   * @return this
   */
  Router route(Route rule);

  default Router route(HttpMethod method, String path, HttpRequestHandler handler) {
    Route rule = Route.builder()
        .method(method)
        .path(path)
        .handler(handler)
        .build();
    return route(rule);
  }

  /**
   * Set resource not found handler.
   *
   * @return this
   */
  Router notFound(HttpRequestHandler handler);

  /**
   * The router will map the request uri to the relative path of files under the {@code rootPath}
   * directory. By default, the router loads static resources from `classpath:static`.
   *
   * <ul>
   *   <li/> The {@code rootPath} must be a valid directory, the router will map the request uri
   *   to relative path of files under the {@code rootPath} directory.
   *
   *   <li/> If static resource paths conflict with routes registered via {@code route()},
   *   the router will ignore static resources.
   * </ul>
   *
   *
   * @param rootPath static resources root directory or resource path.
   * @return this.
   */
  Router staticResource(String rootPath);

  /**
   * Set a handler for exceptions with {@code exceptionType}.
   *
   * @param exceptionType subtype of Throwable.
   * @param handler handle specific exception.
   * @return this.
   * @param <T> type of the exception to handle..
   */
  <T extends Throwable> Router exceptionHandler(Class<T> exceptionType, ExceptionHandler<T> handler);

  /**
   * Find the best match exception handler for the given {@code exceptionType}.
   */
  ExceptionHandler<Throwable> findExceptionHandler(Class<? extends Throwable> exceptionType);

  /**
   * Find a handler for the given request according to registered routes.
   *
   * @param request HTTP request.
   * @return a matched handler; or {@code null} if no matched handlers.
   */
  HttpRequestHandler match(HttpRequest request);

}
