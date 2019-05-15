## SDK 维护期说明

我们于 2018 年 9 月推出了新的 Java Unified SDK，兼容纯 Java、云引擎和 Android 等多个平台，此 java-sdk 目前进入维护状态，直到 2019 年 9 月底停止维护。 欢迎大家切换到新的 Unified SDK，具体使用方法详见 [Unified SDK Wiki](https://github.com/leancloud/java-unified-sdk/wiki)。

## LeanCloud Java SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Platform](https://img.shields.io/badge/platform-java-3cbe00.svg)

java-sdk is the souce code for Java SDK

leancloud-utils is the mock interface for android platform features which doesn't exist on Java platform

## Features
  * [x] Data Storage
  * [x] Object Query
  * [x] Cloud Engine
  * [x] File Storage
  * [x] Short Message Service

## Communication
  * If you **have some advice**, open an issue.
  * If you **found a bug**, open an issue, or open a ticket in [LeanTicket][LeanTicket].
  * If you **want to contribute**, submit a pull request.


## Installation

### maven

add dependency:

``` xml
    <dependency>
      <groupId>cn.leancloud</groupId>
      <artifactId>java-sdk</artifactId>
      <version>0.1.6</version>
    </dependency>
```

### gradle

add dependency:

``` gradle
compile 'cn.leancloud:java-sdk:0.1.+'

```
