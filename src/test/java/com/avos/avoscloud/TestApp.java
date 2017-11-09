package com.avos.avoscloud;

import com.avos.avoscloud.internal.InternalConfigurationController;
import com.avos.avoscloud.internal.InternalLogger;
import com.avos.avoscloud.internal.impl.DefaultAppRouter;
import com.avos.avoscloud.internal.impl.JavaAppConfiguration;
import com.avos.avoscloud.internal.impl.Log4j2Implementation;
import com.avos.avoscloud.internal.impl.SimplePersistence;

public class TestApp {

  public static void init() {
    TestApp.init("uu2P5gNTxGhjyaJGAPPnjCtJ-gzGzoHsz", "j5lErUd6q7LhPD8CXhfmA2Rg",
        "atXAmIVlQoBDBLqumMgzXhcY", "rTXH9hotZfAJjtFOQqwfEHpC", true);
  }

  public static void init(String applicationId, String clientKey, String masterKey, String hookKey,
      boolean isCN) {
    System.setProperty("LEANCLOUD_APP_HOOK_KEY", hookKey);
    JavaAppConfiguration configuration = JavaAppConfiguration.instance();
    configuration.setIsCN(isCN);
    configuration.setApplicationId(applicationId);
    configuration.setClientKey(clientKey);
    configuration.setMasterKey(masterKey);

    InternalLogger logger = Log4j2Implementation.instance();
    logger.setDebugEnabled(true);

    new InternalConfigurationController.Builder().setAppConfiguration(configuration)
        .setAppRouter(DefaultAppRouter.instance()).setInternalLogger(logger)
        .setInternalPersistence(SimplePersistence.instance())
        .build();
    InternalConfigurationController.globalInstance().getAppRouter().updateServerHosts();
  }

}
