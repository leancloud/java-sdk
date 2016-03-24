package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import junit.textui.ResultPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 12/25/13 Time: 9:58 AM To change this template
 * use File | Settings | File Templates.
 */
@AVClassName("_Status")
@JSONType(ignores = {"acl", "updatedAt", "uuid"})
public class AVStatus extends AVObject {

  private final Map<String, Object> dataMap = new ConcurrentHashMap<String, Object>();
  public static final String IMAGE_TAG = "image";
  public static final String MESSAGE_TAG = "message";
  private static final String AV_CLASS_NAME = "_FeedStatus";
  private static int DEFAULT_COUNT = 100;
  @Deprecated
  public static final String INBOX_TIMELINE = "default";
  @Deprecated
  public static final String INBOX_PRIVATE = "private";

  public static final String STATUS_END_POINT = "statuses";

  public enum INBOX_TYPE {
    TIMELINE("default"), PRIVATE("private");
    private String type;

    private INBOX_TYPE(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return this.type;
    }
  }

  static private final String UNREAD_TAG = "unread";
  private long messageId = 0;
  private String createdAt;
  private String inboxType;
  private AVObject source = null;
  private AVQuery query = null;
  static List<String> ignoreList = Arrays.asList("objectId", "updatedAt", "createdAt", "inboxType",
      "messageId");

  static {
    AVPowerfulUtils.createSettings(AVStatus.class.getSimpleName(), "statuses", "_Status");
    AVPowerfulUtils.createSettings("_Status", "statuses", "_Status");
    AVObject.registerSubclass(AVStatus.class);
  }

  static String userClassName() {
    return AVPowerfulUtils.getAVClassName(AVStatus.class.getSimpleName());
  }

  public static AVStatus createStatus(String imageUrl, String message) {
    AVStatus status = new AVStatus();
    status.setImageUrl(imageUrl);
    status.setMessage(message);
    return status;
  }

  public static AVStatus createStatusWithData(Map<String, Object> data) {
    AVStatus status = new AVStatus();
    status.setData(data);
    return status;
  }

  public AVStatus() {
    super();
  }

  private static boolean checkCurrentUser(final AVCallback callback) {
    if (AVUser.getCurrentUser() == null) {
      if (callback != null) {
        callback.internalDone(null, AVErrorUtils.sessionMissingException());
      }
      return false;
    }
    return true;
  }

  @Override
  public String getObjectId() {
    return objectId;
  }

  @Override
  public Date getCreatedAt() {
    return AVUtils.dateFromString(createdAt);
  }

  @Override
  protected void setCreatedAt(String date) {
    this.createdAt = date;
  }

  public void setImageUrl(final String url) {
    if (url != null) {
      dataMap.put(IMAGE_TAG, url);
    }
  }

  public String getImageUrl() {
    Object obj = dataMap.get(IMAGE_TAG);
    if (obj instanceof String) {
      return (String) obj;
    } else {
      return null;
    }
  }

  /**
   * 获取Status的发送者
   * 
   * @return
   */
  public AVUser getSource() {
    return (AVUser) source;
  }

  public void setSource(AVObject source) {
    this.source = source;
  }

  public void setInboxType(final String type) {
    if (type != null) {
      this.inboxType = type;
    }
  }

  public void setQuery(AVQuery query) {
    this.query = query;
  }

  public void setMessage(final String message) {
    if (message != null) {
      dataMap.put(MESSAGE_TAG, message);
    }
  }

  public String getMessage() {
    Object obj = dataMap.get(MESSAGE_TAG);
    if (obj instanceof String) {
      return (String) obj;
    } else {
      return null;
    }
  }

  public void setData(Map<String, Object> data) {
    dataMap.putAll(data);
  }

  public Map<String, Object> getData() {
    return dataMap;
  }

  /**
   * 添加AVStatus中的一对自定义内容
   * 
   * @param key
   * @param value
   */
  @Override
  public void put(String key, Object value) {
    // TODO Auto-generated method stub
    dataMap.put(key, value);
  }


