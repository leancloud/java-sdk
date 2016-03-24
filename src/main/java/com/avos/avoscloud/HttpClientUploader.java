package com.avos.avoscloud;

import com.avos.avoscloud.okhttp.OkHttpClient;

/**
 * Created with IntelliJ IDEA. User: dennis (xzhuang@avos.com) Date: 13-7-26 Time: 下午3:37
 */
public abstract class HttpClientUploader extends AVUploader {

  protected HttpClientUploader(AVFile parseFile, SaveCallback saveCallback,
      ProgressCallback progressCallback) {
    super(parseFile, saveCallback, progressCallback); // To change body of overridden methods use
                                                      // File | Settings | File Templates.
  }

  static OkHttpClient client = new OkHttpClient();;

  protected static synchronized OkHttpClient getOKHttpClient() {
    return client;
  }
}
