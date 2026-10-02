package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

final class DefaultRouter extends AbstractRouter {

  private final Set<Route> ruleSet = new HashSet<>();

  private final TreeNode root = new TreeNode();

  @Override
  public Router route(Route route) {
    if (ruleSet.contains(route)) {
      throw new IllegalArgumentException("The router already has a handler for route " + route);
    }
    ruleSet.add(route);

    TreeNode node = addNode(root, route.getMethod().name());
    String[] segments = splitPath(route.getPath());
    for (int i = 0; i < segments.length; i++) {
      String segment = segments[i];
      node = addNode(node, segment);
      if (isGreedyVariable(segment) && i != segments.length - 1) {
        throw new IllegalArgumentException(
            "The greedy path variable '" + segment + "' must be the last segment in '" + route.getPath() + "'.");
      }
    }
    node.routes.add(route);
    return this;
  }

  private TreeNode addNode(TreeNode parent, String path) {
    TreeNode child = new TreeNode();
    if (isVariable(path)) {
      if (path.length() == 2) {
        throw new IllegalArgumentException("The path variable name cannot be empty.");
      }
      if (isGreedyVariable(path)) {
        if (parent.greedyChild == null) {
          parent.greedyChild = child;
        }
        return parent.greedyChild;
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
    if (null != (handler = matchHandler(request)) || null != (handler = super.staticResourceMatcher().match(request))) {
      return handler;
    }
    return super.notFoundHandler();
  }

  private HttpRequestHandler matchHandler(HttpRequest request) {
    String[] segments = splitPath(request.getPath());
    TreeNode node = root.exactChildren.get(request.getMethod().name());
    if (node == null) {
      return null;
    }

    int idx = 0;
    TreeNode greedyNode = null;
    int greedyIdx = -1;
    for (; idx < segments.length; idx++) {
      if (node.greedyChild != null) {
        // A greedy variable consumes the remaining segments; remember the deepest
        // position where it can apply and try exact/variable children first, so
        // more specific routes win over the greedy one.
        greedyNode = node.greedyChild;
        greedyIdx = idx;
      }
      TreeNode tmp = getNode(node, segments[idx]);
      if (tmp == null) {
        break;
      }
      node = tmp;
    }

    Route result = null;
    if (idx == segments.length && !node.routes.isEmpty()) {
      result = pickRoute(node, request);
    }

    if (result == null && greedyNode != null && greedyNode.routes.isEmpty() == false) {
      result = pickRoute(greedyNode, request);
      if (result != null) {
        idx = greedyIdx;
        node = greedyNode;
      }
    }

    if (result == null) {
      return null;
    }

    request.getParams().putAll(parsePathParams(result.getPath(), request, idx));
    return result.getHandler();
  }

  private Route pickRoute(TreeNode node, HttpRequest request) {
    for (Route route : node.routes) {
      boolean headerMatched = (route.getHeaderMatcher() == null || route.getHeaderMatcher().apply(request.getHeaders()));
      boolean paramMatched = (route.getParamMatcher() == null || route.getParamMatcher().apply(request.getParams()));
      if (headerMatched && paramMatched) {
        return route;
      }
    }
    return null;
  }

  private Map<String, List<String>> parsePathParams(String pattern, HttpRequest request, int consumedSegments) {
    Map<String, List<String>> result = new HashMap<>();
    String[] patternSegments = splitPath(pattern);
    String[] pathSegments = splitPath(request.getPath());

    // Simple (non-greedy) variables consume one decoded segment each; a trailing
    // greedy variable consumes the whole remaining (raw) part of the URI.
    int nonGreedyCount = patternSegments.length;
    for (String patternSegment : patternSegments) {
      if (isGreedyVariable(patternSegment)) {
        nonGreedyCount--;
      }
    }
    if (pathSegments.length < nonGreedyCount) {
      throw new IllegalArgumentException("'" + request.getPath() + "' should not match '" + pattern + "'.");
    }

    String remainder = rawRemainder(request, patternSegments, consumedSegments);
    int pathIdx = 0;
    for (String patternSegment : patternSegments) {
      if (!isVariable(patternSegment)) {
        pathIdx++;
        continue;
      }
      String key = variableName(patternSegment);
      result.putIfAbsent(key, new ArrayList<>());
      if (isGreedyVariable(patternSegment)) {
        result.get(key).add(remainder);
        break;
      }
      if (pathIdx >= pathSegments.length) {
        break;
      }
      result.get(key).add(pathSegments[pathIdx]);
      pathIdx++;
    }

    return result;
  }

  /**
   * Returns the remaining raw (still URL-encoded) part of the request URI after
   * the fixed prefix of the pattern, without the query string. Encoded slashes
   * ("%2F") therefore survive inside a greedy variable value.
   */
  private String rawRemainder(HttpRequest request, String[] patternSegments, int consumedSegments) {
    String uri = request.getUri();
    int queryIdx = uri.indexOf('?');
    String rawPath = queryIdx >= 0 ? uri.substring(0, queryIdx) : uri;
    String[] rawSegments = splitPath(rawPath);
    StringBuilder sb = new StringBuilder();
    for (int i = consumedSegments; i < rawSegments.length; i++) {
      sb.append('/').append(rawSegments[i]);
    }
    String joined = sb.toString();
    return joined.startsWith("/") ? joined.substring(1) : joined;
  }

  TreeNode getNode(TreeNode parent, String segment) {
    return parent.exactChildren.getOrDefault(segment, parent.likeChild);
  }

  private boolean isVariable(String segment) {
    return segment.startsWith("{") && segment.endsWith("}");
  }

  private boolean isGreedyVariable(String segment) {
    return isVariable(segment) && segment.endsWith("+}")
        && segment.length() > 3 && segment.charAt(segment.length() - 2) == '+';
  }

  private String variableName(String segment) {
    String inner = segment.substring(1, segment.length() - 1);
    return inner.endsWith("+") ? inner.substring(0, inner.length() - 1) : inner;
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
        if (!(seg.length() == 0)) {
          segments.add(seg.toString());
          seg = new StringBuilder();
        }
      } else {
        seg.append(path.charAt(i));
      }
    }

    if (!(seg.length() == 0)) {
      segments.add(seg.toString());
    }

    return segments.toArray(new String[0]);
  }

  /**
   * A dictionary tree node.
   */
  private static class TreeNode {

    private final Map<String, TreeNode> exactChildren = new HashMap<>();

    private TreeNode likeChild;

    private TreeNode greedyChild;

    private final TreeSet<Route> routes = new TreeSet<>((r1, r2) -> {
      // r1 and r2 has the same method and path
      int score1 = 0, score2 = 0;
      // Header matcher has higher priority.
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

}
