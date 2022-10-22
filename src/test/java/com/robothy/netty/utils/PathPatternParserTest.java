package com.robothy.netty.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

class PathPatternParserTest {

  PathPatternParser parser = new PathPatternParser();

  @MethodSource("extractUriTemplateVariablesCases")
  @ParameterizedTest
  void extractUriTemplateVariables(String pattern, String path, Map<String, String> variables) {
    PathPattern pathPattern = parser.parse(pattern);
    PathContainer pathContainer = PathContainer.parsePath(path);
    assertEquals(variables, pathPattern.matchAndExtract(pathContainer).getUriVariables());
  }

  static Stream<Arguments> extractUriTemplateVariablesCases() {
    return Stream.of(
        arguments("/{user}", "/Bob", Map.of("user", "Bob")),
        arguments("/{group}/{user}", "/A/Bob", Map.of("group", "A", "user", "Bob")),
        arguments("/{*key}", "/a/b/c", Map.of("key", "/a/b/c")),
        arguments("/b/{*key}", "/b/a/c", Map.of("key", "/a/c"))
    );
  }

}