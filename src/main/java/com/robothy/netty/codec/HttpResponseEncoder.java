package com.robothy.netty.codec;

import com.robothy.netty.http.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import java.util.List;

public class HttpResponseEncoder extends MessageToMessageEncoder<HttpResponse> {
  @Override
  protected void encode(ChannelHandlerContext ctx, HttpResponse msg, List<Object> out) throws Exception {
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, msg.getStatus(), msg.getBody());
    HttpHeaders headers = response.headers();
    msg.getHeaders().forEach(headers::add);
    out.add(response);
  }
}
