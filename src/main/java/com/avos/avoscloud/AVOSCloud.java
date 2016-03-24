package com.avos.avoscloud;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.avos.avoscloud.AVServerDateCallback;

/**
 * The AVOSCloud class contains static functions that handle global configuration for the AVOSCloud
 * library.
 */
public class AVOSCloud {
  public static String applicationId;
  public static String clientKey;

  public static final int LOG_LEVEL_VERBOSE = 1 << 1;
  public static final int LOG_LEVEL_DEBUG = 1 << 2;
  public static final int LOG_LEVEL_INFO = 1 << 3;
  public static final int LOG_LEVEL_WARNING = 1 << 4;
  public static final int LOG_LEVEL_ERROR = 1 << 5;
  public static final int LOG_LEVEL_NONE = ~0;
  static final String AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY = "AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY";
  static final String AV_CLOUD_CACHE_EXPIRE_DATE_KEY = "AV_CLOUD_CACHE_EXPIRE_DATE_KEY";
  static final Integer AV_CLOUD_CACHE_DEFAULT_EXPIRE_DATE = 30;
  static final String AV_CLOUD_CACHE_EXPIRE_KEY_ZONE = "AV_CLOUD_CACHE_EXPIRE_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY_ZONE = "AV_CLOUD_API_VERSION_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY = "AV_CLOUD_API_VERSION";

  private static int logLevel = LOG_LEVEL_NONE;

  private static boolean internalDebugLog = false;
  private static boolean userInternalDebugLog = false;

  public static final int DEFAULT_NETWORK_TIMEOUT = 15000;

  static final int DEFAULT_THREAD_POOL_SIZE = 10;

  private static int networkTimeoutInMills = DEFAULT_NETWORK_TIMEOUT;

  private static int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

  /**
   * Set network timeout in milliseconds.default is 10 seconds.
   * 
   * @param timeoutInMills
   */
  public static void setNetworkTimeout(int timeoutInMills) {
    networkTimeoutInMills = timeoutInMills;
  }

  /**
   * Returns the network timeout in milliseconds.It's 15 seconds by default.
   * 
   * @return
   */
  public static int getNetworkTimeout() {
    return networkTimeoutInMills;
  }

  public static int getThreadPoolSize() {
    return threadPoolSize;
  }

  public static void setThreadPoolSize(int size) {
    threadPoolSize = size;
  }

  public enum StorageType {
    StorageTypeQiniu, StorageTypeAV, StorageTypeS3;
  }

  static {
    JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    ParserConfig.getGlobalInstance().putDeserializer(AVObject.class, AVObjectDeserializer.instance);
    ParserConfig.getGlobalInstance().putDeserializer(AVUser.class, AVObjectDeserializer.instance);

    SerializeConfig.getGlobalInstance().put(AVObject.class, AVObjectSerializer.instance);
    SerializeConfig.getGlobalInstance().put(AVUser.class, AVObjectSerializer.instance);
  }

  private static StorageType storageType = StorageType.StorageTypeQiniu;

  private AVOSCloud() {}

  /**
   * <p>
   * Authenticates this client as belonging to your application. This must be called before your
   * application can use the AVOSCloud library. The recommended way is to put a call to
   * AVOSCloud.initialize in each of your onCreate methods. An example:
   * </p>
   * 
   * <pre>
   *         import android.app.Application;
   *         import com.avos.avoscloud.AVOSCloud;
   * 
   *         public class MyApplication extends Application {
   *             public void onCreate() {
   *                 AVOSCloud.initialize(this, "your application id", "your client key");
   *             }
   *         }
   * @param context The active Context for your application.
   * @param applicationId  The application id provided in the AVOSCloud dashboard.
   * @param clientKey The client key provided in the AVOSCloud dashboard.
   */
  public static void initialize(String applicationId, String clientKey) {
    AVOSCloud.applicationId = applicationId;
    AVOSCloud.clientKey = clientKey;
  }

  public static void useAVCloudUS() {
    PaasClient.useAVCloudUS();
  }

  public static void useAVCloudCN() {
    PaasClient.useAVCloudCN();
  }

  static void showInternalDebugLog(boolean show) {
    internalDebugLog = show;
  }

  public static boolean showInternalDebugLog() {
    return internalDebugLog;
  }

  public static void setDebugLogEnabled(boolean enable) {
    userInternalDebugLog = enable;
  }

  public static boolean isDebugLogEnabled() {
    return userInternalDebugLog || internalDebugLog;
  }

  public static StorageType getStorageType() {
    return storageType;
  }

  public static void setStorageType(StorageType storageType) {
    AVOSCloud.storageType = storageType;
  }

  public static void setBaseUrl(final String baseUrl) {
    PaasClient.storageInstance().setBaseUrl(baseUrl);
  }

