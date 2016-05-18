package com.avos.avoscloud;

import junit.framework.TestCase;

public class QCloudTest extends TestCase {

  public void testQCloudEndPoint() throws AVException {
    TestApp.initForQCloud();
    AVQuery query = new AVQuery("Validation");
    AVObject object = query.getFirst();
    assertTrue(object.getBoolean("isQCloud"));
  }

}