  @Override
  protected void put(String key, Object value, boolean pending) {
    if ("inboxType".equals(key)) {
      if (value instanceof String) {
        this.inboxType = (String) value;
      }
    } else if ("messageId".equals(key)) {
      if (value instanceof Number) {
        this.messageId = ((Number) value).longValue();
      }
    } else if ("source".equals(key)) {
      if (value instanceof AVObject) {
        this.source = (AVObject) value;
      }
    } else {
      dataMap.put(key, value);
    }
  }

  /**
   * 删除AVStatus中的一对自定义内容
   * 
   * @param key
   */
  @Override
  public void remove(String key) {
    // TODO Auto-generated method stub
    this.dataMap.remove(key);
  }

  /**
   * 此状态在用户Inbox中的ID
   * 
   * @warning 仅用于分片查询,不具有唯一性
   */
  public long getMessageId() {
    return messageId;
  }

  protected void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  /**
   * 到达收件箱类型, 默认是`default`,私信是`private`, 可以自定义任何类型
   */
  public String getInboxType() {
    return inboxType;
  }

  /**
   * 删除当前的状态
   *
   */
  public void deleteStatus() {
    deleteStatusWithID(this.objectId);
  }

  /**
   * 删除当前用户发布的某条状态
   * 
   * @param statusId 状态的objectId
   * @param callback 回调结果
   */
  public static void deleteStatusWithID(String statusId) {
    deleteStatusWithId(true, statusId, null);
  }

  private static void deleteStatusWithId(boolean sync, String statusId,
      final DeleteCallback callback) {

    if (!checkCurrentUser(null)) {
      if (callback != null) {
        callback.internalDone(AVErrorUtils.sessionMissingException());
      }
      return;
    }

    if (AVUtils.isBlankString(statusId)) {
      if (callback != null) {
        callback.internalDone(AVErrorUtils.invalidObjectIdException());
      }
      return;
    }
    String endPoint = String.format("statuses/%s", statusId);
    PaasClient.storageInstance().deleteObject(endPoint, sync, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        if (callback != null) {
          callback.internalDone(null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (callback != null) {
          callback.internalDone(AVErrorUtils.createException(error, content));
        }
      }
    }, statusId, null);
  }

