package com.robothy.netty.codec;


import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpRequestHandler;
import com.robothy.netty.http.HttpResponse;
import com.robothy.netty.router.ExceptionHandler;
import com.robothy.netty.router.Router;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpMessageHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private final Router router;

  public HttpMessageHandler(Router router) {
    this.router = router;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
    log.info("{} {}", request.getMethod(), request.getUri());
    HttpRequestHandler handler = router.match(request);
    HttpResponse response = new HttpResponse();
    if (null == handler) {
      log.warn("No handler for {} {}", request.getMethod(), request.getUri());
      response.write("Not found " + request.getPath())
          .status(HttpResponseStatus.NOT_FOUND)
          .putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.TEXT_HTML);
    } else {
      try {
        handler.handle(request, response);
      } catch (Throwable e) {
        log.error("Failed to handle " + request.getMethod() + " " + request.getPath(), e);
        ExceptionHandler<Throwable> exceptionHandler = router.findExceptionHandler(e.getClass());
        response = new HttpResponse();
        // Exceptions from exceptionHandler will be handled by exceptionCaught().
        exceptionHandler.handle(e, request, response);
      }
    }

    if (null == response.getStatus()) {
      response.status(HttpResponseStatus.OK);
    }

    boolean keepAlive = request.getHttpVersion().isKeepAliveDefault() ||
        HttpHeaderValues.KEEP_ALIVE.contentEquals(request.header(HttpHeaderNames.CONNECTION.toString()));
    response.putHeader(HttpHeaderNames.CONNECTION.toString(), keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
    response.putHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), response.getBody().readableBytes());
    ChannelFuture channelFuture = ctx.writeAndFlush(response);
    if (!keepAlive) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Caught exception.", cause);
    HttpResponse response = new HttpResponse();
    response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        .write("<h1>Internal Server Error.</h1>")
        .write(cause.getMessage())
        .putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.TEXT_HTML)
        .putHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.CLOSE)
        .putHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), response.getBody().readableBytes());
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    log.info("Channel " + ctx.channel().id() + " active.");
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel ch = ctx.channel();
    log.info("Channel " + ch.id() + " inactive.");
  }
}
