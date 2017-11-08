package com.avos.avoscloud.internal.impl;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.internal.AppConfiguration;
import com.avos.avoscloud.internal.MasterKeyConfiguration;

public class JavaAppConfiguration extends AppConfiguration implements MasterKeyConfiguration {

  String masterKey;

  private final String apiHookKeyField = "X-LC-Hook-Key";

  private final String hookKey;

  public static JavaAppConfiguration instance() {
    synchronized (JavaAppConfiguration.class) {
      if (instance == null) {
        instance = new JavaAppConfiguration();
      }
    }
    return instance;
  }

  protected JavaAppConfiguration() {
    hookKey = getEnvOrProperty("LEANCLOUD_APP_HOOK_KEY");
  }

  private static JavaAppConfiguration instance;


  @Override
  public boolean isConfigured() {
    return !(AVUtils.isBlankString(this.getApplicationId())
        || AVUtils.isBlankString(this.getClientKey()) || (JavaRequestSignImplementation.instance()
        .isUseMasterKey() && AVUtils.isBlankString(this.getMasterKey())));
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
  }

  protected String getEnvOrProperty(String key) {
    String value = System.getenv(key);
    if (value == null) {
      value = System.getProperty(key);
    }
    return value;
  }

  @Override
  public Map<String, String> getRequestHeaders() {
    Map<String, String> result = super.getRequestHeaders();
    if (hookKey != null) {
      result.put(apiHookKeyField, hookKey);
    }
    return result;
  }

  @Override
  public String dumpRequestHeaders() {
    if (hookKey != null) {
      return String.format("%s -H \"%s: %s\"", super.dumpRequestHeaders(), apiHookKeyField,
          dumpKey(hookKey, "YourHookKey"));
    }
    return super.dumpRequestHeaders();
  }

}
