package com.avos.avoscloud;

public enum AVOSServices {
  STORAGE_SERVICE("avoscloud-storage"), STATISTICS_SERVICE("avoscloud-statistics"),
  FUNCTION_SERVICE(
      "avoscloud-function");
  private String service;

  private AVOSServices(String service) {
    this.service = service;
  }

  public String toString() {
    return this.service;
  }
}
