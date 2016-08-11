package com.avos.avoscloud;

import com.avos.avoscloud.BasicTest.TestDownloader;
import com.avos.avoscloud.data.Armor;
import com.avos.avoscloud.data.Player;
import com.avos.avoscloud.internal.InternalConfigurationController;

public class TestApp {

  public static void init() {
    InternalConfigurationController.Builder builder = new InternalConfigurationController.Builder();
    builder.setDownloaderImplementation(TestDownloader.class).build();;
    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(Armor.class);
    AVObject.registerSubclass(Player.class);
    AVOSCloud.initialize("uu2P5gNTxGhjyaJGAPPnjCtJ-gzGzoHsz", "j5lErUd6q7LhPD8CXhfmA2Rg",
        "atXAmIVlQoBDBLqumMgzXhcY");
  }

  public static void initForQCloud() {
    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(Armor.class);
    AVOSCloud.initialize("DV8dqMSRqujdz6hI7NFtCfEq-9Nh9j0Va", "IPvWcVpYBvkHuk6QYc9Jvg3F",
        "atXAmIVlQoBDBLqumMgzXhcY");
  }
}
