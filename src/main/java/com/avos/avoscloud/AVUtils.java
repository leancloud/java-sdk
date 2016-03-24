package com.avos.avoscloud;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.utils.Base64;

public class AVUtils {
  private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String classNameTag = "className";
  public static final String typeTag = "__type";
  public static final String objectIdTag = "objectId";

  public static Map<String, Object> createArrayOpMap(String key, String op, Collection<?> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Object> array = new ArrayList<Object>();
    for (Object obj : objects) {
      array.add(getParsedObject(obj));
    }
    map.put("objects", array);
    Map<String, Object> ops = new HashMap<String, Object>();
    ops.put(key, map);
    return ops;
  }

  private static Map<Class<?>, Field[]> fieldsMap = Collections
      .synchronizedMap(new WeakHashMap<Class<?>, Field[]>());

  public static Field[] getAllFiels(Class<?> clazz) {
    if (clazz == null || clazz == Object.class) {
      return new Field[0];
    }
    Field[] theResult = fieldsMap.get(clazz);
    if (theResult != null) {
      return theResult;
    }
    List<Field[]> fields = new ArrayList<Field[]>();
    int length = 0;
    while (clazz != null && clazz != Object.class) {
      Field[] declaredFields = clazz.getDeclaredFields();
      length += declaredFields != null ? declaredFields.length : 0;
      fields.add(declaredFields);
      clazz = clazz.getSuperclass();
    }
    theResult = new Field[length];
    int i = 0;
    for (Field[] someFields : fields) {
      if (someFields != null) {
        for (Field field : someFields) {
          field.setAccessible(true);
        }
        System.arraycopy(someFields, 0, theResult, i, someFields.length);
        i += someFields.length;
      }
    }
    fieldsMap.put(clazz, theResult);
    return theResult;
  }

  static Pattern pattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  static Pattern emailPattern = Pattern.compile("^\\w+?@\\w+?[.]\\w+");
  static Pattern phoneNumPattern = Pattern.compile("1\\d{10}");
  static Pattern verifyCodePattern = Pattern.compile("\\d{6}");

  public static boolean checkEmailAddress(String email) {
    return emailPattern.matcher(email).find();
  }

  public static boolean checkMobilePhoneNumber(String phoneNumber) {
    return phoneNumPattern.matcher(phoneNumber).find();
  }

  public static boolean checkMobileVerifyCode(String verifyCode) {
    return verifyCodePattern.matcher(verifyCode).find();
  }

  public static void checkClassName(String className) {
    if (isBlankString(className))
      throw new IllegalArgumentException("Blank class name");
    if (!pattern.matcher(className).matches())
      throw new IllegalArgumentException("Invalid class name");
  }

  public static boolean isBlankString(String str) {
    return str == null || str.trim().equals("");
  }

  public static boolean isBlankContent(String content) {
    return isBlankString(content) || content.trim().equals("{}");
  }

  public static boolean contains(Map<String, Object> map, String key) {
    return map.containsKey(key);
  }

