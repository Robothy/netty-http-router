# netty-http-router

[![Build](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml/badge.svg)](https://github.com/Robothy/netty-http-router/actions/workflows/push.yml)

A library help to build web applications based on Netty.

## Getting started

### Config Maven repository

Before adding the dependency to your project, you should add the Maven repository firstly.

+ Maven

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

+ Gradle

Add your Github token and username to `~/.gradle/gradle.properties` to avoid committing your token to VCS. 
Then configure repositories in `${projectDir}/build.gradle`.

- `~/.gradle/gradle.properties`
```properties
GITHUB_USERNAME=You Github username
GITHUB_TOKEN=You Github Token
```
- `~/build.gradle`
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

### Add dependencies

You can add the dependencies to your project after configure the repository.
```xml
<dependency>
    <groupId>com.robothy</groupId>
    <artifactId>netty-http-router</artifactId>
    <version>1.6</version>
</dependency>
```

### Hello World

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

## Features

netty-http-router finds a `HttpMessageHandler` for an HTTP request according to the method, path, headers, and query parameters.
Generally, only use the method and path matching. For example,
+ `"/hello"` matches `"/hello"`
+ `"/user/{id}"` matches `'/user/123'` and `/user/666`
+ `/user/{id}/profile` matches `/user/123/profile` and `/user/12/profie`

To match requests via headers and query parameters. You need to define routes with header and parameter matcher.
The below example matches a request with an X-Authorization header.
```java
Route.builder()
    .method(HttpMethod.HEAD)
    .path("/a/content")
    .headerMatcher(headers -> headers.containsKey("X-Authorization"))
    .handler(paramRequestHandler)
    .build()
```


<style>
  code {
    white-space : pre-wrap !important;
    word-break: break-word;
  }
</style>