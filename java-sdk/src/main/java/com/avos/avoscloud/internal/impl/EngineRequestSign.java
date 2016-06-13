package com.avos.avoscloud.internal.impl;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.internal.InternalRequestSign;

public class EngineRequestSign implements InternalRequestSign {

  boolean useMasterKey = false;

  public static EngineRequestSign instance() {
    synchronized (EngineRequestSign.class) {
      if (instance == null) {
        instance = new EngineRequestSign();
      }
    }
    return instance;
  }

  private EngineRequestSign() {}

  private static EngineRequestSign instance;

  @Override
  public String requestSign() {
    StringBuilder builder = new StringBuilder();
    long ts = AVUtils.getCurrentTimestamp();
    StringBuilder result = new StringBuilder();
    result.append(AVUtils.md5(
        builder
            .append(ts)
            .append(
                useMasterKey ? EngineAppConfiguration.instance().masterKey : EngineAppConfiguration
                    .instance().clientKey).toString()).toLowerCase());
    result.append(',').append(ts);
    if (useMasterKey) {
      return result.toString();
    } else {
      return result.append(",master").toString();
    }
  }

  public void setUserMasterKey(boolean shouldUseMasterKey) {
    this.useMasterKey = shouldUseMasterKey;
  }

}
