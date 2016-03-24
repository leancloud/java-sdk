package com.avos.avoscloud;

import com.avos.avoscloud.okhttp.Callback;
import com.avos.avoscloud.okhttp.Headers;
import com.avos.avoscloud.okhttp.Request;
import com.avos.avoscloud.okhttp.Response;
import com.avos.avoscloud.okhttp.internal.framed.Header;

import java.io.IOException;

/**
 * Created by lbt05 on 9/17/15.
 */
public abstract class AsyncHttpResponseHandler implements Callback {
  protected GenericObjectCallback callback;

  public AsyncHttpResponseHandler(GenericObjectCallback callback) {
    this.callback = callback;
  }

  public AsyncHttpResponseHandler() {

  }

  protected GenericObjectCallback getCallback() {
    return callback;
  }

  protected void setCallback(GenericObjectCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onFailure(Request request, IOException e) {
    onFailure(0, getHeaders(request.headers()), null, e);
  }

  @Override
  public void onResponse(Response response) throws IOException {
    this.onSuccess(response.code(), getHeaders(response.headers()), response.body().bytes());
  }

  public abstract void onSuccess(int statusCode, Header[] headers, byte[] body);

  public abstract void onFailure(int statusCode, Header[] headers, byte[] responseBody,
      Throwable error);

  static Header[] getHeaders(Headers headers) {
    if (headers != null && headers.size() > 0) {
      Header[] httpHeaders = new Header[headers.size()];
      for (int index = 0; index < headers.size(); index++) {
        final String key = headers.name(index);
        final String value = headers.get(key);
        httpHeaders[index] = new Header(key, value);
      }
      return httpHeaders;
    }
    return null;
  }
}
