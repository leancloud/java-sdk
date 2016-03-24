package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.okhttp.Call;
import com.avos.avoscloud.okhttp.Interceptor;
import com.avos.avoscloud.okhttp.MediaType;
import com.avos.avoscloud.okhttp.OkHttpClient;
import com.avos.avoscloud.okhttp.Request;
import com.avos.avoscloud.okhttp.RequestBody;
import com.avos.avoscloud.okhttp.Response;
import com.avos.avoscloud.okhttp.ResponseBody;
import com.avos.avoscloud.okhttp.internal.framed.Header;
import com.avos.avoscloud.okio.Buffer;
import com.avos.avoscloud.okio.BufferedSource;
import com.avos.avoscloud.okio.ForwardingSource;
import com.avos.avoscloud.okio.Okio;
import com.avos.avoscloud.okio.Source;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class PaasClient {
  private static final CookieHandler cookieHandler = new CookieHandler() {
    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> map) throws IOException {
      return Collections.emptyMap();
    }

    @Override
    public void put(URI uri, Map<String, List<String>> map) throws IOException {

    }
  };

  private static String baseUrl;
  private final String apiVersion;

  private static String applicationIdField;

  private static String apiKeyField;

  protected static String sessionTokenField;
  private static boolean isUrulu = true;
  private static boolean isCN = true;
  private boolean isProduction = true;

  private static final String defaultEncoding = "UTF-8";
  public static final String defaultContentType = "application/json";
  public static final String DEFAULT_FAIL_STRING = "request failed!!!";

  public static final String sdkVersion = "v3.13-SNAPSHOT";

  private static final String userAgent = "AVOS Cloud java-" + sdkVersion + " SDK";
  private AVUser currentUser = null;
  private AVACL defaultACL;

  private volatile AVHttpClient httpClient;
  private static boolean lastModifyEnabled = false;
  private static String REQUEST_STATIS_HEADER = "X-Android-RS";

  static Map<String, String> serviceHostMap = Collections
      .synchronizedMap(new HashMap<String, String>());
  static HashMap<String, PaasClient> serviceClientMap = new HashMap<String, PaasClient>();
  static Map<String, AVObjectReferenceCount> internalObjectsForEventuallySave = Collections
      .synchronizedMap(new HashMap<String, AVObjectReferenceCount>());
  static {
    serviceHostMap.put(AVOSServices.STORAGE_SERVICE.toString(), "https://api.leancloud.cn");
  }

  private static Map<String, String> lastModify = Collections
      .synchronizedMap(new WeakHashMap<String, String>());

  void setProduction(boolean production) {
    isProduction = production;
  }

  public static boolean isAVOSCloud() {
    return isUrulu;
  }

  protected static PaasClient sharedInstance(AVOSServices service) {

    String host =
        AVUtils.isBlankString(serviceHostMap.get(service.toString())) ? serviceHostMap
            .get(AVOSServices.STORAGE_SERVICE.toString()) : serviceHostMap.get(service.toString());
    PaasClient instance = serviceClientMap.get(host);
    if (instance == null) {
      instance = new PaasClient();
      instance.setBaseUrl(host);
      serviceClientMap.put(host, instance);
    }
    return instance;
  }

  public static PaasClient storageInstance() {
    return sharedInstance(AVOSServices.STORAGE_SERVICE);
  }

  public static PaasClient cloudInstance() {
    return sharedInstance(AVOSServices.FUNCTION_SERVICE);
  }

  public static PaasClient statistisInstance() {
    return sharedInstance(AVOSServices.STATISTICS_SERVICE);
  }

  AVACL getDefaultACL() {
    return defaultACL;
  }

  void setDefaultACL(AVACL acl) {
    defaultACL = acl;
  }

  AVUser getCurrentUser() {
    return currentUser;
  }

  public Map<String, String> userHeaderMap() {
    AVUser user = AVUser.getCurrentUser();
    if (user != null) {
      return user.headerMap();
    }
    return null;
  }

  void setCurrentUser(AVUser user) {
    currentUser = user;
  }


  private PaasClient() {
    apiVersion = "1.1";

    if (isUrulu) {
      useUruluServer();
    }
  }

  private String signRequest() {
    StringBuilder builder = new StringBuilder();
    long ts = AVUtils.getCurrentTimestamp();
    StringBuilder result = new StringBuilder();
    result.append(AVUtils.md5(builder.append(ts).append(AVOSCloud.clientKey).toString())
        .toLowerCase());
    return result.append(',').append(ts).toString();
  }

  protected void updateHeaders(Request.Builder builder, Map<String, String> header,
      boolean needRequestStatistic) {
    // if the field isnt exist, the server will assume it's true

    builder.header("X-LC-Prod", isProduction ? "1" : "0");
    AVUser currAVUser = AVUser.getCurrentUser();
    builder.header(sessionTokenField,
        (currAVUser != null && currAVUser.getSessionToken() != null) ? currAVUser.getSessionToken()
            : "");
    builder.header(applicationIdField, AVOSCloud.applicationId);
    builder.header("Accept", defaultContentType);
    builder.header("Content-Type", defaultContentType);
    builder.header("User-Agent", userAgent);
    builder.header("X-LC-Sign", signRequest());


    if (header != null) {
      for (Map.Entry<String, String> entry : header.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }

    if (needRequestStatistic) {
      builder.header(REQUEST_STATIS_HEADER, "1");
    }
  }

  public synchronized AVHttpClient clientInstance() {
    if (httpClient == null) {
      httpClient = new AVHttpClient();
    }
    httpClient.setConnectTimeout(AVOSCloud.getNetworkTimeout(), TimeUnit.MILLISECONDS);
    return httpClient;
  }



  public void useUruluServer() {
    if (isCN) {
      useAVCloudCN();
    } else {
      useAVCloudUS();
    }
  }

  public static void useAVCloudUS() {
    isUrulu = true;
    isCN = false;
    baseUrl = "https://us-api.leancloud.cn";
    serviceHostMap.put(AVOSServices.STORAGE_SERVICE.toString(), baseUrl);
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
    AVOSCloud.setStorageType(AVOSCloud.StorageType.StorageTypeS3);
    switchPushRouter("useAVOSCloudUS");
  }

  private static void switchPushRouter(String routerServer) {
    try {
      Class<?> avPushRouterClass = Class.forName("com.avos.avospush.push.AVPushRouter");
      Method switchMethod = avPushRouterClass.getMethod(routerServer);
      switchMethod.invoke(avPushRouterClass);
    } catch (Exception e) {
      LogUtil.avlog.i("avpushRouter server didn't switched");
    }
  }

  public static void useAVCloudCN() {
    isUrulu = true;
    baseUrl = "https://api.leancloud.cn";
    serviceHostMap.put(AVOSServices.STORAGE_SERVICE.toString(), baseUrl);
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
    AVOSCloud.setStorageType(AVOSCloud.StorageType.StorageTypeQiniu);
    switchPushRouter("useAVOSCloudCN");
  }

  public static void useLocalStg() {
    isUrulu = true;
    baseUrl = "https://cn-stg1.avoscloud.com";
    serviceHostMap.put(AVOSServices.STORAGE_SERVICE.toString(), baseUrl);
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
    AVOSCloud.setStorageType(AVOSCloud.StorageType.StorageTypeQiniu);
  }

  public String buildUrl(final String path) {
    return String.format("%s/%s/%s", baseUrl, apiVersion, path);
  }

  public String buildUrl(final String path, AVRequestParams params) {
    String endPoint = buildUrl(path);
    if (params == null || params.isEmpty()) {
      return endPoint;
    } else {
      return params.getWholeUrl(endPoint);
    }

  }

  private String batchUrl() {
    return String.format("%s/%s/batch", baseUrl, apiVersion);
  }

  private String batchSaveRelativeUrl() {
    return "batch/save";
  }

  private AsyncHttpResponseHandler createGetHandler(GenericObjectCallback callback,
      String absoluteURLString) {
    AsyncHttpResponseHandler handler = new GetHttpResponseHandler(callback, absoluteURLString);
    return handler;
  }

  private AsyncHttpResponseHandler createPostHandler(GenericObjectCallback callback) {
    AsyncHttpResponseHandler handler = new PostHttpResponseHandler(callback);
    return handler;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setBaseUrl(final String url) {
    baseUrl = url;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  protected static void setServiceHost(AVOSServices service, String host) {
    serviceHostMap.put(service.toString(), host);
  }

  public String getObject(final String relativePath, final AVRequestParams parameters,
      final boolean sync, final Map<String, String> header, final GenericObjectCallback callback) {
    final String absoluteURLString = buildUrl(relativePath, parameters);
    getObject(relativePath, parameters, sync, header, callback,false);
    return absoluteURLString;
  }

  String generateQueryPath(final String relativePath, final AVRequestParams parameters) {
    return buildUrl(relativePath, parameters);
  }

  public void getObject(final String relativePath, final AVRequestParams parameters,
      final boolean sync, final Map<String, String> inputHeader, GenericObjectCallback callback,
      final boolean fetchRetry) {
    Map<String, String> myHeader = inputHeader;
    if (inputHeader == null) {
      myHeader = new HashMap<String, String>();
    }

    String url = buildUrl(relativePath, parameters);
    AsyncHttpResponseHandler handler = createGetHandler(callback, url);
    if (AVOSCloud.isDebugLogEnabled()) {
      dumpHttpGetRequest(buildUrl(relativePath),
          parameters == null ? null : parameters.getDumpQueryString());
    }
    AVHttpClient client = clientInstance();
    Request.Builder builder = new Request.Builder();
    builder.url(url).get();
    updateHeaders(builder, myHeader, callback != null && callback.isRequestStatisticNeed());
    client.execute(builder.build(), sync, handler);
  }

  public void putObject(final String relativePath, String object, boolean sync,
      Map<String, String> header, GenericObjectCallback callback, String objectId,
      String _internalId) {
    putObject(relativePath, object, sync, false, header, callback, objectId, _internalId);
  }


  public void putObject(final String relativePath, String object, boolean sync,
      boolean isEventually, Map<String, String> header, GenericObjectCallback callback,
      String objectId, String _internalId) {
    try {

      String url = buildUrl(relativePath);
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpPutRequest(header, url, object);
      }
      AVHttpClient client = clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(url).put(RequestBody.create(AVHttpClient.JSON, object));
      updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());
      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  private void processException(Exception e, GenericObjectCallback cb) {
    if (cb != null) {
      cb.onFailure(e, null);
    }
  }

  // path=/1/classes/Parent/a1QCssTp7r
  Map<String, Object> batchItemMap(String method, String path, Object body, Map params) {
    // String myPath = String.format("/%s/%s",
    // PaasClient.sharedInstance().apiVersion, path);
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("method", method);
    result.put("path", path);
    result.put("body", body);
    if (params != null) {
      result.put("params", params);
    }
    return result;
  }

  Map<String, Object> batchItemMap(String method, String path, Object body) {
    return this.batchItemMap(method, path, body, null);
  }

  @Deprecated
  List<Object> assembleBatchOpsList(List<Object> itemList, String path) {
    List<Object> list = new ArrayList<Object>();
    for (Object object : itemList) {
      Map<String, Object> opDict = batchItemMap("PUT", path, object);
      list.add(opDict);
    }
    return list;
  }

  private Map<String, Object> batchRequest(List<Object> list) {
    Map<String, Object> requests = new HashMap<String, Object>();
    requests.put("requests", list);
    return requests;
  }

  // only called @sendPendingOps
  public void postBatchObject(List<Object> parameters, boolean sync, Map<String, String> header,
      GenericObjectCallback callback) {
    try {
      String url = batchUrl();
      Map<String, Object> requests = batchRequest(parameters);
      String json = JSON.toJSONString(requests);
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpPostRequest(header, url, json);
      }
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      AVHttpClient client = clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(url).post(RequestBody.create(AVHttpClient.JSON, json));
      updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());

      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }


  public void postBatchSave(final List list, final boolean sync, final boolean isEventually,
      final Map<String, String> header, final GenericObjectCallback callback,
      final String objectId, final String _internalId) {
    try {
      Map params = new HashMap();
      params.put("requests", list);
      String paramString = AVUtils.jsonStringFromMapWithNull(params);

      String url = buildUrl(batchSaveRelativeUrl());
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpPostRequest(header, url, paramString);
      }
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      AVHttpClient client = clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(url).post(RequestBody.create(AVHttpClient.JSON, paramString));
      updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());
      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  public void postObject(final String relativePath, String object, boolean sync,
      GenericObjectCallback callback) {
    postObject(relativePath, object, sync, false, callback, null, null);
  }

  public void postObject(final String relativePath, String object, boolean sync,
      boolean isEventually, GenericObjectCallback callback, String objectId, String _internalId) {
    try {
      String url = buildUrl(relativePath);
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpPostRequest(null, url, object);
      }
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      AVHttpClient client = clientInstance();
      Request.Builder builder = new Request.Builder();
      updateHeaders(builder, null, callback != null && callback.isRequestStatisticNeed());
      builder.url(url).post(RequestBody.create(AVHttpClient.JSON, object));
      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  public void deleteObject(final String relativePath, boolean sync, GenericObjectCallback callback,
      String objectId, String _internalId) {
    deleteObject(relativePath, sync, false, callback, objectId, _internalId);
  }


  public void deleteObject(final String relativePath, boolean sync, boolean isEventually,
      GenericObjectCallback callback, String objectId, String _internalId) {
    try {
      String url = buildUrl(relativePath);
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpDeleteRequest(null, url, null);
      }
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      AVHttpClient client = clientInstance();
      Request.Builder builder = new Request.Builder();
      updateHeaders(builder, null, callback != null && callback.isRequestStatisticNeed());

      builder.url(url).delete();
      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  // ================================================================================
  // For Debug
  // ================================================================================

  public void dumpHttpGetRequest(String path, String parameters) {
    String string = "";
    if (parameters != null) {
      string =
          String.format("curl -X GET -H \"%s: %s\" -H \"%s: %s\" -G --data-urlencode \'%s\' %s",
              applicationIdField, AVOSCloud.applicationId, apiKeyField, getDebugClientKey(),
              parameters, path);
    } else {
      string =
          String.format("curl -X GET -H \"%s: %s\" -H \"%s: %s\"  %s", applicationIdField,
              AVOSCloud.applicationId, apiKeyField, getDebugClientKey(), path);
    }
    LogUtil.avlog.d(string);
  }

  private String getDebugClientKey() {
    if (AVOSCloud.showInternalDebugLog()) {
      return AVOSCloud.clientKey;
    } else {
      return "YourAppKey";
    }
  }

  private String headerString(Map<String, String> header) {
    String string =
        String.format(" -H \"%s: %s\" -H \"%s: %s\" ", applicationIdField, AVOSCloud.applicationId,
            apiKeyField, getDebugClientKey());
    StringBuilder sb = new StringBuilder(string);
    if (header != null) {
      for (Map.Entry<String, String> entry : header.entrySet()) {
        String item = String.format(" -H \"%s: %s\" ", entry.getKey(), entry.getValue());
        sb.append(item);
      }
    }
    sb.append(" -H \"Content-Type: application/json\" ");
    return sb.toString();
  }

  public void dumpHttpPutRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X PUT %s  -d \' %s \' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public void dumpHttpPostRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X POST %s  -d \'%s\' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public void dumpHttpDeleteRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X DELETE %s  -d \'%s\' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public static String getLastModify(final String absolutURLString) {
    if (!PaasClient.isLastModifyEnabled()) {
      return null;
    }
    return lastModify.get(absolutURLString);
  }

  public static boolean isLastModifyEnabled() {
    return lastModifyEnabled;
  }

  public static void setLastModifyEnabled(boolean e) {
    lastModifyEnabled = e;
  }

  public static boolean isJSONResponse(String contentType, String content) {
    boolean result = false;
    if (!AVUtils.isBlankString(contentType)) {
      result = contentType.toLowerCase().contains("application/json");
    }
    if (!result) {
      result = isJSONResponseContent(content);
    }
    return result;
  }

  public static boolean isJSONResponseContent(String content) {
    try {
      JSON.parse(content);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  protected static String extractContentType(Header[] headers) {
    if (headers != null) {
      for (Header h : headers) {

        if (h.name.toString().equalsIgnoreCase("Content-Type")) {
          return h.value.toString();
        }
      }
    }
    return null;
  }

  public static boolean updateLastModify(final String absolutURLString, final String ts) {
    if (!isLastModifyEnabled()) {
      return false;
    }

    if (!AVUtils.isBlankString(ts)) {
      lastModify.put(absolutURLString, ts);
      return true;
    }
    return false;
  }

  public static void removeLastModifyForUrl(final String absolutURLString) {
    lastModify.remove(absolutURLString);
  }

  protected static void registerEventuallyObject(AVObject object) {
    if (object != null) {
      synchronized (object) {
        AVObjectReferenceCount counter = internalObjectsForEventuallySave.get(object.internalId());
        if (counter != null) {
          counter.increment();
        } else {
          counter = new AVObjectReferenceCount(object);
          internalObjectsForEventuallySave.put(object.internalId(), counter);
        }
      }
    }
  }

  protected static void unregisterEvtuallyObject(AVObject object) {
    if (object != null) {
      synchronized (object) {
        AVObjectReferenceCount counter =
            internalObjectsForEventuallySave.get(object.internalId()) == null ? internalObjectsForEventuallySave
                .get(object.internalId()) : internalObjectsForEventuallySave.get(object.getUuid());
        if (counter != null) {
          if (counter.desc() <= 0) {
            internalObjectsForEventuallySave.remove(object.internalId());
            internalObjectsForEventuallySave.remove(object.getUuid());
          }
        }
      }
    }
  }

  private static Comparator<File> fileModifiedDateComparator = new Comparator<File>() {
    @Override
    public int compare(File f, File s) {
      return (int) (f.lastModified() - s.lastModified());
    }
  };

  public static class AVHttpClient {
    OkHttpClient client;
    public static final MediaType JSON = MediaType.parse(defaultContentType);

    public AVHttpClient() {
      client = new OkHttpClient();
      client.setCookieHandler(cookieHandler);
    }

    public void execute(Request request, boolean sync, final AsyncHttpResponseHandler handler) {
      Call call = getCall(request);
      if (sync) {
        try {
          Response response = call.execute();
          handler.onResponse(response);
        } catch (IOException e) {
          handler.onFailure(request, e);
        }
      } else {
        call.enqueue(handler);
      }
    }

    private synchronized Call getCall(Request request) {
      return client.newCall(request);
    }

    public void setConnectTimeout(long networkTimeout, TimeUnit timeUnit) {
      client.setConnectTimeout(networkTimeout, timeUnit);
    }
  }
}
