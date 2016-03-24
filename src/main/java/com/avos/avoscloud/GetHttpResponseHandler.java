package com.avos.avoscloud;

import com.avos.avoscloud.okhttp.internal.framed.Header;

public class GetHttpResponseHandler extends AsyncHttpResponseHandler {


  private String absoluteURLString;

  public GetHttpResponseHandler(GenericObjectCallback cb) {
    super(cb);
  }

  public GetHttpResponseHandler(GenericObjectCallback cb, String absoluteURLString) {
    this(cb);
    this.absoluteURLString = absoluteURLString;
  }

  private boolean isUnAuthorize(int code) {
    return (code == 401);
  }


  @Override
  public void onSuccess(int statusCode, Header[] headers, byte[] body) {

    String content = AVUtils.stringFromBytes(body);
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d(content);
    }

    String contentType = PaasClient.extractContentType(headers);
    if (AVUtils.checkResponseType(statusCode, content, contentType, getCallback()))
      return;

    int code = AVErrorUtils.errorCode(content);
    if (code > 0) {
      if (getCallback() != null) {
        getCallback().onFailure(AVErrorUtils.createException(code, content), content);
      }
      return;
    }

    if (getCallback() != null) {
      getCallback().onSuccess(content, null);
    }
  }

  @Override
  public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {


    String content = AVUtils.stringFromBytes(responseBody);

    if (isUnAuthorize(statusCode)) {
      LogUtil.avlog.e(content + "\nerror:" + error + " for request:" + absoluteURLString);
    }

    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.e(content + "\nerror:" + error);
    }

    String contentType = PaasClient.extractContentType(headers);
    if (AVUtils.checkResponseType(statusCode, content, contentType, getCallback()))
      return;

    if (getCallback() != null) {
      getCallback().onFailure(statusCode, error, content);
    }
  }
}
