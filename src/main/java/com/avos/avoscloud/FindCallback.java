package com.avos.avoscloud;

import java.util.List;

/**
 * <p>
 * A FindCallback is used to run code after a AVQuery is used to fetch a list of AVObjects in a
 * background thread.
 * </p>
 * 
 * <p>
 * The easiest way to use a FindCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the fetch is complete. The done function
 * will be run in the UI thread, while the fetch happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * <p>
 * For example, this sample code fetches all objects of class "MyClass". It calls a different
 * function depending on whether the fetch succeeded or not.
 * </p>
 */

abstract class FindCallback<T extends AVObject> extends AVCallback<java.util.List<T>> {
  /**
   * Override this function with the code you want to run after the fetch is complete.
   * 
   * @param parseObjects The objects matching the query, or null if it failed.
   * @param parseException The exception raised by the find, or null if it succeeded.
   */
  public abstract void done(List<T> parseObjects, AVException parseException);

  protected final void internalDone0(List<T> returnValue, AVException e) {
    done(returnValue, e);
  }
}