  /**
   * 请求发送短信验证码
   * 
   * 
   * 短信示范为: 您正在{name}中进行{op}，您的验证码是:{Code}，请输入完整验证，有效期为:{ttl}
   * 
   * 
   * @param phone　目标手机号码(必选)
   * @param name　应用名,值为null 则默认是您的应用名
   * @param op　　验证码的目标操作，值为null,则默认为“短信验证”
   * @param ttl　验证码过期时间,单位分钟。如果是0，则默认为10分钟
   * 
   */
  public static void requestSMSCode(String phone, String name, String op, int ttl)
      throws AVException {

    requestSMSCodeInBackground(phone, null, getSMSCodeEnv(name, op, ttl), true,
        new RequestMobileCodeCallback() {
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

  private static Map<String, Object> getSMSCodeEnv(String name, String op, int ttl) {
    Map<String, Object> map = new HashMap<String, Object>();
    if (!AVUtils.isBlankString(op)) {
      map.put("op", op);
    }
    if (!AVUtils.isBlankString(name)) {
      map.put("name", name);
    }
    if (ttl > 0) {
      map.put("ttl", ttl);
    }
    return map;
  }

  private static Map<String, Object> getVoiceCodeEnv(String countryCode) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("smsType", "voice");
    if (!AVUtils.isBlankString(countryCode)) {
      map.put("IDD", countryCode);
    }
    return map;
  }

  /**
   * 通过短信模板来发送短信验证码
   * 
   * 
   * @param phone 目标手机号码(必选)
   * @param templateName 短信模板名称
   * @param env 需要注入的变量env
   * @throws AVException
   */
  public static void requestSMSCode(String phone, String templateName, Map<String, Object> env)
      throws AVException {
    requestSMSCodeInBackground(phone, templateName, env, true, new RequestMobileCodeCallback() {
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

  private static void requestSMSCodeInBackground(String phone, String templateName,
      Map<String, Object> env, boolean sync, RequestMobileCodeCallback callback) {
    final RequestMobileCodeCallback internalCallback = callback;

    if (AVUtils.isBlankString(phone) || !AVUtils.checkMobilePhoneNumber(phone)) {
      callback.internalDone(new AVException(AVException.INVALID_PHONE_NUMBER,
          "Invalid Phone Number"));
    }

    if (env == null) {
      env = new HashMap<String, Object>();
    }
    env.put("mobilePhoneNumber", phone);
    if (!AVUtils.isBlankString(templateName)) {
      env.put("template", templateName);
    }
    String object = AVUtils.jsonStringFromMapWithNull(env);
    PaasClient.storageInstance().postObject("requestSmsCode", object, sync, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        }, null, null);
  }

  /**
   * 请求发送短信验证码
   * 
   * 
   * 短信示范为: 您正在{应用名称}中进行短信验证，您的验证码是:{Code}，请输入完整验证，有效期为:10分钟
   * 
   * 
   * @param phone　目标手机号码
   * 
   */
  public static void requestSMSCode(String phone) throws AVException {
    requestSMSCode(phone, null, null, 0);
  }

  /**
   * 请求发送语音验证码，验证码会以电话形式打给目标手机
   *
   * @param phoneNumber 目标手机号
   * @throws AVException
   */
  public static void requestVoiceCode(String phoneNumber) throws AVException {
    requestVoiceCode(phoneNumber, null);
  }

  /**
   * 请求发送语音验证码，验证码会以电话形式打给目标手机
   * 
   * @param phoneNumber 目标手机号
   * @param idd 电话的国家区号
   * @throws AVException
   */

  private static void requestVoiceCode(String phoneNumber, String idd) throws AVException {
    requestSMSCodeInBackground(phoneNumber, null, getVoiceCodeEnv(idd), true,
        new RequestMobileCodeCallback() {
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


  /**
   * 验证验证码
   * 
   * @param code 验证码
   * @param mobilePhoneNumber 手机号码
   * @throws AVException
   */
  public static void verifySMSCode(String code, String mobilePhoneNumber) throws AVException {
    verifySMSCodeInBackground(code, mobilePhoneNumber, true, new AVMobilePhoneVerifyCallback() {
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

  /**
   * 验证验证码
   *
   * @param code 验证码
   * @param mobilePhoneNumber 手机号码
   * @throws AVException
   */
  public static void verifyCode(String code, String mobilePhoneNumber) throws AVException {
    verifySMSCode(code, mobilePhoneNumber);
  }

  private static void verifySMSCodeInBackground(String code, String mobilePhoneNumber,
      boolean sync, AVMobilePhoneVerifyCallback callback) {
    final AVMobilePhoneVerifyCallback internalCallback = callback;

    if (AVUtils.isBlankString(code) || !AVUtils.checkMobileVerifyCode(code)) {
      callback
          .internalDone(new AVException(AVException.INVALID_PHONE_NUMBER, "Invalid Verify Code"));
    }
    String endpointer = String.format("verifySmsCode/%s", code);
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("mobilePhoneNumber", mobilePhoneNumber);
    PaasClient.storageInstance().postObject(endpointer, AVUtils.restfulServerData(params), sync,
        false, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        }, null, null);
  }

  /**
   * 获取服务器端当前时间
   * 
   * @return
   * @throws AVException
   */
  public static Date getServerDate() throws AVException {
    final Date[] results = {null};
    getServerDateInBackground(true, new AVServerDateCallback() {
      @Override
      public void done(Date serverDate, AVException e) {
        if (e == null) {
          results[0] = serverDate;
        } else {
          AVExceptionHolder.add(e);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return results[0];
  }

  private static void getServerDateInBackground(boolean sync, final AVServerDateCallback callback) {
    PaasClient.storageInstance().getObject("date", null, sync, null, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        try {
          Date date = AVUtils.dateFromMap(JSON.parseObject(content, Map.class));
          if (callback != null) {
            callback.internalDone(date, null);
          }
        } catch (Exception ex) {
          if (callback != null) {
            callback.internalDone(null, AVErrorUtils.createException(ex, null));
          }
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
}
