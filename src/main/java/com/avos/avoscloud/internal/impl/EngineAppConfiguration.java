package com.avos.avoscloud.internal.impl;

import java.util.concurrent.ThreadPoolExecutor;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.internal.AppConfiguration;

public class EngineAppConfiguration extends AppConfiguration {

  public String masterKey;

  public static EngineAppConfiguration instance() {
    synchronized (EngineAppConfiguration.class) {
      if (instance == null) {
        instance = new EngineAppConfiguration();
      }
    }
    return instance;
  }

  private EngineAppConfiguration() {}

  private static EngineAppConfiguration instance;


  @Override
  public boolean isConfigured() {
    return !(AVUtils.isBlankString(this.applicationId) || AVUtils.isBlankString(this.clientKey) || AVUtils
        .isBlankString(masterKey));
  }

  @Override
  public void setupThreadPoolExecutor(ThreadPoolExecutor excutor) {
    // do nothing
  }

  @Override
  public boolean isConnected() {
    return true;
  }

}
