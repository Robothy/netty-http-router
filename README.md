# netty-http-router

[![Build](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml/badge.svg)](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.robothy/netty-http-router?color=blueviolet&logo=apachemaven)](https://central.sonatype.dev/artifact/io.github.robothy/netty-http-router/1.13/versions)

A library help to build web applications based on Netty.

## 1. Getting Started

### Dependency

This library is published to the Maven Central, you can add this dependency without extra configurations.

```xml
<dependency>
    <groupId>io.github.robothy</groupId>
    <artifactId>netty-http-router</artifactId>
    <version>1.13</version>
</dependency>
```

### 1.3 Hello World

The HelloWord example follows the standard steps of startup a Netty application. You only
need to define a `Router` and an executor group that executes HTTP message handlers.

```java
class HelloWorld {
  public static void main(String[] args) throws InterruptedException {
    Router router = Router.router()
        .route(HttpMethod.GET, "/hello", ((request, response) -> response
            .status(HttpResponseStatus.OK)
            .write("Hello World")));

    DefaultEventExecutorGroup executor = new DefaultEventExecutorGroup(2);
    HttpServerInitializer serverInitializer = new HttpServerInitializer(executor, router);

    EventLoopGroup parentGroup = new NioEventLoopGroup(1);
    EventLoopGroup childGroup = new NioEventLoopGroup(1);
    Channel serverSocketChannel = new ServerBootstrap().group(parentGroup, childGroup)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .channel(NioServerSocketChannel.class)
        .childHandler(serverInitializer)
        .bind(8686)
        .sync()
        .channel();
    serverSocketChannel.close().sync();
  }
}
```

## 2. Usages

### 2.1 Request mapping

netty-http-router map an HTTP request to a `HttpMessageHandler` according to the method, path, headers, and query parameters.

#### Request path pattern

This library uses SpringMVC path pattern to match request paths.

+ `"/hello"` matches `"/hello"`
+ `"/user/{id}"` matches `'/user/123'`, `/user/666`, etc.
+ `/user/{id}/profile` matches `/user/123/profile`, `/user/bob/profie`, etc.
+ `/user/{id:[0-9]{1,}}/profile` matches `/user/123/profile` and not match `/user/bob/profile`.

You can find more details about the path pattern in the [PathPattern](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/pattern/PathPattern.html) javadoc.

#### Headers and parameters matcher

netty-http-router allows setting headers and parameters match rules via 
`headerMatcher(header rule)` and `paramMatcher(parameter rule)`. The matching
priority from high to low is HTTP method, path, headers, and parameters.

```java
Route.builder()
    .method(HttpMethod.HEAD)
    .path("/a/content")
    .headerMatcher(headers -> headers.containsKey("X-Authorization"))
    .paramsMatcher(params -> params.containsKey("user"))
    .handler(paramRequestHandler)
    .build()
```

### 2.2 Serving static resources

netty-http-router scans static resources in the `/static` directory of the classpath by default.
You can customize the path via `Router#staticResources()`.

```java
Router router=Router.router()
    .route(HttpMethod.GET,"/", handler)
    .staticResource("my-static-resources");
```

### 2.3 Not found handler and exception handlers

You can set a not found handler and exception handlers for a Router.

```java
Router router=Router.router()
    .route(HttpMethod.GET,"/", handler)
    .notFound((request, response) -> response.status(HttpResponseStatus.NOT_FOUND))
    .exceptionHandler(IllegalArgumentException.class, (e, request, response) -> response
      .status(HttpResponseStatus.BAD_REQUEST)
      .write(e.getMessage()))
    .exceptionHandler(IllegalStateException.class, (e, request, response) -> response
      .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      .write(e.getMessage()))
    ;
```