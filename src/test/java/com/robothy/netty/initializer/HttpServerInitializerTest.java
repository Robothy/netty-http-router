package com.robothy.netty.initializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.robothy.netty.router.Router;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpServerInitializerTest {

  @Test
  void test() throws URISyntaxException, IOException, InterruptedException {

    Router router = Router.router()
        .route(HttpMethod.GET, "/", ((request, response) -> response
            .status(HttpResponseStatus.OK)
            .write("Hello World")))
        .staticResource(Paths.get("src"));

    DefaultEventExecutorGroup executor = new DefaultEventExecutorGroup(2);
    HttpServerInitializer serverInitializer = new HttpServerInitializer(executor, router);

    EventLoopGroup parentGroup = new NioEventLoopGroup(1);
    EventLoopGroup childGroup = new NioEventLoopGroup(1);
    Channel serverSocketChannel = new ServerBootstrap().group(parentGroup, childGroup)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .channel(NioServerSocketChannel.class)
        .childHandler(serverInitializer)
        .bind(8080)
        .sync()
        .channel();


    HttpRequest.Builder requestBuilder = HttpRequest
        .newBuilder()
        .version(HttpClient.Version.HTTP_1_1);

    HttpResponse<String> response = HttpClient.newHttpClient()
        .send(requestBuilder.GET().uri(new URI("http://localhost:8080"))
            .build(), body -> {
          assertEquals(200, body.statusCode());
          return HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        });
    assertEquals("Hello World", response.body());


    Path staticResourceDirectory = Files.createTempDirectory("static-resource");
    router.staticResource(staticResourceDirectory);

    // Test get small static file.
    Path smallFilePath = Paths.get(staticResourceDirectory.toString(), "/small.txt");
    Files.writeString(smallFilePath, "Hello World");
    HttpResponse<String> smallResourceResp = HttpClient.newHttpClient()
        .send(requestBuilder.GET()
            .uri(new URI("http://localhost:8080/small.txt"))
            .build(), responseInfo -> HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, smallResourceResp.statusCode());
    HttpHeaders headers = smallResourceResp.headers();
    Optional<String> contentLenOptional = headers.firstValue("Content-Length");
    assertTrue(contentLenOptional.isPresent());
    assertEquals(String.valueOf("Hello World".length()), contentLenOptional.get());
    assertEquals("Hello World", smallResourceResp.body());

    // Test get large static file.
    int size = 20 * 1024 * 1024; // 20M
    Random random = new Random(6);

    Path largeFilePath = Paths.get(staticResourceDirectory.toString(), "/large.dat");
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(largeFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      for (int i = 0; i < size; i += 1024) {
        char[] chars = new char[1024];
        Arrays.fill(chars, (char) ('a' + random.nextInt(26)));
        bufferedWriter.write(chars);
      }
      bufferedWriter.flush();
    }

    HttpResponse<InputStream> largeResourceResp = HttpClient.newHttpClient()
        .send(requestBuilder
                .GET()
                .uri(new URI("http://localhost:8080/large.dat"))
                .build(),
            responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
    OptionalLong optionalSize = largeResourceResp.headers().firstValueAsLong("Content-Length");
    assertTrue(optionalSize.isPresent());
    assertEquals(optionalSize.getAsLong(), size);


    // Test HttpRequestHandler throws unhandled exception.
    AtomicReference<RuntimeException> exceptionHolder = new AtomicReference<>();
    router.route(HttpMethod.GET,"/test/exception", (req, resp) -> {
      exceptionHolder.set(new RuntimeException("Unhandled."));
      throw exceptionHolder.get();
    });

    HttpResponse<String> exceptionResponse = HttpClient.newHttpClient()
        .send(requestBuilder.GET().uri(new URI("http://localhost:8080/test/exception")).build(),
            responseInfo -> HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exceptionHolder.get().printStackTrace(new PrintStream(out));
    assertEquals("" + out, exceptionResponse.body());

    serverSocketChannel.close().sync();
    parentGroup.shutdownGracefully();
    childGroup.shutdownGracefully();
    executor.shutdownGracefully();
  }

}