package com.avos.avoscloud;

import com.avos.avoscloud.BasicTest.TestDownloader;
import com.avos.avoscloud.data.Armor;
import com.avos.avoscloud.data.Player;
import com.avos.avoscloud.internal.InternalConfigurationController;
import com.avos.avoscloud.internal.impl.JavaAppConfiguration;
import com.avos.avoscloud.internal.impl.JavaRequestSignImplementation;
import com.avos.avoscloud.internal.impl.Log4j2Implementation;
import com.avos.avoscloud.internal.impl.SimplePersistence;

public class TestApp {

  public static void init() {
    System.setProperty("LEANCLOUD_APP_HOOK_KEY", "rTXH9hotZfAJjtFOQqwfEHpC");
    InternalConfigurationController.Builder builder = new InternalConfigurationController.Builder();
    builder.setDownloaderImplementation(TestDownloader.class);
    builder.setAppConfiguration(JavaAppConfiguration.instance())
        .setInternalRequestSign(JavaRequestSignImplementation.instance())
        .setInternalPersistence(SimplePersistence.instance())
        .setInternalLogger(Log4j2Implementation.instance());
    builder.build();

    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(Armor.class);
    AVObject.registerSubclass(Player.class);
    AVOSCloud.initialize("uu2P5gNTxGhjyaJGAPPnjCtJ-gzGzoHsz", "j5lErUd6q7LhPD8CXhfmA2Rg",
        "atXAmIVlQoBDBLqumMgzXhcY");
  }

  public static void initForQCloud() {
    System.setProperty("LEANCLOUD_APP_HOOK_KEY", "rTXH9hotZfAJjtFOQqwfEHpC");
    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(Armor.class);
    AVOSCloud.initialize("DV8dqMSRqujdz6hI7NFtCfEq-9Nh9j0Va", "IPvWcVpYBvkHuk6QYc9Jvg3F",
        "atXAmIVlQoBDBLqumMgzXhcY");
  }

  public static void initForUS() {
    System.setProperty("LEANCLOUD_APP_HOOK_KEY", "dCMnBqEygcursl7dtC2WmseV");
    AVOSCloud.setDebugLogEnabled(true);
    AVOSCloud.useAVCloudUS();
    AVOSCloud.initialize("cswk4i7a7fgprutnxr9cldg6f7d9yr4jpsak2dxlm94vgaoy",
        "7u1kavw2y2805kue7pxyxxszxyj46cbf3zxmde9n6exfpfpo",
        "dtq0s5sjdf4op3nfnqtu2rqbk4yef6h0f1pysagtsjod8g2s");
  }
}
