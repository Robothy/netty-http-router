package com.robothy.netty.http;

public interface HttpRequestHandler {

  void handle(HttpRequest request, HttpResponse response) throws Exception;

}
