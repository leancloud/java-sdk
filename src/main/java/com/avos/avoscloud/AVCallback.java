package com.avos.avoscloud;

/**
 * User: summer Date: 13-4-11 Time: PM3:46
 */
abstract class AVCallback<T> {
  public void internalDone(final T t, final AVException parseException) {
    internalDone0(t, parseException);
  }

  public void internalDone(final AVException parseException) {
    this.internalDone(null, parseException);
  }

  protected abstract void internalDone0(T t, AVException parseException);
}
