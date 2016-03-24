package com.avos.avoscloud;

/**
 * Created by lbt05 on 1/15/16.
 */
public class AVSaveOption {

  AVQuery matchQuery;
  boolean fetchWhenSave;

  public AVSaveOption setFetchWhenSave(boolean fetchWhenSave) {
    this.fetchWhenSave = fetchWhenSave;
    return this;
  }

  public AVSaveOption query(AVQuery query) {
    this.matchQuery = query;
    return this;
  }
}
