package com.avos.avoscloud;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 12/27/13 Time: 9:46 AM To change this template
 * use File | Settings | File Templates.
 */

abstract class FollowersAndFolloweesCallback<T extends AVObject>
    extends AVCallback<java.util.Map<String, T>> {
  /**
   * Override this function with the code you want to run after the fetch is complete.
   * 
   * @param parseObjects The objects matching the query, or null if it failed.
   * @param parseException The exception raised by the find, or null if it succeeded.
   */
  public abstract void done(Map<String, T> parseObjects, AVException parseException);

  protected final void internalDone0(Map<String, T> returnValue, AVException e) {
    done(returnValue, e);
  }
}
