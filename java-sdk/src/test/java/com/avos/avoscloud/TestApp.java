package com.avos.avoscloud;

import com.avos.avoscloud.data.Armor;
import com.avos.avoscloud.data.SubUser;

public class TestApp {

  public static void init() {
    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(Armor.class);
    AVOSCloud.initialize("uu2P5gNTxGhjyaJGAPPnjCtJ-gzGzoHsz", "j5lErUd6q7LhPD8CXhfmA2Rg");
  }
}
