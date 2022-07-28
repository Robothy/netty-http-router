package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

abstract class StaticResourceMatcher {

  abstract HttpRequestHandler match(HttpRequest request);

  static StaticResourceMatcher create(String path) {
    Objects.requireNonNull(path, "The static resource path shouldn't be null.");
    if (path.startsWith("classpath:")) {
      return new ClasspathResourceMatcher(path);
    } else {
      return new DirectoryResourceMatcher(path);
    }
  }

  /**
   * Match resource in classpath.
   */
  private static class ClasspathResourceMatcher extends StaticResourceMatcher {

    private final String resourceRoot;

    private final HttpRequestHandler handler;

    ClasspathResourceMatcher(String path) {
      this.resourceRoot = path.substring("classpath:".length());
      this.handler = (request, response) -> {
        String resourceName = resourceName(request);
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        Objects.requireNonNull(url, request.getPath() + " not exist.");
        String urlStr = url.toString();
        String contentType = Files.probeContentType(Paths.get(urlStr.substring(urlStr.lastIndexOf("/") + 1)));
        response.putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
        try (InputStream in = url.openStream()) {
          response.write(in.readAllBytes());
        }
      };
    }

    @Override
    public HttpRequestHandler match(HttpRequest request) {
      String resourceName = resourceName(request);
      URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
      if (null == resource) {
        return null;
      }
      return handler;
    }

    String resourceName(HttpRequest request) {
      if ("/".equals(request.getPath()) || "".equals(request.getPath())) {
        return resourceRoot + "/index.html";
      }
      return resourceRoot + request.getPath();
    }

  }

  /**
   * Match resources in a directory.
   */
  private static class DirectoryResourceMatcher extends StaticResourceMatcher {

    private static final int MAP_THRESHOLD = 10 * 1024 * 1024; // 10MB

    private final HttpRequestHandler handler;

    private final String rootDirectory;

    DirectoryResourceMatcher(String directory) {
      this.rootDirectory = directory;
      this.handler = (request, response) -> {
        String filename = filename(request);
        Path absPath = Paths.get(directory, filename);

        try (FileChannel fileChannel = FileChannel.open(absPath, StandardOpenOption.READ)) {
          long contentLength = fileChannel.size();
          response.status(HttpResponseStatus.OK)
              .putHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), contentLength)
              .putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), Files.probeContentType(absPath));

          if (contentLength > MAP_THRESHOLD) {
            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, contentLength);
            response.write(Unpooled.wrappedBuffer(byteBuffer));
          } else {
            int size = (int) contentLength;
            ByteBuffer buf = ByteBuffer.allocate(size);
            int readLen = 0;
            while( size != (readLen += fileChannel.read(buf, readLen)) ) {
              buf.compact();
            }
            buf.flip();
            response.write(Unpooled.wrappedBuffer(buf));
          }

        }
      };
    }

    @Override
    public HttpRequestHandler match(HttpRequest request) {
      if (request.getMethod() != HttpMethod.GET) {
        return null;
      }

      String filename = filename(request);
      Path absPath = Paths.get(rootDirectory, filename);
      if (!absPath.toFile().exists()) {
        return null;
      }

      return handler;
    }

    private String filename(HttpRequest request) {
      if ("".equals(request.getPath()) || "/".equals(request.getPath())) {
        return "index.html";
      }

      return Paths.get(request.getPath()).normalize().toString();
    }

  }

}
