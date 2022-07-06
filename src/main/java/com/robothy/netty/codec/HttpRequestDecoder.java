package com.robothy.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.HashMap;
import java.util.List;

public class HttpRequestDecoder extends MessageToMessageDecoder<HttpObject> {

  private com.robothy.netty.http.HttpRequest.HttpRequestBuilder builder;

  private CompositeByteBuf body;

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest httpRequest = (HttpRequest) msg;
      HashMap<CharSequence, String> headers = new HashMap<>();
      httpRequest.headers().forEach(header -> headers.put(header.getKey(), header.getValue()));
      this.body = Unpooled.compositeBuffer();
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
      this.builder = com.robothy.netty.http.HttpRequest.builder()
          .method(httpRequest.method())
          .uri(httpRequest.uri())
          .httpVersion(httpRequest.protocolVersion())
          .headers(headers)
          .body(body)
          .path(queryStringDecoder.path())
          .params(new HashMap<>(queryStringDecoder.parameters()));
    } else if (msg instanceof HttpContent) {
      HttpContent httpContent = (HttpContent) msg;
      ByteBuf content = httpContent.content();
      ReferenceCountUtil.retain(content);
      body.addComponent(true, content);
      if (msg instanceof LastHttpContent) {
        com.robothy.netty.http.HttpRequest request = builder.build();
        out.add(request);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
