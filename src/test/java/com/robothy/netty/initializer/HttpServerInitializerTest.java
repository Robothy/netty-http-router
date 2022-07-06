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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HttpServerInitializerTest {

  @Test
  void test() throws URISyntaxException, IOException, InterruptedException {
    HttpServerInitializer serverInitializer = new HttpServerInitializer(new DefaultEventExecutorGroup(4), Router.router()
        .route(HttpMethod.GET, "/", ((request, response) -> response
            .status(HttpResponseStatus.OK)
            .write("Hello World")))
        .staticResource(Paths.get("src")));


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


    String staticResourcePath = "/test/java/com/robothy/netty/initializer/HttpServerInitializerTest.java";
    HttpResponse<String> staticResourceResp = HttpClient.newHttpClient()
        .send(requestBuilder.GET()
            .uri(new URI("http://localhost:8080" + staticResourcePath))
            .build(), responseInfo -> HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, staticResourceResp.statusCode());
    HttpHeaders headers = staticResourceResp.headers();
    Optional<String> contentLenOptional = headers.firstValue("Content-Length");
    assertTrue(contentLenOptional.isPresent());
    assertEquals(String.valueOf(Files.size(Paths.get("src", staticResourcePath))), contentLenOptional.get());

    serverSocketChannel.close().sync();
  }

}