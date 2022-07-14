package com.robothy.netty.router;

import com.robothy.netty.http.HttpRequest;
import com.robothy.netty.http.HttpResponse;

public interface ExceptionHandler<T extends Throwable> {

  void handle(T e, HttpRequest request, HttpResponse response);

}
