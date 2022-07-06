package com.robothy.netty.router;


import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.nio.file.Path;

/**
 * A Router is a S3RequestHandler container. The route path must starts with a '/'.
 *
 * "/", "//", "" matches route("/")
 * "/a" matches route("/a") firstly, then match "/{param}"
 *
 */
public interface Router {

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
    return route(new Route(method, path, handler));
  }

  Router notFound(HttpRequestHandler handler);

  /**
   * Route for static resources. This method could be invoked multiple times for
   * different resources. The router will map the request uri to the relative path
   * of files under the {@code rootPath} directory.
   *
   * <ul>
   *   <li/> If {@code rootPath} is a directory, the router will map the request uri
   *   to relative path of files under the {@code rootPath} directory.
   *
   *   <li/> If the {@code rootPath} is a file, then its parent is a static resource root.
   *
   *   <li/> If static resource paths conflict with routes registered via {@code route()},
   *   the router will ignore static resources.
   * </ul>
   *
   *
   * @param rootPath static resources root directory or resource path.
   * @return this.
   */
  Router staticResource(Path rootPath);

  /**
   * Find a handler for the given request according to registered routes.
   *
   * @param request HTTP request.
   * @return a matched handler; or {@code null} if no matched handlers.
   */
  HttpRequestHandler match(HttpRequest request);




}
