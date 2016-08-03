package com.avos.avoscloud.internal.impl;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.internal.AppConfiguration;
import com.avos.avoscloud.internal.InternalConfigurationController;
import com.avos.avoscloud.internal.InternalRequestSign;
import com.avos.avoscloud.internal.MasterKeyConfiguration;

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
    return requestSign(AVUtils.getCurrentTimestamp(), this.isUserMasterKey());
  }

  public static String requestSign(long ts, boolean useMasterKey) {
    StringBuilder builder = new StringBuilder();

    StringBuilder result = new StringBuilder();

    AppConfiguration appConfiguration =
        InternalConfigurationController.globalInstance().getAppConfiguration();
    String masterKey = null;
    if (appConfiguration instanceof MasterKeyConfiguration) {
      masterKey = ((MasterKeyConfiguration) appConfiguration).getMasterKey();
    }

    result.append(AVUtils.md5(
        builder.append(ts).append(useMasterKey ? masterKey : appConfiguration.getClientKey())
            .toString()).toLowerCase());
    result.append(',').append(ts);
    if (!useMasterKey) {
      return result.toString();
    } else {
      return result.append(",master").toString();
    }
  }

  public void setUserMasterKey(boolean shouldUseMasterKey) {
    this.useMasterKey = shouldUseMasterKey;
  }

  protected boolean isUserMasterKey() {
    return this.useMasterKey;
  }

}
