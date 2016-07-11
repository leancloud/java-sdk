package com.avos.avoscloud.internal.impl;

import java.io.File;

import com.avos.avoscloud.AVUser;
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
    return false;
  }

  @Override
  public boolean saveContentToFile(byte[] content, File fileForSave) {
    return false;
  }

  @Override
  public void saveToDocumentDir(String content, String folderName, String fileName) {}

  @Override
  public String getFromDocumentDir(String folderName, String fileName) {
    return null;
  }

  @Override
  public String readContentFromFile(File fileForRead) {
    return null;
  }

  @Override
  public byte[] readContentBytesFromFile(File fileForRead) {
    return null;
  }

  @Override
  public void deleteFile(File file) {

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
    return (T) AVUser.cast(currentUser, userClass);
  }

}
