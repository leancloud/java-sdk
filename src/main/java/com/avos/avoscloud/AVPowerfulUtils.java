package com.avos.avoscloud;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: tangxiaomin Date: 4/18/13 Time: 3:19 PM
 */
public class AVPowerfulUtils {
  private static Map<String/* javaClassName */, Map<String, String>> powerfulTable =
      new HashMap<String, Map<String, String>>();
  private static final String ENDPOINT = "endpoint";
  private static final String PARSE_CLASSNAME = "dbClassName";
  static {
    createTable();
  }

  public static void createSettings(String javaClassName, String endpoint, String parseClassName) {
    Map<String, String> settings = new HashMap<String, String>();
    settings.put(ENDPOINT, endpoint);
    settings.put(PARSE_CLASSNAME, parseClassName);
    powerfulTable.put(javaClassName, settings);
  }

  private static void createTable() {
    // Add both java class name and parse class name.
    // init AVUser
    createSettings(AVUser.class.getSimpleName(), "users", "_User");
    createSettings("_User", "users", "_User");

    // init AVRole
    createSettings(AVRole.class.getSimpleName(), "roles", "_Role");
    createSettings("_Role", "roles", "_Role");

    // init AVFile
    createSettings(AVFile.class.getSimpleName(), "files", "_File");
    createSettings("_File", "files", "_File");

  }

  private static String get(String javaClassName, String key) {
    String res = "";
    if (powerfulTable.containsKey(javaClassName)) {
      res = powerfulTable.get(javaClassName).get(key);
      if (res == null) {
        res = "";
      }
    }
    return res;
  }

  private static String getAVClassEndpoint(String javaClassName, String parseClassName,
      String objectId) {
    String endpoint = get(javaClassName, ENDPOINT);
    if (AVUtils.isBlankString(endpoint)) {
      if (AVUtils.isBlankString(objectId)) {
        endpoint = String.format("classes/%s", parseClassName);
      } else {
        endpoint = String.format("classes/%s/%s", parseClassName, objectId);
      }
    }
    return endpoint;
  }

  private static String getAVUserEndpoint(AVUser object) {
    String endpoint = get(AVUser.class.getSimpleName(), ENDPOINT);
    if (!AVUtils.isBlankString(object.getObjectId())) {
      endpoint = String.format("%s/%s", endpoint, object.getObjectId());
    }
    return endpoint;
  }

  private static String getAVRoleEndpoint(AVRole object) {
    String endpoint = get(AVRole.class.getSimpleName(), ENDPOINT);
    if (!AVUtils.isBlankString(object.getObjectId())) {
      endpoint = String.format("%s/%s", endpoint, object.getObjectId());
    }
    return endpoint;
  }

  // Endpoint handler. Try java classname at first, fallback to parse class name if not found.
  public static String getEndpoint(String className) {
    String endpoint = get(className, ENDPOINT);
    if (AVUtils.isBlankString(endpoint)) {
      if (!AVUtils.isBlankString(className)) {
        endpoint = String.format("classes/%s", className);
      } else {
        throw new AVRuntimeException("Blank class name");
      }
    }
    return endpoint;
  }

  public static String getEndpoint(Object object) {
    return getEndpoint(object, false);
  }

  public static String getEndpoint(Object object, boolean post) {
    if (object instanceof AVUser) {
      AVUser parseUser = (AVUser) object;
      return getAVUserEndpoint(parseUser);
    } else if (object instanceof AVRole) {
      AVRole role = (AVRole) object;
      return getAVRoleEndpoint(role);
    } else if (object instanceof AVObject) {
      AVObject parseObject = (AVObject) object;
      Class<? extends AVObject> clazz = parseObject.getClass();
      String javaClassName = clazz.getSimpleName();
      String subClassName = AVObject.getSubClassName(clazz);
      if (subClassName != null) {
        return getAVClassEndpoint(javaClassName, subClassName, parseObject.getObjectId());
      }
      return getAVClassEndpoint(javaClassName, parseObject.getClassName(),
          parseObject.getObjectId());
    } else {
      return getEndpoint(object.getClass().getSimpleName());
    }
  }

  public static String getBatchEndpoint(String version, AVObject object) {
    return getBatchEndpoint(version, object, false);
  }

  public static String getBatchEndpoint(String version, AVObject object, boolean post) {
    return String.format("/%s/%s", version, getEndpoint(object, post));
  }

  public static String getEndpointByAVClassName(String className, String objectId) {
    String rootUrl = getEndpoint(className);
    if (AVUtils.isBlankString(rootUrl)) {
      return rootUrl;
    }
    return String.format("%s/%s", rootUrl, objectId);
  }

  public static String getAVClassName(String className) {
    return get(className, PARSE_CLASSNAME);
  }

  // followee follows follower.
  public static String getFollowEndPoint(String followee, String follower) {
    return String.format("users/%s/friendship/%s", followee, follower);
  }

  public static String getFollowersEndPoint(String userId) {
    return String.format("users/%s/followers", userId);
  }

  public static String getFolloweesEndPoint(String userId) {
    return String.format("users/%s/followees", userId);
  }

  public static String getFollowersAndFollowees(String userId) {
    return String.format("users/%s/followersAndFollowees", userId);
  }

  public static String getInternalIdFromRequestBody(Map request) {
    if (request.get("body") != null) {
      Map body = (Map) request.get("body");
      return (String) body.get("__internalId");
    }
    return null;
  }

}
