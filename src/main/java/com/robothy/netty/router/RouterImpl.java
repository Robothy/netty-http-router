package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import com.robothy.netty.http.HttpResponse;
import com.robothy.netty.router.utils.MimeTypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

final class RouterImpl implements Router {

  static final HttpRequestHandler DEFAULT_NOT_FOUND_HANDLER = (request, response) -> {
    response.status(HttpResponseStatus.NOT_FOUND);
    response.write("Netty HTTP Router: 404 Not Found.");
  };

  private final Set<Route> ruleSet = new HashSet<>();

  private final TreeNode root = new TreeNode();

  private HttpRequestHandler notFoundHandler;

  private StaticResourceRequestHandler staticResourceRequestHandler;


  @Override
  public Router route(Route route) {
    if (ruleSet.contains(route)) {
      throw new IllegalArgumentException("The router already has a handler for route " + route);
    }
    ruleSet.add(route);

    TreeNode node = addNode(root, route.getMethod().name());
    String[] segments = splitPath(route.getPath());
    for (String segment : segments) {
      node = addNode(node, segment);
    }
    node.routes.add(route);
    return this;
  }

  @Override
  public Router notFound(HttpRequestHandler handler) {
    this.notFoundHandler = handler;
    return this;
  }

  @Override
  public Router staticResource(Path resourceDirectory) {
    this.staticResourceRequestHandler = new StaticResourceRequestHandler(resourceDirectory);
    return this;
  }

  private HttpRequestHandler notFoundHandler() {
    return this.notFoundHandler != null ? this.notFoundHandler : DEFAULT_NOT_FOUND_HANDLER;
  }

  private TreeNode addNode(TreeNode parent, String path) {
    TreeNode child = new TreeNode();
    if (path.startsWith("{") && path.endsWith("}")) {
      if (path.length() == 2) {
        throw new IllegalArgumentException("The path variable name cannot be empty.");
      }
      if (parent.likeChild == null) {
        parent.likeChild = child;
      }
      return parent.likeChild;
    } else {
      if (!parent.exactChildren.containsKey(path)) {
        parent.exactChildren.put(path, child);
      }
      return parent.exactChildren.get(path);
    }
  }


  @Override
  public HttpRequestHandler match(HttpRequest request) {
    HttpRequestHandler handler;
    if (null != (handler = matchHandler(request)) || null != (handler = matchStaticResource(request))) {
      return handler;
    }
    return this.notFoundHandler();
  }

  private HttpRequestHandler matchHandler(HttpRequest request) {
    String[] segments = splitPath(request.getPath());
    TreeNode node = root.exactChildren.get(request.getMethod().name());
    if (node == null) {
      return null;
    }

    int idx = 0;
    for (; idx < segments.length; idx++) {
      TreeNode tmp = getNode(node, segments[idx]);
      if (tmp == null) {
        break;
      }
      node = tmp;
    }

    if (idx != segments.length) {
      return null;
    }

    Route result = null;
    for (Route route : node.routes) {
      boolean headerMatched = (route.getHeaderMatcher() == null || route.getHeaderMatcher().apply(request.getHeaders()));
      boolean paramMatched = (route.getParamMatcher() == null || route.getParamMatcher().apply(request.getParams()));
      if (headerMatched && paramMatched) {
        result = route;
        break;
      }
    }

    if (result == null) {
      return null;
    }

    request.getParams().putAll(parsePathParams(result.getPath(), request.getPath()));
    return result.getHandler();
  }

  private HttpRequestHandler matchStaticResource(HttpRequest request) {
    if (this.staticResourceRequestHandler == null || request.getMethod() != HttpMethod.GET) {
      return null;
    }

    Path staticResourceDir = this.staticResourceRequestHandler.staticResourceDirectory;
    String relativePath = request.getPath();
    Path normalizedRelativePath = Paths.get(relativePath).normalize();
    Path absPath = Paths.get(staticResourceDir.toString(), normalizedRelativePath.toString());
    if (absPath.toFile().exists()) {
      return staticResourceRequestHandler;
    }

    return null;
  }

  private Map<String, List<String>> parsePathParams(String pattern, String path) {
    Map<String, List<String>> result = new HashMap<>();
    String[] pathSegments = splitPath(path);
    String[] patternSegments = splitPath(pattern);
    if (pathSegments.length != patternSegments.length) {
      throw new IllegalArgumentException("'" + path + "' should not match '" + pattern + "'.");
    }

    for (int i = 0; i < pathSegments.length; i++) {
      if (patternSegments[i].startsWith("{") && patternSegments[i].endsWith("}")) {
        String key = patternSegments[i].substring(1, patternSegments[i].length() - 1);
        result.putIfAbsent(key, new ArrayList<>());
        result.get(key).add(pathSegments[i]);
      }
    }

    return result;
  }

  TreeNode getNode(TreeNode parent, String segment) {
    return parent.exactChildren.getOrDefault(segment, parent.likeChild);
  }

  private String[] splitPath(String path) {
    Objects.requireNonNull(path, "The path cannot be null.");
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("The path must start with '/'.");
    }

    List<String> segments = new ArrayList<>();
    StringBuilder seg = new StringBuilder();
    for (int i = 1; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        if (!seg.isEmpty()) {
          segments.add(seg.toString());
          seg = new StringBuilder();
        }
      } else {
        seg.append(path.charAt(i));
      }
    }

    if (!seg.isEmpty()) {
      segments.add(seg.toString());
    }

    return segments.toArray(String[]::new);
  }

  /**
   * A dictionary tree node.
   */
  private static class TreeNode {

    private final Map<String, TreeNode> exactChildren = new HashMap<>();

    private TreeNode likeChild;

    private final TreeSet<Route> routes = new TreeSet<>((r1, r2) -> {
      // r1 and r2 has the same method and path
      int score1 = 0, score2 = 0;
      if (Objects.nonNull(r1.getHeaderMatcher())) {
        score1 |= (1 << 1);
      }

      if (Objects.nonNull(r1.getParamMatcher())) {
        score1 |= 1;
      }

      if (Objects.nonNull(r2.getHeaderMatcher())) {
        score2 |= (1 << 1);
      }

      if (Objects.nonNull(r2.getParamMatcher())) {
        score2 |= 1;
      }

      return score2 - score1;
    });
  }

  private static class StaticResourceRequestHandler implements HttpRequestHandler {

    private final Path staticResourceDirectory;

    StaticResourceRequestHandler(Path staticResourcePath) {
      this.staticResourceDirectory = staticResourcePath;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
      String path = request.getPath();
      Path normalizedRelativePath = Paths.get(path).normalize();
      Path absPath = Paths.get(staticResourceDirectory.toString(), normalizedRelativePath.toString());

      try (FileChannel fileChannel = FileChannel.open(absPath, StandardOpenOption.READ)) {
        long contentLength = fileChannel.size();
        MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, contentLength);
        response.status(HttpResponseStatus.OK)
            .putHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), contentLength)
            .putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), Files.probeContentType(absPath))
            .write(Unpooled.wrappedBuffer(byteBuffer));
      }

    }
  }

}
