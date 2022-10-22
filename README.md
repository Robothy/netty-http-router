# netty-http-router

[![Build](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml/badge.svg)](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml)

A library help to build web applications based on Netty.

## 1. Getting Started

### 1.1 Config Maven repository

Before adding the dependency to your project, you should add the Maven repository firstly.

#### Maven

Add your Github token and repository configuration to `~/.m2/settings.xml` file.

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <activeProfiles>
    <activeProfile>github-robothy</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github-robothy</id>
          <url>https://maven.pkg.github.com/robothy/netty-http-router</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <servers>
    <server>
      <id>github-robothy</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
</settings>
```

#### Gradle

Add your Github token and username to `~/.gradle/gradle.properties` to avoid committing your token to VCS. 
Then configure repositories in `${projectDir}/build.gradle`.

- _~/.gradle/gradle.properties_
```properties
GITHUB_USERNAME=You Github username
GITHUB_TOKEN=You Github Token
```
- _~/build.gradle_
```groovy
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url 'https://maven.pkg.github.com/robothy/netty-http-router'
        credentials {
            username = "${GITHUB_USERNAME}"
            password = "${GITHUB_TOKEN}"
        }
    }
}
```

### 1.2 Add dependencies

You can add the dependencies to your project after configure the repository.
```xml
<dependency>
    <groupId>com.robothy</groupId>
    <artifactId>netty-http-router</artifactId>
    <version>1.6</version>
</dependency>
```

### 1.3 Hello World

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

netty-http-router mapping a request to a `HttpMessageHandler` according to the method, path, headers, and query parameters.

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

<style>
  code {
    white-space : pre-wrap !important;
    word-break: break-word;
  }
</style>