  public static Map<String, Object> createDeleteOpMap(String key) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", "Delete");
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(key, map);
    return result;
  }

  public static Map<String, Object> createPointerArrayOpMap(String key, String op,
      Collection<AVObject> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Map<String, ?>> list = new ArrayList<Map<String, ?>>();
    for (AVObject obj : objects) {
      list.add(AVUtils.mapFromPointerObject(obj));
    }
    map.put("objects", list);
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(key, map);
    return result;
  }

  public static Map<String, Object> createStringObjectMap(String key, Object value) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(key, value);
    return map;
  }

  public static Map<String, Object> mapFromPointerObject(AVObject object) {
    return mapFromAVObject(object, false);
  }

  public static Map<String, Object> mapFromUserObjectId(final String userObjectId) {
    if (isBlankString(userObjectId)) {
      return null;
    }
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "Pointer");
    result.put("className", "_User");
    result.put("objectId", userObjectId);
    return result;
  }

  public static Map<String, String> mapFromChildObject(AVObject object, String key) {
    String cid = object.internalId();
    Map<String, String> child = new HashMap(3);
    child.put("cid", cid);
    child.put("className", object.getClassName());
    child.put("key", key);
    return child;
  }

  private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_DATE_FORMAT =
      new ThreadLocal<SimpleDateFormat>();

  public static boolean isDigitString(String s) {
    if (s == null)
      return false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  public static Date dateFromString(String content) {
    if (isBlankString(content))
      return null;
    if (isDigitString(content)) {
      return new Date(Long.parseLong(content));
    }
    Date date = null;
    SimpleDateFormat format = THREAD_LOCAL_DATE_FORMAT.get();
    // reuse date format.
    if (format == null) {
      format = new SimpleDateFormat(dateFormat);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      THREAD_LOCAL_DATE_FORMAT.set(format);
    }
    try {
      date = format.parse(content);
    } catch (Exception exception) {
      LogUtil.log.e(exception.toString());
    }
    return date;
  }

  public static String stringFromDate(Date date) {
    SimpleDateFormat df = new SimpleDateFormat(dateFormat);
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    String isoDate = df.format(date);
    return isoDate;
  }

  public static Map<String, Object> mapFromDate(Date date) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Date");
    result.put("iso", stringFromDate(date));
    return result;
  }

  public static Date dateFromMap(Map<String, Object> map) {
    String value = (String) map.get("iso");
    return dateFromString(value);
  }

  public static Map<String, Object> mapFromGeoPoint(AVGeoPoint point) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "GeoPoint");
    result.put("latitude", point.getLatitude());
    result.put("longitude", point.getLongitude());
    return result;
  }

  public static AVGeoPoint geoPointFromMap(Map<String, Object> map) {
    double la = ((Number) map.get("latitude")).doubleValue();
    double lo = ((Number) map.get("longitude")).doubleValue();
    AVGeoPoint point = new AVGeoPoint(la, lo);
    return point;
  }

  // create cooresponding object from class name.
  public static AVObject objectFromRelationMap(Map<String, Object> map) {
    String className = (String) map.get(classNameTag);
    AVObject object = objectFromClassName(className);
    return object;
  }

  public static Map<String, Object> mapFromByteArray(byte[] data) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Bytes");
    result.put("base64", Base64.encodeToString(data, Base64.NO_WRAP));
    return result;
  }

  public static byte[] dataFromMap(Map<String, Object> map) {
    String value = (String) map.get("base64");
    return Base64.decode(value, Base64.NO_WRAP);
  }

  public static String jsonStringFromMapWithNull(Object map) {

    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero,
          SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero);
    }
  }

  public static String jsonStringFromObjectWithNull(Object map) {

    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero,
          SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero);
    }
  }

  /*
   * // from parse { "__type": "File", "url":
   * "http://files.parse.com/bc9f32df-2957-4bb1-93c9-ec47d9870a05/db295fb2-8a8b-49f3-aad3-dd911142f64f-hello.txt"
   * , "name": "db295fb2-8a8b-49f3-aad3-dd911142f64f-hello.txt" } // from urulu(qiniu) { "__type":
   * "File", "bucket": "x5ocz6du3qyn5jiay7xw", "createdAt": "2013-05-23T07:38:18.000Z", "key":
   * "8dyu9yShs6hi47co", "mime_type": "application/octet-stream", "name": "sample.apk", "objectId":
   * "519dc76ae4b034b9cc5170a8", "updatedAt": "2013-05-23T07:38:18.000Z" } // from urulu(s3) {
   * "__type": "File", "createdAt": "2013-05-27T07:10:52.000Z", "objectId":
   * "51a306fce4b06e53feb1d95f", "updatedAt": "2013-05-27T07:10:52.000Z", "url":
   * "https://s3-ap-northeast-1.amazonaws.com/avos-cloud/b60b1e29-5314-4538-9759-2cb6d6c74185" }
   */
  public static Map<String, Object> mapFromFile(AVFile file) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", AVFile.className());
    result.put("metaData", file.getMetaData());

    switch (AVOSCloud.getStorageType()) {
      case StorageTypeAV:
        result.put("name", file.getName());
        break;
      case StorageTypeQiniu:
      case StorageTypeS3:
        // we store the objectId in file.name
        result.put("id", file.getName());
        break;
      default:
        break;
    }

    return result;
  }

  public static AVFile fileFromMap(Map<String, Object> map) {
    AVFile file = new AVFile("", "");
    AVUtils.copyPropertiesFromMapToObject(map, file);
    Object metadata = map.get("metaData");
    if (metadata != null && metadata instanceof Map)
      file.getMetaData().putAll((Map) metadata);
    if (AVUtils.isBlankString((String) file.getMetaData(AVFile.FILE_NAME_KEY))) {
      file.getMetaData().put(AVFile.FILE_NAME_KEY, file.getName());
    }

    // maybe there isnt url in dict, so we need do some trick
    switch (AVOSCloud.getStorageType()) {
      case StorageTypeAV:
        break;
      case StorageTypeQiniu:
        // file.setUrl(QiniuUploader.getFileLink((String) map.get("bucket"),
        // (String) map.get("key")));
      case StorageTypeS3:
        file.setName((String) map.get("objectId"));
        break;
      default:
        break;
    }

    return file;
  }

  public static AVObject parseObjectFromMap(Map<String, Object> map) {
    AVObject object = newAVObjectByClassName((String) map.get(classNameTag));
    object.setObjectId((String) map.get("objectId"));
    AVUtils.copyPropertiesFromMapToAVObject(map, object);
    return object;
  }

  public static String restfulServerData(Map<String, ?> data) {
    if (data == null)
      return "{}";

    Map<String, Object> map = getParsedMap((Map<String, Object>) data);
    return jsonStringFromMapWithNull(map);
  }

  public static String restfulCloudData(Object object) {
    if (object == null)
      return "{}";
    if (object instanceof Map) {
      return jsonStringFromMapWithNull(getParsedMap((Map<String, Object>) object, true));
    } else if (object instanceof Collection) {
      return jsonStringFromObjectWithNull(getParsedList((Collection) object, true));
    } else if (object instanceof AVObject) {
      return jsonStringFromMapWithNull(mapFromAVObject((AVObject) object, true));
    } else if (object instanceof AVGeoPoint) {
      return jsonStringFromMapWithNull(mapFromGeoPoint((AVGeoPoint) object));
    } else if (object instanceof Date) {
      return jsonStringFromObjectWithNull(mapFromDate((Date) object));
    } else if (object instanceof byte[]) {
      return jsonStringFromMapWithNull(mapFromByteArray((byte[]) object));
    } else if (object instanceof AVFile) {
      return jsonStringFromMapWithNull(mapFromFile((AVFile) object));
    } else if (object instanceof org.json.JSONObject) {
      return jsonStringFromObjectWithNull(JSON.parse(object.toString()));
    } else if (object instanceof org.json.JSONArray) {
      return jsonStringFromObjectWithNull(JSON.parse(object.toString()));
    } else {
      return jsonStringFromObjectWithNull(object);
    }
  }

  private static Map<String, Object> mapFromAVObject(AVObject object, boolean topObject) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("className", object.internalClassName());

    if (!isBlankString(object.getObjectId())) {
      result.put("objectId", object.getObjectId());
    }
    if (!topObject) {
      result.put("__type", "Pointer");
    } else {
      result.put("__type", "Object");

      Map<String, Object> serverData = getParsedMap(object.serverData, false);
      if (serverData != null && !serverData.isEmpty()) {
        result.putAll(serverData);
      }
    }
    return result;
  }

  private static List getParsedList(Collection object, boolean topObject) {
    if (!topObject) {
      return getParsedList(object);
    } else {
      List newList = new ArrayList(object.size());

      for (Object o : object) {
        newList.add(getParsedObject(o, true));
      }

      return newList;
    }
  }

  private static Map<String, Object> getParsedMap(Map<String, Object> object, boolean topObject) {
    Map newMap = new HashMap<String, Object>(object.size());

    for (Map.Entry<String, Object> entry : object.entrySet()) {
      final String key = entry.getKey();
      Object o = entry.getValue();
      newMap.put(key, getParsedObject(o, topObject));
    }

    return newMap;
  }

  public static boolean hasProperty(Class<?> clazz, String property) {
    Field fields[] = getAllFiels(clazz);
    for (Field f : fields) {
      if (f.getName().equals(property)) {
        return true;
      }
    }
    return false;
  }

  public static boolean checkAndSetValue(Class<?> clazz, Object parent, String property,
      Object value) {
    if (clazz == null) {
      return false;
    }
    try {
      Field fields[] = getAllFiels(clazz);
      for (Field f : fields) {
        if (f.getName().equals(property) && (f.getType().isInstance(value) || value == null)) {
          f.set(parent, value);
          return true;
        }
      }
      return false;
    } catch (Exception exception) {
      // TODO throw exception?
      // exception.printStackTrace();
    }
    return false;
  }

  public static void updatePropertyFromMap(AVObject parent, String key, Map<String, Object> map) {
    String objectId = (String) map.get(objectIdTag);
    String type = (String) map.get(typeTag);
    if (type == null && objectId == null) {
      parent.put(key, map, false);
      return;
    }

    if (isGeoPoint(type)) {
      AVGeoPoint point = geoPointFromMap(map);
      parent.put(key, point, false);
    } else if (isDate(type)) {
      Date date = dateFromMap(map);
      parent.put(key, date, false);
    } else if (isData(type)) {
      byte[] data = dataFromMap(map);
      parent.put(key, data, false);
    } else if (isFile(type)) {
      AVFile file = AVUtils.fileFromMap(map);
      parent.put(key, file, false);
    } else if (isFileFromUrulu(map)) {
      AVFile file = AVUtils.fileFromMap(map);
      parent.put(key, file, false);
    } else if (isRelation(type)) {
      parent.addRelationFromServer(key, (String) map.get(classNameTag), false);
    } else if (isPointer(type) || (!isBlankString(objectId) && type != null)) {
      AVObject object = AVUtils.parseObjectFromMap(map);
      parent.put(key, object, false);
    } else {
      parent.put(key, map, false);
    }
  }

  public static void updatePropertyFromList(AVObject object, String key, Collection<Object> list) {
    List data = getObjectFrom(list);
    object.put(key, data, false);
  }

  public static void copyPropertiesFromJsonStringToAVObject(String content, AVObject object) {
    if (isBlankString(content))
      return;
    try {
      Map<String, Object> map = JSONHelper.mapFromString(content);
      copyPropertiesFromMapToAVObject(map, object);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  public static void copyPropertiesFromMapToAVObject(Map<String, Object> map, AVObject object) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      if (key != null && key.startsWith("_")) {
        continue;
      }
      Object valueObject = entry.getValue();
      if (checkAndSetValue(object.getClass(), object, key, valueObject)) {
        // also put it into keyValues map.
        if (!key.startsWith("_") && !AVObject.INVALID_KEYS.contains(key)) {
          object.put(key, valueObject, false);
        }
        continue;
      } else if (valueObject instanceof Collection) {
        updatePropertyFromList(object, key, (Collection) valueObject);
      } else if (valueObject instanceof Map) {
        updatePropertyFromMap(object, key, (Map<String, Object>) valueObject);
      } else {
        if (!key.startsWith("_")) {
          object.put(key, valueObject, false);
        }
      }
    }
  }

  public static void copyPropertiesFromMapToObject(Map<String, Object> map, Object object) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      Object valueObject = entry.getValue();
      if (checkAndSetValue(object.getClass(), object, key, valueObject)) {
        continue;
      }
    }
  }

  public static Class getClass(Map<String, ?> map) {
    Object type = map.get("__type");

    if (type == null || !(type instanceof String)) {
      return Map.class;
    } else if (type.equals("Pointer")) {
      return AVObject.class;
    } else if (type.equals("GeoPoint")) {
      return AVGeoPoint.class;
    } else if (type.equals("Bytes")) {
      return byte[].class;
    } else if (type.equals("Date")) {
      return Date.class;
    }

    return Map.class;
  }

  public static boolean isRelation(String type) {
    return (type != null && type.equals("Relation"));
  }

  public static boolean isPointer(String type) {
    return (type != null && type.equals("Pointer"));
  }

  public static boolean isGeoPoint(String type) {
    return (type != null && type.equals("GeoPoint"));
  }

  public static boolean isACL(String type) {
    return (type != null && type.equals("ACL"));
  }

  public static boolean isDate(String type) {
    return (type != null && type.equals("Date"));
  }

  public static boolean isData(String type) {
    return (type != null && type.equals("Bytes"));
  }

  public static boolean isFile(String type) {
    return (type != null && type.equals("File"));
  }

  public static boolean isFileFromUrulu(Map<String, Object> map) {
    // ugly way to check dict whether is avfile
    boolean result = true;
    result &= map.get("mime_type") != null;
    return result;
  }

  public static AVObject objectFromClassName(String className) {
    if (className.equals(AVPowerfulUtils.getAVClassName(AVUser.class.getSimpleName()))) {
      return AVUser.newAVUser();
    }
    AVObject object = newAVObjectByClassName(className);
    return object;
  }

  public static AVObject newAVObjectByClassName(String name) {
    if (name.equals(AVRole.className)) {
      return new AVRole();
    } else if (name.equals(AVUser.userClassName())) {
      return AVUser.newAVUser();
    } else {
      // maybe it's AVObject's subclass
      Class<? extends AVObject> subClazz = AVObject.getSubClass(name);
      if (subClazz != null) {
        try {
          return subClazz.newInstance();
        } catch (Exception e) {
          throw new AVRuntimeException("New subclass instance failed.", e);
        }
      } else {
        // just new a AVObject
        return new AVObject(name);
      }
    }
  }

  public static Class<? extends AVObject> getAVObjectClassByClassName(String name) {
    if (name.equals(AVRole.className)) {
      return AVRole.class;
    } else if (name.equals(AVUser.userClassName())) {
      return AVUser.class;
    } else {
      // maybe it's AVObject's subclass
      Class<? extends AVObject> subClazz = AVObject.getSubClass(name);
      return subClazz;
    }
  }

  public static AVObject newAVObjectByClassName(String className, String defaultClassName) {
    String objectClassName = AVUtils.isBlankString(className) ? defaultClassName : className;
    return newAVObjectByClassName(objectClassName);
  }

  // ================================================================================
  // Handle JSON and Object
  // ================================================================================

  public static final <T> T getFromJSON(String json, Class<T> clazz) {
    return JSON.parseObject(json, clazz);
  }

  public static final <T> String toJSON(T clazz) {
    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(clazz, SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(clazz);
    }
  }

  // ================================================================================
  // Data for server
  // ================================================================================

  static Map<String, Object> getParsedMap(Map<String, Object> map) {
    return getParsedMap(map, false);
  }

  static List getParsedList(Collection list) {
    List newList = new ArrayList(list.size());

    for (Object o : list) {
      newList.add(getParsedObject(o));
    }

    return newList;
  }

  public static Object getParsedObject(Object object) {
    return getParsedObject(object, false);
  }

  public static Object getParsedObject(Object object, boolean topObject) {
    if (object == null) {
      return null;
    } else if (object instanceof Map) {
      return getParsedMap((Map<String, Object>) object, topObject);
    } else if (object instanceof Collection) {
      return getParsedList((Collection) object, topObject);
    } else if (object instanceof AVObject) {
      if (!topObject) {
        return mapFromPointerObject((AVObject) object);
      } else {
        return mapFromAVObject((AVObject) object, true);
      }
    } else if (object instanceof AVGeoPoint) {
      return mapFromGeoPoint((AVGeoPoint) object);
    } else if (object instanceof Date) {
      return mapFromDate((Date) object);
    } else if (object instanceof byte[]) {
      return mapFromByteArray((byte[]) object);
    } else if (object instanceof AVFile) {
      return mapFromFile((AVFile) object);
    } else if (object instanceof org.json.JSONObject) {
      return JSON.parse(object.toString());
    } else if (object instanceof org.json.JSONArray) {
      return JSON.parse(object.toString());
    } else {
      return object;
    }
  }

  // ================================================================================
  // Data from server
  // ================================================================================
  /*
   * response like this: {"result":"Hello world!"} { "result": { "__type": "Object", "className":
   * "Armor", "createdAt": "2013-04-02T06:15:27.211Z", "displayName": "Wooden Shield", "fireproof":
   * false, "objectId": "2iGGg18C7H", "rupees": 50, "updatedAt": "2013-04-02T06:15:27.211Z" } } {
   * "result": [ { "__type": "Object", "cheatMode": false, "className": "Armor", "createdAt":
   * "2013-04-20T07:45:54.962Z", "objectId": "8o2ncpWitt", "otherArmor": { "__type": "Pointer",
   * "className": "Armor", "objectId": "dEvrhyRGcr" }, "playerName": "Sean Plott", "score": 1337,
   * "testBytes": { "__type": "Bytes", "base64": "VGhpcyBpcyBhbiBlbmNvZGVkIHN0cmluZw==" },
   * "testDate": { "__type": "Date", "iso": "2011-08-21T18:02:52.249Z" }, "testGeoPoint": {
   * "__type": "GeoPoint", "latitude": 40, "longitude": -30 }, "testRelation": { "__type":
   * "Relation", "className": "GameScore" }, "updatedAt": "2013-04-20T07:45:54.962Z" } ] }
   */
  static List getObjectFrom(Collection list) {
    List newList = new ArrayList();

    for (Object obj : list) {
      newList.add(getObjectFrom(obj));
    }

    return newList;
  }

  static Object getObjectFrom(Map<String, Object> map) {
    Object type = map.get("__type");
    if (type == null || !(type instanceof String)) {
      Map<String, Object> newMap = new HashMap<String, Object>(map.size());

      for (Map.Entry<String, Object> entry : map.entrySet()) {
        final String key = entry.getKey();
        Object o = entry.getValue();
        newMap.put(key, getObjectFrom(o));
      }

      return newMap;
    } else if (type.equals("Pointer") || type.equals("Object")) {
      AVObject parseObject = objectFromClassName((String) map.get("className"));
      map.remove("__type");
      AVUtils.copyPropertiesFromMapToAVObject(map, parseObject);
      return parseObject;
    } else if (type.equals("GeoPoint")) {
      return AVUtils.geoPointFromMap(map);
    } else if (type.equals("Bytes")) {
      return AVUtils.dataFromMap(map);
    } else if (type.equals("Date")) {
      return AVUtils.dateFromMap(map);
    } else if (type.equals("Relation")) {
      return AVUtils.objectFromRelationMap(map);
    } else if (type.equals("File")) {
      return AVUtils.fileFromMap(map);
    }
    return map;
  }

  static Object getObjectFrom(Object obj) {
    if (obj instanceof Collection) {
      return getObjectFrom((Collection) obj);
    } else if (obj instanceof Map) {
      return getObjectFrom((Map<String, Object>) obj);
    }

    return obj;
  }

  // ================================================================================
  // String Utils
  // ================================================================================
  public static String md5(String string) {
    byte[] hash = null;
    try {
      hash = string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Huh,UTF-8 should be supported?", e);
    }
    return computeMD5(hash);
  }

  static Random random = new Random();

  public static String getRandomString(int length) {
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder randomString = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      randomString.append(letters.charAt(random.nextInt(letters.length())));
    }

    return randomString.toString();
  }

  static AtomicInteger acu = new AtomicInteger(-65536);

  public static int getNextIMRequestId() {
    int val = acu.incrementAndGet();
    if (val > 65535) {
      while (val > 65535 && !acu.compareAndSet(val, -65536)) {
        val = acu.get();
      }
      return val;
    } else {
      return val;
    }
  }

  // ================================================================================
  // NetworkUtil
  // ================================================================================

  public static long getCurrentTimestamp() {
    return System.currentTimeMillis();
  }

  public static String joinCollection(Collection<String> collection, String separator) {
    StringBuilder builder = new StringBuilder();
    boolean wasFirst = true;
    for (String value : collection) {
      if (wasFirst) {
        wasFirst = false;
        builder.append(value);
      } else {
        builder.append(separator).append(value);
      }
    }
    return builder.toString();
  }

  public static String stringFromBytes(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return null;
  }

  public static String fileMd5(String fileName) throws IOException {
    return computeMD5(readFile(fileName));
  }

  public static byte[] readFile(String file) throws IOException {
    return readFile(new File(file));
  }

  public static byte[] readFile(File file) throws IOException {
    // Open file
    RandomAccessFile f = new RandomAccessFile(file, "r");

    try {
      // Get and check length
      long longlength = f.length();
      int length = (int) longlength;
      if (length != longlength)
        throw new IOException("File size >= 2 GB");

      // Read file and return data
      byte[] data = new byte[length];
      f.readFully(data);
      return data;
    } finally {
      closeQuietly(f);
    }
  }

  public static String computeMD5(byte[] input) {
    try {
      if (null == input) {
        return null;
      }
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(input, 0, input.length);
      byte[] md5bytes = md.digest();

      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < md5bytes.length; i++) {
        String hex = Integer.toHexString(0xff & md5bytes[i]);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  static String getJSONString(com.alibaba.fastjson.JSONObject object, final String key,
      final String defaultValue) {
    if (object.containsKey(key)) {
      return object.getString(key);
    }
    return defaultValue;
  }

  static long getJSONInteger(com.alibaba.fastjson.JSONObject object, final String key,
      long defaultValue) {
    if (object.containsKey(key)) {
      return object.getInteger(key);
    }
    return defaultValue;
  }

  public static final int TYPE_WIFI = 1;
  public static final int TYPE_MOBILE = 2;
  public static final int TYPE_NOT_CONNECTED = 0;

  // TODO return file name for different eventually request
  public static String getArchiveRequestFileName(String objectId, String _internalId,
      String method, String relativePath, String paramString) {
    // 当数据是更新时，说明已经有了ObjectId，那么paramString其实是增量数据，所以需要分开文件
    if (method.equalsIgnoreCase("put")) {
      return AVUtils.md5(relativePath + paramString);
    }
    // 当对象尚未在服务器创建时，每一次请求都是全数据的，所以只需要最新的那份数据即可
    else if (method.equalsIgnoreCase("post")) {
      return _internalId;
    } else if (method.equalsIgnoreCase("delete")) {
      // 倘若都没有ObjectId就已经出现代码删除，则直接覆盖
      return AVUtils.isBlankString(objectId) ? _internalId : AVUtils
          .md5(relativePath + paramString);
    }
    return AVUtils.md5(relativePath + paramString);
  }

  public static int collectionNonNullCount(Collection collection) {
    int count = 0;
    Iterator iterator = collection.iterator();
    while (iterator.hasNext()) {
      if (iterator.next() != null) {
        count++;
      }
    }
    return count;
  }

  public static String urlCleanLastSlash(String url) {
    if (!AVUtils.isBlankString(url) && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    } else {
      return url;
    }
  }

  public static String getSessionKey(String selfId) {
    StringBuilder sb = new StringBuilder(AVOSCloud.applicationId);
    sb.append(selfId);
    return sb.toString();
  }

  public static boolean isEmptyList(List e) {
    return e == null || e.isEmpty();
  }

  public static void ensureElementsNotNull(List<String> e, String errorLog) {
    for (String i : e) {
      if (i == null) {
        throw new NullPointerException(errorLog);
      }
    }
  }

  /*
   * true when firstNumber is bigger false when firstNumber is smaller
   */
  public static boolean compareNumberString(String firstNumber, String secondNumber) {
    return (Double.compare(Double.parseDouble(firstNumber), Double.parseDouble(secondNumber)) == 1);
  }

  public static String Base64Encode(String data) {
    return Base64.encodeToString(data.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
  }

  public static PaasClient.AVHttpClient getDirectlyClientForUse() {
    return PaasClient.storageInstance().clientInstance();
  }

  public static Map<String, Object> createMap(String cmp, Object value) {
    Map<String, Object> dict = new HashMap<String, Object>();
    dict.put(cmp, value);
    return dict;
  }

  public static boolean checkResponseType(int statusCode, String content, String contentType,
      GenericObjectCallback callback) {
    if (statusCode > 0 && !PaasClient.isJSONResponse(contentType, content)) {
      if (callback != null) {
        callback.onFailure(statusCode, new AVException(AVException.INVALID_JSON,
            "Wrong response content type:" + contentType), content);
      }
      return true;
    }
    return false;
  }

  public static String getHostName(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  public static String getAVObjectClassName(Class<? extends AVObject> clazz) {
    return AVObject.getSubClassName(clazz);
  }

  public static String getAVObjectCreatedAt(AVObject object) {
    return object.createdAt;
  }

  public static String getAVObjectUpdatedAt(AVObject object) {
    return object.updatedAt;
  }

  public static String getEncodeUrl(String url, Map<String, String> params) {
    return new AVRequestParams(params).getWholeUrl(url);
  }

  public static String getJSONValue(String msg, String key) {
    Map<String, Object> jsonMap = JSON.parseObject(msg, HashMap.class);
    if (jsonMap == null || jsonMap.isEmpty())
      return null;

    Object action = jsonMap.get(key);
    return action != null ? action.toString() : null;
  }

  public static boolean equals(String a, String b) {
    if (a == b)
      return true;
    int length;
    if (a != null && b != null && (length = a.length()) == b.length()) {
      if (a instanceof String && b instanceof String) {
        return a.equals(b);
      } else {
        for (int i = 0; i < length; i++) {
          if (a.charAt(i) != b.charAt(i))
            return false;
        }
        return true;
      }
    }
    return false;
  }

  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null)
        closeable.close();
    } catch (IOException e) {
      LogUtil.log.d(e.toString());
    }
  }

  public static byte[] readContentBytesFromFile(File fileForRead) {
    if (fileForRead == null) {
      LogUtil.avlog.e("null file object.");
      return null;
    };
    if (!fileForRead.exists() || !fileForRead.isFile()) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.log.d("not file object", new FileNotFoundException());
      }
      return null;
    }
    byte[] data = null;
    InputStream input = null;
    try {
      data = new byte[(int) fileForRead.length()];
      int totalBytesRead = 0;
      input = new BufferedInputStream(new FileInputStream(fileForRead), 8192);
      while (totalBytesRead < data.length) {
        int bytesRemaining = data.length - totalBytesRead;
        int bytesRead = input.read(data, totalBytesRead, bytesRemaining);
        if (bytesRead > 0) {
          totalBytesRead = totalBytesRead + bytesRead;
        }
      }
      return data;
    } catch (IOException e) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.log.e("Exception during file read", e);
      }
    } finally {
      closeQuietly(input);
    }
    return null;
  }
}
