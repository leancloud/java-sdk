package com.avos.avoscloud;

import java.util.Date;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.avos.avoscloud.callback.AVServerDateCallback;
import com.avos.avoscloud.internal.InternalConfigurationController;
import com.avos.avoscloud.internal.InternalDate;
import com.avos.avoscloud.internal.InternalSMS;
import com.avos.avoscloud.internal.MasterKeyConfiguration;
import com.avos.avoscloud.internal.impl.EngineRequestSign;
import com.avos.avoscloud.internal.impl.JavaAppConfiguration;
import com.avos.avoscloud.internal.impl.Log4j2Implementation;
import com.avos.avoscloud.internal.impl.SimplePersistence;

/**
 * The AVOSCloud class contains static functions that handle global configuration for the AVOSCloud
 * library.
 */
public class AVOSCloud {

  static final String AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY = "AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY";
  static final String AV_CLOUD_CACHE_EXPIRE_DATE_KEY = "AV_CLOUD_CACHE_EXPIRE_DATE_KEY";
  static final Integer AV_CLOUD_CACHE_DEFAULT_EXPIRE_DATE = 30;
  static final String AV_CLOUD_CACHE_EXPIRE_KEY_ZONE = "AV_CLOUD_CACHE_EXPIRE_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY_ZONE = "AV_CLOUD_API_VERSION_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY = "AV_CLOUD_API_VERSION";


  /**
   * Set network timeout in milliseconds.default is 10 seconds.
   * 
   * @param timeoutInMills
   */
  public static void setNetworkTimeout(int timeoutInMills) {
    InternalConfigurationController.globalInstance().getClientConfiguration()
        .setNetworkTimeoutInMills(timeoutInMills);
  }

  /**
   * Returns the network timeout in milliseconds.It's 15 seconds by default.
   * 
   * @return
   */
  public static int getNetworkTimeout() {
    return InternalConfigurationController.globalInstance().getClientConfiguration()
        .getNetworkTimeoutInMills();
  }

  static {
    JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    ParserConfig.getGlobalInstance().putDeserializer(AVObject.class, AVObjectDeserializer.instance);
    ParserConfig.getGlobalInstance().putDeserializer(AVUser.class, AVObjectDeserializer.instance);

    SerializeConfig.getGlobalInstance().put(AVObject.class, AVObjectSerializer.instance);
    SerializeConfig.getGlobalInstance().put(AVUser.class, AVObjectSerializer.instance);
    InternalConfigurationController.globalInstance().setInternalLogger(
        Log4j2Implementation.instance());

    InternalConfigurationController.globalInstance().setInternalPersistence(
        SimplePersistence.instance());
  }

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
   * @param masterKey The master key provided in the AVOSCloud dashboard.
   */

  public static void initialize(String applicationId, String clientKey, String masterKey) {
    if (!(InternalConfigurationController.globalInstance().getAppConfiguration() instanceof JavaAppConfiguration)) {
      InternalConfigurationController.globalInstance().setAppConfiguration(
          JavaAppConfiguration.instance());
    }
    InternalConfigurationController.globalInstance().setInternalRequestSign(
        EngineRequestSign.instance());

    InternalConfigurationController.globalInstance().getAppConfiguration()
        .setApplicationId(applicationId);
    InternalConfigurationController.globalInstance().getAppConfiguration().setClientKey(clientKey);
    if (InternalConfigurationController.globalInstance().getAppConfiguration() instanceof MasterKeyConfiguration) {
      ((MasterKeyConfiguration) InternalConfigurationController.globalInstance()
          .getAppConfiguration()).setMasterKey(masterKey);
    }
  }

  public static void useAVCloudUS() {
    PaasClient.useAVCloudUS();
  }

  public static void useAVCloudCN() {
    PaasClient.useAVCloudCN();
  }

  public static boolean showInternalDebugLog() {
    return InternalConfigurationController.globalInstance().getInternalLogger()
        .showInternalDebugLog();
  }

  public static void setDebugLogEnabled(boolean enable) {
    InternalConfigurationController.globalInstance().getInternalLogger().setDebugEnabled(enable);
  }

  public static boolean isDebugLogEnabled() {
    return InternalConfigurationController.globalInstance().getInternalLogger().isDebugEnabled()
        || InternalConfigurationController.globalInstance().getInternalLogger()
            .showInternalDebugLog();
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
    InternalSMS.requestSMSCode(phone, name, op, ttl);
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
    InternalSMS.requestSMSCode(phone, templateName, env);
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
    InternalSMS.requestSMSCode(phone, null, null, 0);
  }

  /**
   * 请求发送语音验证码，验证码会以电话形式打给目标手机
   *
   * @param phoneNumber 目标手机号
   * @throws AVException
   */
  public static void requestVoiceCode(String phoneNumber) throws AVException {
    InternalSMS.requestVoiceCode(phoneNumber, null);
  }


  /**
   * 验证验证码
   * 
   * @param code 验证码
   * @param mobilePhoneNumber 手机号码
   * @throws AVException
   */
  public static void verifySMSCode(String code, String mobilePhoneNumber) throws AVException {
    InternalSMS.verifySMSCode(code, mobilePhoneNumber);
  }

  /**
   * 验证验证码
   *
   * @param code 验证码
   * @param mobilePhoneNumber 手机号码
   * @throws AVException
   */
  public static void verifyCode(String code, String mobilePhoneNumber) throws AVException {
    InternalSMS.verifySMSCode(code, mobilePhoneNumber);
  }

  /**
   * 获取服务器端当前时间
   * 
   * @return
   * @throws AVException
   */
  public static Date getServerDate() throws AVException {
    return InternalDate.getServerDate();
  }

  /**
   * 获取服务器端当前时间
   * 
   * @param callback
   */
  public static void getServerDateInBackground(AVServerDateCallback callback) {
    InternalDate.getServerDateInBackground(callback);
  }

  public static void setShouldUseMasterKey(boolean should) {
    EngineRequestSign.instance().setUserMasterKey(should);
  }
}
