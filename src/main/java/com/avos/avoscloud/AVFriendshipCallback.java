package com.avos.avoscloud;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFriendship;

import java.util.List;

/**
 * AVFriendshipQuery的getInBackground中得回调类
 */
abstract class AVFriendshipCallback extends AVCallback<AVFriendship> {

  public abstract void done(AVFriendship friendship, AVException e);

  @Override
  protected final void internalDone0(AVFriendship returnValue, AVException e) {
    done(returnValue, e);
  }
}
