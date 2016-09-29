package com.avos.avoscloud.internal.impl;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.internal.InternalLogger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Log4j2Implementation extends InternalLogger {

  private static final Logger logger = LogManager.getLogger(AVOSCloud.class);


  public static Log4j2Implementation instance() {
    synchronized (Log4j2Implementation.class) {
      if (instance == null) {
        instance = new Log4j2Implementation();
      }
    }
    return instance;
  }

  private Log4j2Implementation() {}

  private static Log4j2Implementation instance;

  @Override
  public int v(String tag, String msg) {
    logger.info(msg);
    return 0;
  }

  @Override
  public int v(String tag, String msg, Throwable tr) {
    logger.info(msg, tr);
    return 0;
  }

  @Override
  public int d(String tag, String msg) {
    logger.debug(msg);
    return 0;
  }

  @Override
  public int d(String tag, String msg, Throwable tr) {
    logger.debug(msg, tr);
    return 0;
  }

  @Override
  public int i(String tag, String msg) {
    logger.info(msg);
    return 0;
  }

  @Override
  public int i(String tag, String msg, Throwable tr) {
    logger.info(msg, tr);
    return 0;
  }

  @Override
  public int w(String tag, String msg) {
    logger.warn(msg);
    return 0;
  }

  @Override
  public int w(String tag, String msg, Throwable tr) {
    logger.warn(msg, tr);
    return 0;
  }

  @Override
  public int w(String tag, Throwable tr) {
    logger.warn(tr);
    return 0;
  }

  @Override
  public int e(String tag, String msg) {
    logger.error(msg);
    return 0;
  }

  @Override
  public int e(String tag, String msg, Throwable tr) {
    logger.error(msg, tr);
    return 0;
  }

}
