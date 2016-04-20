package com.avos.avoscloud;

import com.avos.avoscloud.internal.InternalConfigurationController;
import com.avos.avoscloud.internal.InternalFileDownloader;

import junit.framework.Assert;
import junit.framework.TestCase;

public class BasicTest extends TestCase {
  @Override
  public void setUp() {
    TestApp.init();
  }

  public void testAppConfiguration() {
    Assert.assertEquals("uu2P5gNTxGhjyaJGAPPnjCtJ-gzGzoHsz", InternalConfigurationController
        .globalInstance().getAppConfiguration().applicationId);
    Assert.assertEquals("j5lErUd6q7LhPD8CXhfmA2Rg", InternalConfigurationController
        .globalInstance().getAppConfiguration().clientKey);
  }

  public void testDownloader() {
    InternalConfigurationController.globalInstance().setDownloaderImplementation(
        TestDownloader.class);
    Assert.assertNull(InternalConfigurationController.globalInstance().getDownloaderInstance(null,
        null));;
  }

  static class TestDownloader implements InternalFileDownloader {

    @Override
    public AVException doWork(String url) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void execute(String url) {
      // TODO Auto-generated method stub

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void setProgressCallback(ProgressCallback callback) {
      // TODO Auto-generated method stub

    }

    @Override
    public void setGetDataCallback(GetDataCallback callback) {
      // TODO Auto-generated method stub

    }

  }
}
