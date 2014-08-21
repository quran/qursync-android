package com.quran.profiles.library.util;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;

import android.net.http.AndroidHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.client.ApacheClient;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;

public class ProxyHttpClient extends ApacheClient {
  private static HttpClient createDefaultClient() {
    final HttpClient client = AndroidHttpClient.newInstance("retrofit");
    final HttpHost proxy = new HttpHost("10.0.3.2", 8080, "http");
    client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    return client;
  }

  public ProxyHttpClient() {
    super(createDefaultClient());
  }

  @Override
  public Response execute(Request request) throws IOException {
    List<Header> headers = request.getHeaders();
    List<retrofit.client.Header> modified = new ArrayList<>();
    for (Header header : headers) {
      if (!header.getName().equals("Content-Length")) {
        modified.add(header);
      }
    }
    return super.execute(new Request(request.getMethod(),
        request.getUrl(), modified, request.getBody()));
  }
}