package com.robothy.netty.initializer;

import com.robothy.netty.codec.HttpMessageHandler;
import com.robothy.netty.router.Router;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.EventExecutorGroup;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

  private final EventExecutorGroup executorGroup;

  private final Router router;

  public HttpServerInitializer(EventExecutorGroup executorGroup, Router router) {
    this.executorGroup = executorGroup;
    this.router = router;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    ch.config().setConnectTimeoutMillis(10000);
    ch.config().setAutoClose(true);
    pipeline.addLast("http-request-decoder", new HttpRequestDecoder());
    pipeline.addLast("http-response-encoder", new HttpResponseEncoder());
    pipeline.addLast(this.executorGroup, "router-http-request-decoder", new com.robothy.netty.codec.HttpRequestDecoder());
    pipeline.addLast(this.executorGroup, "router-http-response-encoder", new com.robothy.netty.codec.HttpResponseEncoder());
    pipeline.addLast(this.executorGroup, "router-http-message-handler", new HttpMessageHandler(router));
  }

}