  /**
   * 删除收件箱消息
   * 
   * @param messageId 消息的messageId
   * @param inboxType 收件箱类型
   * @param owner 消息所有者
   * @throws Exception
   */
  public static void deleteInboxStatus(long messageId, String inboxType, AVUser owner)
      throws Exception {
    deleteInboxStatus(true, messageId, inboxType, owner, new DeleteCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  private static void deleteInboxStatus(boolean sync, long statusId, String inboxType,
      AVUser owner, final DeleteCallback callback) {
    String ownerString = null;
    if (owner != null) {
      String ownerId = owner.getObjectId();
      Map<String, Object> ownerMap = AVUtils.mapFromUserObjectId(ownerId);
      ownerString = JSON.toJSONString(ownerMap);
    } else {
      if (callback != null) {
        callback
            .internalDone(new AVException(AVException.USER_DOESNOT_EXIST, "Owner can't be null"));
      }
      return;
    }

    Map<String, String> params = new HashMap<String, String>();
    params.put("messageId", String.valueOf(statusId));
    params.put("inboxType", inboxType);
    params.put("owner", ownerString);

    String endPoint = AVUtils.getEncodeUrl("subscribe/statuses/inbox", params);
    PaasClient.storageInstance().deleteObject(endPoint, sync, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        if (callback != null) {
          callback.internalDone(null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (callback != null) {
          callback.internalDone(AVErrorUtils.createException(error, content));
        }
      }
    }, null, null);
  }

  /**
   * 获取当前用户发布的状态列表
   * 
   * @param skip 从某个状态id开始向下返回. 默认是`0`返回最新的.
   * @param limit 需要返回的条数 默认`100`，最大`100`
   * @param callback 回调结果
   */
  @Deprecated
  public static void getStatuses(long skip, long limit, final StatusListCallback callback) {
    if (!checkCurrentUser(callback)) {
      return;
    }
    String userId = AVUser.getCurrentUser().getObjectId();
    Map<String, String> params = statusQueryMap(userId, skip, limit, 0, null, null, true, false);
    getStatusImpl(STATUS_END_POINT, params, callback);
  }

  @Deprecated
  static Map<String, String> sourceQueryMap(String ownerId, long skip, long count) {
    Map<String, Object> source = new HashMap<String, Object>();
    Map<String, String> result = new HashMap<String, String>();
    try {
      source.put("source", AVUtils.mapFromUserObjectId(ownerId));
      result.put("where", JSON.toJSONString(source));
      result.put("include", "source");
      if (skip > 0) {
        result.put("skip", Long.toString(skip));
      }
      if (count > 0) {
        result.put("count", Long.toString(count));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    return result;
  }

  /**
   * 获取当前用户发布的状态
   * 
   * @param type 状态类型,默认是kAVStatusTypeTimeline, 可以是任意自定义字符串
   * @param skip 跳过条数
   * @param limit 需要返回的条数 默认`100`，最大`100`
   * @param callback 回调结果
   */
  @Deprecated
  public static void getStatusesFromCurrentUserWithType(final String type, long skip, long limit,
      final StatusListCallback callback) {
    if (!checkCurrentUser(callback)) {
      return;
    }

    Map<String, String> params = sourceQueryMap(AVUser.getCurrentUser().getObjectId(), skip, limit);
    getStatusImpl(STATUS_END_POINT, params, callback);
  }

  @Deprecated
  static Map<String, String> statusQueryMap(String ownerId, long skip, long limit, long maxId,
      String inboxType, Map<String, Object> where, boolean includeSource, boolean count) {
    Map<String, Object> owner = AVUtils.mapFromUserObjectId(ownerId);
    Map<String, String> result = new HashMap<String, String>();
    try {
      result.put("owner", JSON.toJSONString(owner));
      if (skip > 0) {
        result.put("skip", Long.toString(skip));
      }
      if (limit > 0) {
        result.put("limit", Long.toString(limit));
      }
      if (maxId > 0) {
        result.put("maxId", Long.toString(maxId));
      }
      if (!AVUtils.isBlankString(inboxType)) {
        result.put("inboxType", inboxType);
      }
      if (where != null) {
        result.put("where", JSON.toJSONString(where));
      }
      if (includeSource) {
        result.put("include", "source");
      }
      if (count) {
        result.put("count", Long.toString(1));
      }
    } catch (Exception exception) {
      LogUtil.log.e(exception.toString());
    }
    return result;
  }

  static Map<String, String> getStatusQueryMap(String ownerId, long sinceId, long limit,
      long maxId, String inboxType, Map<String, Object> where, boolean includeSource, boolean count) {
    Map<String, Object> owner = AVUtils.mapFromUserObjectId(ownerId);
    Map<String, String> result = new HashMap<String, String>();
    try {
      result.put("owner", JSON.toJSONString(owner));
      if (sinceId > 0) {
        result.put("sinceId", Long.toString(sinceId));
      }
      if (limit > 0) {
        result.put("limit", Long.toString(limit));
      }
      if (maxId > 0) {
        result.put("maxId", Long.toString(maxId));
      }
      if (!AVUtils.isBlankString(inboxType)) {
        result.put("inboxType", inboxType);
      }
      if (where != null) {
        result.put("where", JSON.toJSONString(where));
      }
      if (includeSource) {
        result.put("include", "source");
      }
      if (count) {
        result.put("count", Long.toString(1));
      }
    } catch (Exception exception) {
      LogUtil.log.e(exception.toString());
    }
    return result;
  }

  static List<AVStatus> processStatusResultList(final String content) {
    if (AVUtils.isBlankContent(content)) {
      return Collections.emptyList();
    }

    com.alibaba.fastjson.JSONObject results = JSON.parseObject(content);
    com.alibaba.fastjson.JSONArray array = results.getJSONArray("results");
    List<AVStatus> result = new LinkedList<AVStatus>();
    for (Object item : array) {
      AVStatus object = new AVStatus();
      processStatusFromObject(item, object);
      result.add(object);
    }
    return result;
  }

  static void processStatus(final String content, final AVStatus status) {
    com.alibaba.fastjson.JSONObject object = JSON.parseObject(content);
    processStatusFromObject(object, status);
  }

  // TODO, move to AVUtils later.
  static void processStatusFromObject(final Object object, final AVStatus status) {

    com.alibaba.fastjson.JSONObject jsonObject = (com.alibaba.fastjson.JSONObject) object;
    status.objectId = AVUtils.getJSONString(jsonObject, "objectId", status.objectId);
    status.messageId = AVUtils.getJSONInteger(jsonObject, "messageId", status.messageId);
    status.inboxType = AVUtils.getJSONString(jsonObject, "inboxType", status.inboxType);
    status.createdAt = AVUtils.getJSONString(jsonObject, "createdAt", status.createdAt);

    status.setImageUrl(AVUtils.getJSONString(jsonObject, IMAGE_TAG, status.getImageUrl()));
    status.setMessage(AVUtils.getJSONString(jsonObject, MESSAGE_TAG, status.getMessage()));


    String sourceString = AVUtils.getJSONString(jsonObject, "source", "");
    if (!AVUtils.isBlankString(sourceString)) {
      Map<String, Object> map = JSON.parseObject(sourceString);
      status.source = AVUtils.parseObjectFromMap(map);
    }

    java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.Object>> entries =
        jsonObject.entrySet();
    for (Map.Entry entry : entries) {
      if (!ignoreList.contains(entry.getKey().toString()) && entry.getValue() != null) {
        status.dataMap.put(entry.getKey().toString(), AVUtils.getParsedObject(entry.getValue()));
      }
    }
  }

  static void getStatusImpl(final String endPoint, Map<String, String> map,
      final StatusListCallback callback) {
    AVRequestParams params = null;
    if (map != null) {
      params = new AVRequestParams(map);
    }

    PaasClient.storageInstance().getObject(endPoint, params, false, null,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            List<AVStatus> result = processStatusResultList(content);
            if (callback != null) {
              callback.internalDone(result, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        });
  }

  static int processStatusCount(String content) {
    com.alibaba.fastjson.JSONObject data = JSON.parseObject(content);
    return data.getInteger(UNREAD_TAG);
  }

  static void getStatusCountImpl(final String endPoint, Map<String, String> map,
      final CountCallback callback) {
    PaasClient.storageInstance().getObject(endPoint, new AVRequestParams(map), true, null,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            int count = processStatusCount(content);
            if (callback != null) {
              callback.internalDone(count, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(0, AVErrorUtils.createException(error, content));
            }
          }
        });
  }

  public static int getUnreadStatusesCount(String inboxType) throws AVException {
    final int[] value = {0};
    getUnreadStatusesCountInBackground(inboxType, new CountCallback() {

      @Override
      public void done(int count, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        } else {
          value[0] = count;
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return value[0];
  }

  private static void getUnreadStatusesCountInBackground(String inboxType,
      final CountCallback callback) {

    if (!checkCurrentUser(null)) {
      if (callback != null) {
        callback.internalDone(0, AVErrorUtils.sessionMissingException());
      }
      return;
    }
    String userId = AVUser.getCurrentUser().getObjectId();
    Map<String, String> map = getStatusQueryMap(userId, 0, 0, 0, inboxType, null, true, true);
    getStatusCountImpl("subscribe/statuses/count", map, callback);
  }

  static boolean checkStatusId(String statusId, StatusCallback callback) {
    if (AVUtils.isBlankString(statusId)) {
      if (callback != null) {
        callback.internalDone(null, AVErrorUtils.invalidObjectIdException());
      }
      return false;
    }
    return true;
  }

  public static AVStatus getStatusWithId(String statusId) throws AVException {
    if (!checkStatusId(statusId, null)) {
      throw AVErrorUtils.invalidObjectIdException();
    }
    if (!checkCurrentUser(null)) {
      throw AVErrorUtils.sessionMissingException();
    }
    String userId = AVUser.getCurrentUser().getObjectId();
    String endPoint = String.format("statuses/%s", statusId);
    Map<String, String> map = statusQueryMap(userId, 0, 0, 0, null, null, true, false);
    AVRequestParams params = new AVRequestParams(map);
    final AVStatus[] result = new AVStatus[1];
    PaasClient.storageInstance().getObject(endPoint, params, true, null,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVStatus status = new AVStatus();
            processStatus(content, status);
            result[0] = status;
          }

          @Override
          public void onFailure(Throwable error, String content) {
            AVExceptionHolder.add(AVErrorUtils.createException(error, content));
          }
        });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return result[0];
  }

  static void postStatusImpl(final AVStatus status, Map<String, Object> map,
      final SaveCallback callback, boolean sync) {
    String postData = AVUtils.restfulServerData(map);
    PaasClient.storageInstance().postObject(STATUS_END_POINT, postData, sync, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            processStatus(content, status);
            if (callback != null) {
              callback.internalDone(null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(AVErrorUtils.createException(error, content));
            }
          }
        }, status.getObjectId(), null);
  }

  static Map<String, Object> statusBody(AVStatus status, String inboxType,
      Map<String, Object> queryBody) {
    Map<String, Object> data = new HashMap<String, Object>();
    data.putAll(status.dataMap);
    Map<String, Object> body = new HashMap<String, Object>();
    if (status.source != null) {
      data.put("source", AVUtils.getParsedObject(status.source));
    } else {
      data.put("source", AVUtils.getParsedObject(AVUser.getCurrentUser()));
    }
    body.put("data", data);
    body.put("inboxType", inboxType);
    body.put("query", queryBody);
    return body;
  }

  // fastjson will add additional " to string. the server side
  // cannot handle it correctly, so have to serialize by using JSONHelper.
  // change later if we find a way to remove the ".
  Map<String, Object> myQueryParameters(AVQuery query) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    if (query.getWhere().keySet().size() > 0) {
      parameters.put("where", AVUtils.getParsedMap(query.getWhere()));
    }
    if (query.getLimit() > 0) {
      parameters.put("limit", Integer.toString(query.getLimit()));
    }
    if (query.getSkip() > 0) {
      parameters.put("skip", Integer.toString(query.getSkip()));
    }
    if (query.getOrder() != null && query.getOrder().length() > 0) {
      parameters.put("order", query.getOrder());
    }
    if (query.getInclude() != null && query.getInclude().size() > 0) {
      String value = AVUtils.joinCollection(query.getInclude(), ",");
      parameters.put("include", value);
    }
    if (query.getSelectedKeys() != null && query.getSelectedKeys().size() > 0) {
      String keys = AVUtils.joinCollection(query.getSelectedKeys(), ",");
      parameters.put("keys", keys);
    }
    return parameters;
  }

  public void send() throws AVException {
    sendInBackground(new SaveCallback() {

      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  private void sendInBackground(final SaveCallback callback) {
    if (!checkCurrentUser(callback)) {
      return;
    }

    if (query == null) {
      AVStatus.sendStatusToFollowersInBackgroud(this, callback);
      return;
    }

    Map<String, Object> queryBody = new HashMap<String, Object>();
    Map<String, Object> parameters = myQueryParameters(query);
    queryBody.putAll(parameters);
    queryBody.put("className", query.getClassName());
    // 之前好像直接设成Timeline，而inboxType会永远被忽略掉。
    Map<String, Object> body =
        statusBody(this, AVUtils.isBlankString(this.inboxType) ? INBOX_TYPE.TIMELINE.toString()
            : this.inboxType, queryBody);
    postStatusImpl(this, body, callback, true);
  }


  /**
   * 向用户的粉丝发送新状态
   * 
   * @param status 要发送的状态
   * @param callback 回调结果
   */
  static private void sendStatusToFollowersInBackgroud(AVStatus status, final SaveCallback callback) {
    if (!checkCurrentUser(callback)) {
      return;
    }

    // ignore query in status so far, otherwise have to merge them.
    Map<String, Object> queryBody = new HashMap<String, Object>();
    queryBody.put(AVUtils.classNameTag, "_Follower");
    queryBody.put("keys", "follower");
    queryBody.put("where", currentUserBody());
    Map<String, Object> body =
        statusBody(status, AVUtils.isBlankString(status.inboxType) ? INBOX_TYPE.TIMELINE.toString()
            : status.inboxType, queryBody);
    postStatusImpl(status, body, callback, true);
  }

  /**
   * 向用户发私信
   * 
   * @param status 要发送的状态
   * @param receiverObjectId 接受私信的用户objectId
   * @throws AVException 发送可能出现的异常
   */
  public static void sendPrivateStatus(AVStatus status, final String receiverObjectId)
      throws AVException {
    sendPrivateStatusInBackgroud(status, receiverObjectId, new SaveCallback() {

      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }


  static private void sendPrivateStatusInBackgroud(AVStatus status, final String receiverObjectId,
      SaveCallback callback) {
    if (!checkCurrentUser(callback)) {
      return;
    }

    // ignore query in status so far, otherwise have to merge them.
    Map<String, Object> queryBody = new HashMap<String, Object>();
    queryBody.put(AVUtils.classNameTag, "_User");
    Map<String, Object> whereBody = new HashMap<String, Object>();
    whereBody.put("objectId", receiverObjectId);
    queryBody.put("where", whereBody);
    Map<String, Object> body = statusBody(status, INBOX_TYPE.PRIVATE.toString(), queryBody);
    postStatusImpl(status, body, callback, true);
  }

  static private Map<String, Object> currentUserBody() {
    Map<String, Object> userBody = new HashMap<String, Object>();
    Map<String, Object> object = AVUtils.mapFromPointerObject(AVUser.getCurrentUser());
    userBody.put("user", object);
    return userBody;
  }

  /**
   * 返回一个AVStatusQuery对象，用来查询用户发件箱内容
   * 
   * @param owner
   * @return
   * @throws AVException
   */
  public static AVStatusQuery statusQuery(AVUser owner) throws AVException {
    AVStatusQuery query = new AVStatusQuery();
    query.setSelfQuery(true);
    query.whereEqualTo("source", owner);
    query.setExternalQueryPath("statuses");
    return query;
  }

  /**
   * 返回一个AVStatusQuery对象，用来查询用户收件箱内容
   * 
   * @param owner
   * @param inBoxType
   * @return
   */
  public static AVStatusQuery inboxQuery(AVUser owner, String inBoxType) {
    AVStatusQuery query = new AVStatusQuery();
    query.setInboxType(inBoxType);
    query.setOwner(owner);
    query.setExternalQueryPath("subscribe/statuses");
    return query;
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void add(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void addAll(String key, Collection<?> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void addAllUnique(String key, Collection<?> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void addUnique(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public boolean containsKey(String k) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete() throws AVException {
    AVStatus.deleteStatusWithId(true, this.getObjectId(), new DeleteCallback() {

      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  public AVObject toObject() {
    return AVObject.createWithoutData("_Status", this.objectId);
  }

  @Override
  public boolean equals(Object obj) {
    if (AVUtils.isBlankString(this.objectId)) {
      return false;
    }
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AVStatus other = (AVStatus) obj;
    if (objectId == null) {
      if (other.objectId != null)
        return false;
    } else if (!objectId.equals(other.objectId))
      return false;
    return true;
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVObject fetch() {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVObject fetch(String includedKeys) {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVObject fetchIfNeeded() {
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVObject fetchIfNeeded(String includedKeys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return "AVStatus [" + ", objectId=" + objectId + ", createdAt=" + createdAt + ", data="
        + dataMap + "]";
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public boolean isFetchWhenSave() {
    // 不知道为什么在JSONField里面ignore不掉
    return false;
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void setFetchWhenSave(boolean fetchWhenSave) {
    // TODO Auto-generated method stub
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public String getUuid() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public Object get(String key) {
    return dataMap.get(key);
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVACL getACL() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public boolean getBoolean(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public byte[] getBytes(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public Date getDate(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public double getDouble(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public int getInt(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public JSONArray getJSONArray(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public JSONObject getJSONObject(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public List getList(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public long getLong(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public <V> Map<String, V> getMap(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public Number getNumber(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public <T extends AVFile> T getAVFile(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public AVGeoPoint getAVGeoPoint(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends AVObject> T getAVObject(String key) {
    return (T) get(key);
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public <T extends AVUser> T getAVUser(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public <T extends AVObject> AVRelation<T> getRelation(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public String getString(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public Date getUpdatedAt() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public boolean has(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public boolean hasSameId(AVObject other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void increment(String key) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void increment(String key, Number amount) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public Set<String> keySet() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void refresh() throws AVException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void refresh(String includeKeys) throws AVException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void removeAll(String key, Collection<?> values) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void save() throws AVException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * 此方法并没有实现，调用会抛出UnsupportedOperationException
   */
  @Deprecated
  @Override
  public void setACL(AVACL acl) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }
}
