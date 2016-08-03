package com.avos.avoscloud.internal.impl;

import java.util.concurrent.ThreadPoolExecutor;

import com.avos.avoscloud.AVOSServices;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.AppRouterManager;
import com.avos.avoscloud.internal.AppConfiguration;
import com.avos.avoscloud.internal.MasterKeyConfiguration;

public class JavaAppConfiguration extends AppConfiguration implements MasterKeyConfiguration {

  String masterKey;

  public static JavaAppConfiguration instance() {
    synchronized (JavaAppConfiguration.class) {
      if (instance == null) {
        instance = new JavaAppConfiguration();
      }
    }
    return instance;
  }

  protected JavaAppConfiguration() {}

  private static JavaAppConfiguration instance;


  @Override
  public boolean isConfigured() {
    return !(AVUtils.isBlankString(this.getApplicationId())
        || AVUtils.isBlankString(this.getClientKey()) || (EngineRequestSign.instance()
        .isUserMasterKey() && AVUtils.isBlankString(this.getMasterKey())));
  }

  @Override
  public void setupThreadPoolExecutor(ThreadPoolExecutor excutor) {
    // do nothing
  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public String getMasterKey() {
    return this.masterKey;
  }

  @Override
  public void setMasterKey(String masterKey) {
    this.masterKey = masterKey;
    this.setEnv();
  }

  protected void setEnv() {
    serviceHostMap.put(AVOSServices.STORAGE_SERVICE.toString(), AppRouterManager.getInstance()
        .getAPIServer());
  }
}
