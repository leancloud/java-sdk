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



### gradle

Add LeanCloud repository

``` gradle
repositories {
        maven {
            url "http://mvn.leancloud.cn/nexus/content/repositories/releases"
        }
    }
```


add dependency

``` gradle
compile 'cn.leancloud.java:java-sdk:0.1.+'

```
