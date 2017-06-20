package com.avos.avoscloud.internal.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.internal.InternalPersistence;

public class SimplePersistence implements InternalPersistence {

  public static SimplePersistence instance() {
    synchronized (SimplePersistence.class) {
      if (instance == null) {
        instance = new SimplePersistence();
      }
    }
    return instance;
  }

  protected SimplePersistence() {}

  private static SimplePersistence instance;

  @Override
  public File getPaasDocumentDir() {

    return null;
  }

  @Override
  public File getCacheDir() {
    return null;
  }

  @Override
  public File getCommandCacheDir() {
    return null;
  }

  @Override
  public boolean saveContentToFile(String content, File fileForSave) {
    return saveContentToFile(content.getBytes(), fileForSave);
  }

  @Override
  public boolean saveContentToFile(byte[] content, File fileForSave) {
    FileOutputStream fos;
    try {
      fos = new FileOutputStream(fileForSave);
      fos.write(content);

      return true;
    } catch (IOException e) {
      throw new RuntimeException(e);
      return false;
    } finally {
      AVUtils.closeQuietly(fos);
    }
  }

  @Override
  public void saveToDocumentDir(String content, String folderName, String fileName) {
    saveContentToFile(content.getBytes(), new File(folderName, fileName));
  }

  @Override
  public String getFromDocumentDir(String folderName, String fileName) {
    return readContentFromFile(new File(folderName, fileName));
  }

  @Override
  public String readContentFromFile(File fileForRead) {
    return new String(readContentBytesFromFile(fileForRead));
  }

  @Override
  public byte[] readContentBytesFromFile(File fileForRead) {
    byte[] buffer = null;
    FileInputStream fis;
    ByteArrayOutputStream bos;
    try {
      fis = new FileInputStream(fileForRead);
      bos = new ByteArrayOutputStream();
      byte[] b = new byte[1024 * 5];
      int n;
      while ((n = fis.read(b)) != -1) {
        bos.write(b, 0, n);
      }
      buffer = bos.toByteArray();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      AVUtils.closeQuietly(fis);
      AVUtils.closeQuietly(bos);
    }
    return buffer;
  }

  @Override
  public void deleteFile(File file) {
    try {
      file.deleteOnExit();
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void savePersistentSettingBoolean(String keyzone, String key, Boolean value) {

  }

  @Override
  public boolean getPersistentSettingBoolean(String keyzone, String key) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean getPersistentSettingBoolean(String keyzone, String key, Boolean defaultValue) {
    // TODO Auto-generated method stub
    return defaultValue;
  }

  @Override
  public void savePersistentSettingInteger(String keyzone, String key, Integer value) {
    // TODO Auto-generated method stub

  }

  @Override
  public Integer getPersistentSettingInteger(String keyzone, String key, Integer defaultValue) {
    // TODO Auto-generated method stub
    return defaultValue;
  }

  @Override
  public Long getPersistentSettingLong(String keyzone, String key, Long defaultValue) {
    // TODO Auto-generated method stub
    return defaultValue;
  }

  @Override
  public void savePersistentSettingLong(String keyzone, String key, Long value) {
    // TODO Auto-generated method stub

  }

  @Override
  public void savePersistentSettingString(String keyzone, String key, String value) {

  }

  @Override
  public String getPersistentSettingString(String keyzone, String key, String defaultValue) {
    return defaultValue;
  }

  @Override
  public void removePersistentSettingString(String keyzone, String key) {

  }

  @Override
  public String removePersistentSettingString(String keyzone, String key, String defaultValue) {
    return null;
  }

  @Override
  public void removeKeyZonePersistentSettings(String keyzone) {

  }

  @Override
  public String getAVFileCachePath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public File getAVFileCacheFile(String url) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void cleanAVFileCache(int days) {
    // TODO Auto-generated method stub

  }

  private AVUser currentUser;

  @Override
  public void setCurrentUser(AVUser user, boolean clean) {
    this.currentUser = user;
  }

  @Override
  public <T extends AVUser> T getCurrentUser(Class<T> userClass) {
    if (currentUser != null) {
      return (T) AVUser.cast(currentUser, userClass);
    } else {
      return null;
    }

  }

}
