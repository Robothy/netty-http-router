package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

public class SpringWebRouter extends AbstractRouter {

  private final Map<HttpMethod, TreeSet<ParsedPattern>> parsedPatternMap = new HashMap<>();

  @Override
  public Router route(Route rule) {
    Objects.requireNonNull(rule);
    parsedPatternMap.putIfAbsent(rule.getMethod(), new TreeSet<>(ParsedPattern.COMPARATOR));
    ParsedPattern parsedPattern = new ParsedPattern();
    try {
      parsedPattern.pathPattern = PathPatternParser.defaultInstance.parse(rule.getPath());
    } catch (PatternParseException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    parsedPattern.headersMather = rule.getHeaderMatcher();
    parsedPattern.paramsMather = rule.getParamMatcher();
    parsedPattern.handler = rule.getHandler();
    parsedPatternMap.get(rule.getMethod()).add(parsedPattern);
    return this;
  }

  @Override
  public HttpRequestHandler match(HttpRequest request) {
    return matchAndExtract(request).orElse(notFoundHandler());
  }

  private Optional<HttpRequestHandler> matchAndExtract(HttpRequest request) {
    PathContainer pathContainer = PathContainer.parsePath(request.getPath());
    if (!parsedPatternMap.containsKey(request.getMethod())) {
      return Optional.empty();
    }

    for (ParsedPattern parsedPattern : parsedPatternMap.get(request.getMethod())) {
      PathPattern.PathMatchInfo pathMatchInfo = parsedPattern.pathPattern.matchAndExtract(pathContainer);
      if (Objects.nonNull(pathMatchInfo)) { // path matched
        boolean headerMatched = Optional.ofNullable(parsedPattern.headersMather)
            .map(matcher -> matcher.apply(request.getHeaders())).orElse(true);
        boolean parameterMatched = Optional.ofNullable(parsedPattern.paramsMather)
            .map(matcher -> matcher.apply(request.getParams())).orElse(true);

        if (headerMatched && parameterMatched) {
          Map<CharSequence, List<String>> params = Optional.ofNullable(request.getParams())
              .orElse(new HashMap<>());
          pathMatchInfo.getUriVariables()
              .forEach((key, value) -> {
                if (params.containsKey(key)) {
                  params.get(key).add(value);
                } else {
                  List<String> values = new ArrayList<>(1);
                  values.add(value);
                  params.put(key, values);
                }
              });
          return Optional.of(parsedPattern.handler);
        }

      }
    }
    // Lookup static resources.
    return Optional.ofNullable(staticResourceMatcher().match(request));
  }

  static class ParsedPattern {

    PathPattern pathPattern;

    Function<Map<CharSequence, String>, Boolean> headersMather;

    Function<Map<CharSequence, List<String>>, Boolean> paramsMather;

    private HttpRequestHandler handler;

    Function<Map<CharSequence, String>, Boolean> headersMather() {
      return headersMather;
    }

    Function<Map<CharSequence, List<String>>, Boolean> paramsMather() {
      return paramsMather;
    }

    public PathPattern pathPattern() {
      return pathPattern;
    }

    /**
     * Header matcher first;
     * Then parameter matcher;
     * Then PathPattern.SPECIFICITY_COMPARATOR.
     */
    static Comparator<ParsedPattern> COMPARATOR = Comparator
        .comparing(ParsedPattern::headersMather, Comparator.nullsLast((a, b) -> 0))
        .thenComparing(ParsedPattern::paramsMather, Comparator.nullsLast((a, b) -> 0))
        .thenComparing(ParsedPattern::pathPattern, PathPattern.SPECIFICITY_COMPARATOR)
        .thenComparing(ParsedPattern::hashCode); // Avoid override elements in TreeSet.
  }

}
