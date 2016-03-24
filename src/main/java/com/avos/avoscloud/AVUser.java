package com.avos.avoscloud;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONType;
import java.util.*;

@JSONType(ignores = {"query", "password","followersAndFollowees"}, asm = false)
public class AVUser extends AVObject {
  transient private static boolean enableAutomatic = false;
  private String sessionToken;
  transient private boolean isNew;
  private String username;
  transient private String password;
  private String mobilePhoneNumber;
  private String email;
  private transient String facebookToken;
  private transient String twitterToken;
  private transient String sinaWeiboToken;
  private transient String qqWeiboToken;
  private transient boolean needTransferFromAnonymousUser;
  private boolean anonymous;
  public static final String LOG_TAG = AVUser.class.getSimpleName();

  public static final String FOLLOWER_TAG = "follower";
  public static final String FOLLOWEE_TAG = "followee";
  public static final String SESSION_TOKEN_KEY = "sessionToken";
  private static Class<? extends AVUser> subClazz;

  // getter/setter for fastjson
  public String getFacebookToken() {
    return facebookToken;
  }

  void setFacebookToken(String facebookToken) {
    this.facebookToken = facebookToken;
  }

  public String getTwitterToken() {
    return twitterToken;
  }

  void setTwitterToken(String twitterToken) {
    this.twitterToken = twitterToken;
  }

  public String getQqWeiboToken() {
    return qqWeiboToken;
  }

  void setQqWeiboToken(String qqWeiboToken) {
    this.qqWeiboToken = qqWeiboToken;
  }

  String getPassword() {
    return password;
  }

  // end of getter/setter

  static void setEnableAutomatic(boolean enableAutomatic) {
    AVUser.enableAutomatic = enableAutomatic;
  }

  void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  /**
   * Constructs a new AVUser with no data in it. A AVUser constructed in this way will not have an
   * objectId and will not persist to the database until AVUser.signUp() is called.
   */
  public AVUser() {
    super(userClassName());
  }

  @Override
  protected void rebuildInstanceData() {
    super.rebuildInstanceData();
    this.sessionToken = (String) get(SESSION_TOKEN_KEY);
    this.username = (String) get("username");
    this.processAuthData(null);
    this.email = (String) get("email");
    this.mobilePhoneNumber = (String) get("mobilePhoneNumber");
  }

  /**
   * Enables automatic creation of anonymous users. After calling this method,
   * AVUser.getCurrentUser() will always have a value. The user will only be created on the server
   * once the user has been saved, or once an object with a relation to that user or an ACL that
   * refers to the user has been saved. Note: saveEventually will not work if an item being saved
   * has a relation to an automatic user that has never been saved.
   */
  public static void enableAutomaticUser() {
    enableAutomatic = true;
  }

  public static boolean isEnableAutomatic() {
    return enableAutomatic;
  }

  public static void disableAutomaticUser() {
    enableAutomatic = false;
  }

  protected static synchronized void changeCurrentUser(AVUser newUser, boolean save) {
    // clean password for security reason
    PaasClient.storageInstance().setCurrentUser(newUser);
  }

  /**
   * This retrieves the currently logged in AVUser with a valid session, either from memory or disk
   * if necessary.
   * 
   * @return The currently logged in AVUser
   */
  public static AVUser getCurrentUser() {
    return getCurrentUser(AVUser.class);
  }

  /**
   * This retrieves the currently logged in AVUser with a valid session, either from memory or disk
   * if necessary.
   * 
   * @param userClass subclass.
   * @return The currently logged in AVUser subclass instance.
   */
  @SuppressWarnings("unchecked")
  public static <T extends AVUser> T getCurrentUser(Class<T> userClass) {
    T user = (T) PaasClient.storageInstance().getCurrentUser();
    if (user != null) {
      if (!userClass.isAssignableFrom(user.getClass())) {
        user = AVUser.cast(user, userClass);
      }
    }
    if (enableAutomatic && user == null) {
      user = newAVUser(userClass, null);
      AVUser.changeCurrentUser(user, false);;
    }
    return user;
  }

  static String userClassName() {
    return AVPowerfulUtils.getAVClassName(AVUser.class.getSimpleName());
  }

  void setNewFlag(boolean isNew) {
    this.isNew = isNew;
  }

  /**
   * Retrieves the email address.
   */
  public String getEmail() {
    return this.email;
  }

  /**
   * Constructs a query for AVUsers subclasses.
   */
  public static <T extends AVUser> AVQuery<T> getUserQuery(Class<T> clazz) {
    AVQuery<T> query = new AVQuery<T>(userClassName(), clazz);
    return query;
  }

  /**
   * Constructs a query for AVUsers.
   */
  public static AVQuery<AVUser> getQuery() {
    return getQuery(AVUser.class);
  }

  public String getSessionToken() {
    return sessionToken;
  }

  void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  /**
   * Retrieves the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Whether the AVUser has been authenticated on this device. This will be true if the AVUser was
   * obtained via a logIn or signUp method. Only an authenticated AVUser can be saved (with altered
   * attributes) and deleted.
   */
  public boolean isAuthenticated() {
    return (!AVUtils.isBlankString(sessionToken) || !AVUtils.isBlankString(sinaWeiboToken) || !AVUtils
        .isBlankString(qqWeiboToken));
  }

  public boolean isAnonymous() {
    return this.anonymous;
  }

  protected void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  /**
   * <p>
   * Indicates whether this AVUser was created during this session through a call to AVUser.signUp()
   * or by logging in with a linked service such as Facebook.
   * </p>
   */
  public boolean isNew() {
    return isNew;
  }

  /**
   * @see #logIn(String, String, Class)
   * @param username
   * @param password
   * @return
   */
  public static AVUser logIn(String username, String password) throws AVException {
    return logIn(username, password, AVUser.class);
  }

  /**
   * <p>
   * Logs in a user with a username and password. On success, this saves the session to disk, so you
   * can retrieve the currently logged in user using AVUser.getCurrentUser()
   * </p>
   * <p>
   * 
   * @param username The username to log in with.
   * @param password The password to log in with.
   * @param clazz The AVUser itself or subclass.
   * @return The user if the login was successful.
   */
  public static <T extends AVUser> T logIn(String username, String password, Class<T> clazz)
      throws AVException {
    final AVUser[] list = {null};

    logInInBackground(username, password, true, new LogInCallback<T>() {

      @Override
      public void done(T user, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        } else {
          list[0] = user;
        }
      }
    }, clazz);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return (T) list[0];
  }

  static private String logInPath() {
    return "login";
  }

  static private Map<String, String> createUserMap(String username, String password, String email) {
    Map<String, String> map = new HashMap<String, String>();
    map.put("username", username);
    if (AVUtils.isBlankString(username)) {
      throw new IllegalArgumentException("Blank username.");
    }
    if (!AVUtils.isBlankString(password)) {
      map.put("password", password);
    }
    if (!AVUtils.isBlankString(email)) {
      map.put("email", email);
    }
    return map;
  }

  static private Map<String, String> createUserMap(String username, String password, String email,
      String phoneNumber, String smsCode) {
    Map<String, String> map = new HashMap<String, String>();

    if (AVUtils.isBlankString(username) && AVUtils.isBlankString(phoneNumber)) {
      throw new IllegalArgumentException("Blank username and blank mobile phone number");
    }
    if (!AVUtils.isBlankString(username)) {
      map.put("username", username);
    }
    if (!AVUtils.isBlankString(password)) {
      map.put("password", password);
    }
    if (!AVUtils.isBlankString(email)) {
      map.put("email", email);
    }
    if (!AVUtils.isBlankString(phoneNumber)) {
      map.put("mobilePhoneNumber", phoneNumber);
    }
    if (!AVUtils.isBlankString(smsCode)) {
      map.put("smsCode", smsCode);
    }
    return map;
  }

  private static <T extends AVUser> void logInInBackground(String username, String password,
      boolean sync, LogInCallback<T> callback, Class<T> clazz) {
    Map<String, String> map = createUserMap(username, password, "");
    final LogInCallback<T> internalCallback = callback;
    final T user = newAVUser(clazz, callback);
    if (user == null) {
      return;
    }
    user.put("username", username, false);
    PaasClient.storageInstance().postObject(logInPath(), JSON.toJSONString(map), sync, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVException error = e;
            T resultUser = user;
            if (!AVUtils.isBlankContent(content)) {
              AVUtils.copyPropertiesFromJsonStringToAVObject(content, user);
              user.processAuthData(null);
              AVUser.changeCurrentUser(user, true);
            } else {
              resultUser = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "User is not found.");
            }
            if (internalCallback != null) {
              internalCallback.internalDone(resultUser, error);
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

  public static AVUser loginByMobilePhoneNumber(String phone, String password) throws AVException {
    return loginByMobilePhoneNumber(phone, password, AVUser.class);
  }

  public static <T extends AVUser> T loginByMobilePhoneNumber(String phone, String password,
      Class<T> clazz) throws AVException {

    final AVUser[] list = {null};

    loginByMobilePhoneNumberInBackground(phone, password, true, new LogInCallback<T>() {

      @Override
      public void done(T user, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        } else {
          list[0] = user;
        }
      }
    }, clazz);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return (T) list[0];
  }

  private static <T extends AVUser> void loginByMobilePhoneNumberInBackground(String phone,
      String password, boolean sync, LogInCallback<T> callback, Class<T> clazz) {
    Map<String, String> map = createUserMap(null, password, null, phone, null);
    final LogInCallback<T> internalCallback = callback;
    final T user = newAVUser(clazz, callback);
    if (user == null) {
      return;
    }
    user.setMobilePhoneNumber(phone);
    PaasClient.storageInstance().postObject(logInPath(), JSON.toJSONString(map), sync, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVException error = e;
            T resultUser = user;
            if (!AVUtils.isBlankContent(content)) {
              AVUtils.copyPropertiesFromJsonStringToAVObject(content, user);
              AVUser.changeCurrentUser(user, true);
            } else {
              resultUser = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "User is not found.");
            }
            if (internalCallback != null) {
              internalCallback.internalDone(resultUser, error);
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
   * 通过短信验证码和手机号码来登录用户
   * 
   * 请不要在UI线程内调用本方法
   * 
   * @param phone
   * @param smsCode
   * @return
   * @throws AVException
   */
  public static AVUser loginBySMSCode(String phone, String smsCode) throws AVException {
    return loginBySMSCode(phone, smsCode, AVUser.class);
  }

  /**
   * 通过短信验证码和手机号码来登录用户
   * 
   * 请不要在UI线程内调用本方法
   * 
   * @param phone
   * @param smsCode
   * @param clazz AVUser的子类对象
   * @return
   * @throws AVException
   */
  public static <T extends AVUser> T loginBySMSCode(String phone, String smsCode, Class<T> clazz)
      throws AVException {
    final AVUser[] list = {null};
    loginBySMSCodeInBackground(phone, smsCode, true, new LogInCallback<T>() {

      @Override
      public void done(T user, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        } else {
          list[0] = user;
        }
      }
    }, clazz);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return (T) list[0];
  }

  private static <T extends AVUser> void loginBySMSCodeInBackground(String phone, String smsCode,
      boolean sync, LogInCallback<T> callback, Class<T> clazz) {
    Map<String, String> map = createUserMap(null, null, "", phone, smsCode);
    final LogInCallback<T> internalCallback = callback;
    final T user = newAVUser(clazz, callback);
    if (user == null) {
      return;
    }
    user.setMobilePhoneNumber(phone);
    PaasClient.storageInstance().postObject(logInPath(), JSON.toJSONString(map), sync, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVException error = e;
            T resultUser = user;
            if (!AVUtils.isBlankContent(content)) {
              AVUtils.copyPropertiesFromJsonStringToAVObject(content, user);
              AVUser.changeCurrentUser(user, true);
            } else {
              resultUser = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "User is not found.");
            }
            if (internalCallback != null) {
              internalCallback.internalDone(resultUser, error);
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
   * 直接通过手机号码和验证码来创建或者登录用户。 如果手机号码已经存在则为登录，否则创建新用户
   * 
   * 请不要在UI线程中间调用此方法
   * 
   * @param mobilePhoneNumber
   * @param smsCode
   * @return
   * @throws AVException
   * @since 2.6.10
   * @see AVOSCloud#requestSMSCode(String)
   */
  public static AVUser signUpOrLoginByMobilePhone(String mobilePhoneNumber, String smsCode)
      throws AVException {
    return signUpOrLoginByMobilePhone(mobilePhoneNumber, smsCode, AVUser.class);
  }

  /**
   * 直接通过手机号码和验证码来创建或者登录用户。 如果手机号码已经存在则为登录，否则创建新用户
   * 
   * 请不要在UI线程中间调用此方法
   * 
   * @param mobilePhoneNumber
   * @param smsCode
   * @param clazz
   * @return
   * @throws AVException
   * @since 2.6.10
   * @see AVOSCloud#requestSMSCode(String)
   */
  public static <T extends AVUser> T signUpOrLoginByMobilePhone(String mobilePhoneNumber,
      String smsCode, Class<T> clazz) throws AVException {
    final AVUser[] list = {null};
    signUpOrLoginByMobilePhoneInBackground(mobilePhoneNumber, smsCode, true, clazz,
        new LogInCallback<T>() {

          @Override
          public void done(T user, AVException e) {
            if (e != null) {
              AVExceptionHolder.add(e);
            } else {
              list[0] = user;
            }
          }
        });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return (T) list[0];
  }

  private static <T extends AVUser> void signUpOrLoginByMobilePhoneInBackground(
      String mobilePhoneNumber, String smsCode, boolean sync, Class<T> clazz,
      LogInCallback<T> callback) {
    if (AVUtils.isBlankString(smsCode)) {
      if (callback != null) {
        callback.internalDone(null, new AVException(AVException.OTHER_CAUSE,
            "SMS Code can't be empty"));
      } else {
        LogUtil.avlog.e("SMS Code can't be empty");
      }
      return;
    }
    Map<String, String> map = createUserMap(null, null, "", mobilePhoneNumber, smsCode);
    final LogInCallback<T> internalCallback = callback;
    final T user = newAVUser(clazz, callback);
    if (user == null) {
      return;
    }
    user.setMobilePhoneNumber(mobilePhoneNumber);
    PaasClient.storageInstance().postObject("usersByMobilePhone", JSON.toJSONString(map), sync,
        false, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVException error = e;
            T resultUser = user;
            if (!AVUtils.isBlankContent(content)) {
              AVUtils.copyPropertiesFromJsonStringToAVObject(content, user);
              AVUser.changeCurrentUser(user, true);
            } else {
              resultUser = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "User is not found.");
            }
            if (internalCallback != null) {
              internalCallback.internalDone(resultUser, error);
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

  public static <T extends AVUser> T newAVUser(Class<T> clazz, LogInCallback<T> cb) {
    try {
      final T user = clazz.newInstance();
      return user;
    } catch (Exception e) {
      if (cb != null) {
        cb.internalDone(null, AVErrorUtils.createException(e, null));
      } else {
        throw new AVRuntimeException("Create user instance failed.", e);
      }
    }
    return null;
  }

  protected static <T extends AVUser> T newAVUser() {
    return (T) newAVUser(subClazz == null ? AVUser.class : subClazz, null);
  }

  /**
   * Logs out the currently logged in user session. This will remove the session from disk, log out
   * of linked services, and future calls to AVUser.getCurrentUser() will return null.
   */
  public static void logOut() {
    AVUser.changeCurrentUser(null, true);
    PaasClient.storageInstance().setDefaultACL(null);
  }

  /**
   * Add a key-value pair to this object. It is recommended to name keys in
   * partialCamelCaseLikeThis.
   * 
   * @param key Keys must be alphanumerical plus underscore, and start with a letter.
   * @param value Values may be numerical, String, JSONObject, JSONArray, JSONObject.NULL, or other
   *        AVObjects.
   */
  @Override
  public void put(String key, Object value) {
    super.put(key, value);
  }

  /**
   * Removes a key from this object's data if it exists.
   * 
   * @param key The key to remove.
   */
  @Override
  public void remove(String key) {
    super.remove(key);
  }

  /**
   * <p>
   * Requests a password reset email to be sent to the specified email address associated with the
   * user account. This email allows the user to securely reset their password on the AVOSCloud
   * site.
   * </p>
   * 
   * @param email The email address associated with the user that forgot their password.
   */
  public static void requestPasswordReset(String email) {
    requestPasswordResetInBackground(email, true, null);
  }

  private static void requestPasswordResetInBackground(String email, boolean sync,
      RequestPasswordResetCallback callback) {
    final RequestPasswordResetCallback internalCallback = callback;
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("email", email);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestPasswordReset", object, sync, false,
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
   * 同步方法调用修改用户当前的密码
   * 
   * 您需要保证用户有效的登录状态
   */
  public void updatePassword(String oldPassword, String newPassword) throws AVException {
    updatePasswordInBackground(oldPassword, newPassword, new UpdatePasswordCallback() {

      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }
    }, true);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  private void updatePasswordInBackground(String oldPassword, String newPassword,
      final UpdatePasswordCallback callback, boolean sync) {
    if (!this.isAuthenticated() || AVUtils.isBlankString(getObjectId())) {
      callback.internalDone(AVErrorUtils.sessionMissingException());
    } else {
      String relativePath = String.format("users/%s/updatePassword", this.getObjectId());
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("old_password", oldPassword);
      params.put("new_password", newPassword);
      String paramsString = AVUtils.restfulServerData(params);
      PaasClient.storageInstance().putObject(relativePath, paramsString, sync, headerMap(),
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              if (null == e && !AVUtils.isBlankString(content)) {
                sessionToken = AVUtils.getJSONValue(content, SESSION_TOKEN_KEY);
              }
              callback.internalDone(e);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              callback.internalDone(AVErrorUtils.createException(error, content));
            }
          }, getObjectId(), getObjectId());
    }
  }

  /**
   * 申请通过短信重置用户密码
   * 
   * 
   */

  public static void requestPasswordResetBySmsCode(String mobilePhoneNumber) throws AVException {
    requestPasswordResetBySmsCodeInBackground(mobilePhoneNumber, true,
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


  protected static void requestPasswordResetBySmsCodeInBackground(String mobilePhoneNumber,
      boolean sync, RequestMobileCodeCallback callback) {
    final RequestMobileCodeCallback internalCallback = callback;

    if (AVUtils.isBlankString(mobilePhoneNumber)
        || !AVUtils.checkMobilePhoneNumber(mobilePhoneNumber)) {
      callback.internalDone(new AVException(AVException.INVALID_PHONE_NUMBER,
          "Invalid Phone Number"));
      return;
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("mobilePhoneNumber", mobilePhoneNumber);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestPasswordResetBySmsCode", object, sync, false,
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
   * 通过短信验证码更新用户密码
   * 
   * 
   * @param smsCode
   * @param newPassword
   * @throws AVException
   */
  public static void resetPasswordBySmsCode(String smsCode, String newPassword) throws AVException {
    resetPasswordBySmsCodeInBackground(smsCode, newPassword, true, new UpdatePasswordCallback() {
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

  protected static void resetPasswordBySmsCodeInBackground(String smsCode, String newPassword,
      boolean sync, UpdatePasswordCallback callback) {
    final UpdatePasswordCallback internalCallback = callback;

    if (AVUtils.isBlankString(smsCode) || !AVUtils.checkMobileVerifyCode(smsCode)) {
      callback
          .internalDone(new AVException(AVException.INVALID_PHONE_NUMBER, "Invalid Verify Code"));
      return;
    }

    String endpointer = String.format("resetPasswordBySmsCode/%s", smsCode);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("password", newPassword);
    PaasClient.storageInstance().putObject(endpointer, AVUtils.restfulServerData(params), sync,
        null, new GenericObjectCallback() {
          @Override
          public void onFailure(Throwable error, String content) {
            if (internalCallback != null) {
              internalCallback.internalDone(new AVException(content, error));
            }
          }

          @Override
          public void onSuccess(String content, AVException e) {
            internalCallback.internalDone(e);
          }
        }, null, null);
  }

  /**
   * <p>
   * 调用这个方法会给用户的邮箱发送一封验证邮件，让用户能够确认在AVOS Cloud网站上注册的账号邮箱
   * </p>
   * 
   * @param email The email address associated with the user that forgot their password.
   */
  public static void requestEmailVerify(String email) {
    requestEmailVerfiyInBackground(email, true, null);
  }

  private static void requestEmailVerfiyInBackground(String email, boolean sync,
      RequestEmailVerifyCallback callback) {
    final RequestEmailVerifyCallback internalCallback = callback;
    if (AVUtils.isBlankString(email) || !AVUtils.checkEmailAddress(email)) {
      callback.internalDone(new AVException(AVException.INVALID_EMAIL_ADDRESS, "Invalid Email"));
      return;
    }
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("email", email);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestEmailVerify", object, sync, false,
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
   * 调用这个方法来请求用户的手机号码验证
   * 
   * 在发送这条请求前，请保证您已经成功保存用户的手机号码，并且在控制中心打开了“验证注册用户手机号码”选项
   * 
   * 本方法请在异步方法中调用
   * 
   * @param mobilePhoneNumber
   */

  public static void requestMobilePhoneVerify(String mobilePhoneNumber) throws AVException {
    requestMobilePhoneVerifyInBackground(mobilePhoneNumber, true, new RequestMobileCodeCallback() {
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

  private static void requestMobilePhoneVerifyInBackground(String mobilePhoneNumber, boolean sync,
      RequestMobileCodeCallback callback) {
    final RequestMobileCodeCallback internalCallback = callback;

    if (AVUtils.isBlankString(mobilePhoneNumber)
        || !AVUtils.checkMobilePhoneNumber(mobilePhoneNumber)) {
      callback.internalDone(new AVException(AVException.INVALID_PHONE_NUMBER,
          "Invalid Phone Number"));
      return;
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("mobilePhoneNumber", mobilePhoneNumber);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestMobilePhoneVerify", object, sync, false,
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
   * 请求登录验证码
   * 
   * 
   * @param mobilePhoneNumber
   * @throws AVException
   */
  public static void requestLoginSmsCode(String mobilePhoneNumber) throws AVException {
    requestLoginSmsCodeInBackground(mobilePhoneNumber, true, new RequestMobileCodeCallback() {
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

  private static void requestLoginSmsCodeInBackground(String mobilePhoneNumber, boolean sync,
      RequestMobileCodeCallback callback) {
    final RequestMobileCodeCallback internalCallback = callback;

    if (AVUtils.isBlankString(mobilePhoneNumber)
        || !AVUtils.checkMobilePhoneNumber(mobilePhoneNumber)) {
      callback.internalDone(new AVException(AVException.INVALID_PHONE_NUMBER,
          "Invalid Phone Number"));
      return;
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("mobilePhoneNumber", mobilePhoneNumber);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestLoginSmsCode", object, sync, false,
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
   * 验证手机收到的验证码
   * 
   * 
   * @param verifyCode
   */
  public static void verifyMobilePhone(String verifyCode) throws AVException {
    verifyMobilePhoneInBackground(true, verifyCode, new AVMobilePhoneVerifyCallback() {
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

  private static void verifyMobilePhoneInBackground(boolean sync, String verifyCode,
      AVMobilePhoneVerifyCallback callback) {
    final AVMobilePhoneVerifyCallback internalCallback = callback;

    if (AVUtils.isBlankString(verifyCode) || !AVUtils.checkMobileVerifyCode(verifyCode)) {
      callback
          .internalDone(new AVException(AVException.INVALID_PHONE_NUMBER, "Invalid Verify Code"));
      return;
    }

    String endpointer = String.format("verifyMobilePhone/%s", verifyCode);
    PaasClient.storageInstance().postObject(endpointer, AVUtils.restfulServerData(null), sync,
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
   * Sets the email address.
   * 
   * @param email The email address to set.
   */
  public void setEmail(String email) {
    this.email = email;
    this.put("email", email);
  }

  /**
   * Sets the password.
   * 
   * @param password The password to set.
   */
  public void setPassword(String password) {
    this.password = password;
    this.put("password", password);
    markAnonymousUserTransfer();
  }

  /**
   * Sets the username. Usernames cannot be null or blank.
   * 
   * @param username The username to set.
   */
  public void setUsername(String username) {
    this.username = username;
    this.put("username", username);
    markAnonymousUserTransfer();
  }

  public String getMobilePhoneNumber() {
    return mobilePhoneNumber;
  }

  public void setMobilePhoneNumber(String mobilePhoneNumber) {
    this.mobilePhoneNumber = mobilePhoneNumber;
    this.put("mobilePhoneNumber", mobilePhoneNumber);
  }

  public boolean isMobilePhoneVerified() {
    return this.getBoolean("mobilePhoneVerified");
  }

  void setMobilePhoneVerified(boolean mobilePhoneVerified) {
    this.put("mobileVerified", mobilePhoneVerified);
  }

  private String signUpPath() {
    return "users";
  }

  private void signUp(boolean sync, final SignUpCallback callback) {
    try {
      this.save();
      if (callback != null)
        callback.internalDone(null);
    } catch (AVException e) {
      if (callback != null)
        callback.internalDone(e);
    }
  }


  /**
   * <p>
   * Signs up a new user. You should call this instead of AVObject.save() for new AVUsers. This will
   * create a new AVUser on the server, and also persist the session on disk so that you can access
   * the user using AVUser.getCurrentUser().
   * </p>
   * <p>
   * A username and password must be set before calling signUp.
   * </p>
   */
  public void signUp() throws AVException {
    signUp(true, new SignUpCallback() {

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

  void setSinaWeiboToken(String token) {
    this.sinaWeiboToken = token;
  }

  public String getSinaWeiboToken() {
    return sinaWeiboToken;
  }

  void setQQWeiboToken(String token) {
    qqWeiboToken = token;
  }

  public String getQQWeiboToken() {
    return qqWeiboToken;
  }

  @Override
  protected void onSaveSuccess() {
    super.onSaveSuccess();
    this.processAuthData(null);
    if (!AVUtils.isBlankString(sessionToken)) {
      changeCurrentUser(this, true);
    }
  }

  @Override
  protected void onDataSynchronized() {
    processAuthData(null);
    if (!AVUtils.isBlankString(sessionToken)) {
      changeCurrentUser(this, true);
    }
  }

  @Override
  protected Map<String, String> headerMap() {
    Map<String, String> map = new HashMap<String, String>();
    if (!AVUtils.isBlankString(sessionToken)) {
      map.put(PaasClient.sessionTokenField, sessionToken);
    }
    return map;
  }

  static AVUser userFromSinaWeibo(String weiboToken, String userName) {
    AVUser user = newAVUser();
    user.sinaWeiboToken = weiboToken;
    user.username = userName;
    return user;
  }

  static AVUser userFromQQWeibo(String weiboToken, String userName) {
    AVUser user = newAVUser();
    user.qqWeiboToken = weiboToken;
    user.username = userName;
    return user;
  }

  private boolean checkUserAuthentication(final AVCallback callback) {
    if (!this.isAuthenticated() || AVUtils.isBlankString(getObjectId())) {
      if (callback != null) {
        callback.internalDone(AVErrorUtils.createException(AVException.SESSION_MISSING,
            "No valid session token, make sure signUp or login has been called."));
      }
      return false;
    }
    return true;
  }

  /**
   * <p>
   * Follow the user specified by userObjectId. This will create a follow relation between this user
   * and the user specified by the userObjectId.
   * </p>
   * 
   * @param userObjectId The user objectId.
   * @param callback callback.done(user, e) is called when the follow completes.
   * @since 2.1.3
   */
  public void follow(String userObjectId) throws Exception {
    this.follow(userObjectId, null);
  }

  public void follow(String userObjectId, Map<String, Object> attributes) throws AVException {
    follow(true, userObjectId, attributes, new FollowCallback() {

      @Override
      public void done(AVObject object, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(e, null));
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  private void follow(boolean sync, String userObjectId, Map<String, Object> attributes,
      final FollowCallback callback) {
    if (!checkUserAuthentication(callback)) {
      return;
    }
    String endPoint = AVPowerfulUtils.getFollowEndPoint(getObjectId(), userObjectId);
    String paramsString = "";
    if (attributes != null) {
      paramsString = AVUtils.restfulServerData(attributes);
    }
    PaasClient.storageInstance().postObject(endPoint, paramsString, sync,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            super.onSuccess(content, e); // To change body of overridden methods use File | Settings
            // |
            // File Templates.
            if (callback != null) {
              callback.internalDone(AVUser.this, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            super.onFailure(error, content); // To change body of overridden methods use File |
            // Settings
            // | File Templates.
            if (callback != null) {
              callback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        });
  }

  public void unfollow(String userObjectId) throws AVException {
    unfollow(true, userObjectId, new FollowCallback() {

      @Override
      public void done(AVObject object, AVException e) {
        if (e != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(e, null));
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  private void unfollow(boolean sync, String userObjectId, final FollowCallback callback) {
    if (!checkUserAuthentication(callback)) {
      return;
    }
    String endPoint = AVPowerfulUtils.getFollowEndPoint(getObjectId(), userObjectId);
    PaasClient.storageInstance().deleteObject(endPoint, sync, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        super.onSuccess(content, e); // To change body of overridden methods use File | Settings |
        // File Templates.
        if (callback != null) {
          callback.internalDone(AVUser.this, null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        super.onFailure(error, content); // To change body of overridden methods use File | Settings
        // | File Templates.
        if (callback != null) {
          callback.internalDone(null, AVErrorUtils.createException(error, content));
        }
      }
    }, null, null);
  }

  /*
   * {"results": [{"objectId":"52bbd4f9e4b0be6d851ef395",
   * "follower":{"className":"_User","objectId":"52bbd4f8e4b0be6d851ef394","__type":"Pointer"},
   * "user":{"className":"_User","objectId":"52bbd4f3e4b0be6d851ef393","__type":"Pointer"},
   * "createdAt":"2013-12-26T15:04:25.856Z", "updatedAt":"2013-12-26T15:04:25.856Z"},
   * {"objectId":"52bbd4ffe4b0be6d851ef398",
   * "follower":{"className":"_User","objectId":"52bbd4fee4b0be6d851ef397","__type":"Pointer"},
   * "user":{"className":"_User","objectId":"52bbd4f3e4b0be6d851ef393","__type":"Pointer"},
   * "createdAt":"2013-12-26T15:04:31.066Z", "updatedAt":"2013-12-26T15:04:31.066Z"} ] }
   */
  private List<AVUser> processResultByTag(String content, String tag) {
    List<AVUser> list = new LinkedList<AVUser>();
    if (AVUtils.isBlankString(content)) {
      return list;
    }
    AVFollowResponse resp = new AVFollowResponse();
    resp = JSON.parseObject(content, resp.getClass());
    processResultList(resp.results, list, tag);
    return list;
  }

  private Map<String, List<AVUser>> processFollowerAndFollowee(String content) {
    Map<String, List<AVUser>> map = new HashMap<String, List<AVUser>>();
    if (AVUtils.isBlankString(content)) {
      return map;
    }
    AVFollowResponse resp = new AVFollowResponse();
    resp = JSON.parseObject(content, resp.getClass());
    List<AVUser> followers = new LinkedList<AVUser>();
    List<AVUser> followees = new LinkedList<AVUser>();
    processResultList(resp.followers, followers, FOLLOWER_TAG);
    processResultList(resp.followees, followees, FOLLOWEE_TAG);
    map.put(FOLLOWER_TAG, followers);
    map.put(FOLLOWEE_TAG, followees);
    return map;
  }

  // TODO, consider subclass.
  private void processResultList(Map[] results, List<AVUser> list, String tag) {
    for (Map item : results) {
      if (item != null && !item.isEmpty()) {
        AVUser user = (AVUser) AVUtils.getObjectFrom(item.get(tag));
        list.add(user);
      }
    }
  }

  /**
   * <p>
   * 创建follower查询。请确保传入的userObjectId不为空，否则会抛出IllegalArgumentException。
   * 创建follower查询后，您可以使用whereEqualTo("follower", userFollower)查询特定的follower。 您也可以使用skip和limit支持分页操作。
   * 
   * </p>
   * 
   * @param userObjectId 待查询的用户objectId。
   * @param clazz AVUser类或者其子类。
   * 
   * @since 2.3.0
   */
  static public <T extends AVUser> AVQuery<T> followerQuery(final String userObjectId,
      Class<T> clazz) {
    if (AVUtils.isBlankString(userObjectId)) {
      throw new IllegalArgumentException("Blank user objectId.");
    }
    AVFellowshipQuery query = new AVFellowshipQuery<T>("_Follower", clazz);
    query.whereEqualTo("user", AVUser.createWithoutData("_User", userObjectId));
    query.setFriendshipTag(AVUser.FOLLOWER_TAG);
    return query;
  }

  /**
   * <p>
   * 创建follower查询。创建follower查询后，您可以使用whereEqualTo("follower", userFollower)查询特定的follower。
   * 您也可以使用skip和limit支持分页操作。
   * 
   * </p>
   * 
   * @param clazz AVUser类或者其子类。
   * @since 2.3.0
   */
  public <T extends AVUser> AVQuery<T> followerQuery(Class<T> clazz) throws AVException {
    if (AVUtils.isBlankString(this.getObjectId())) {
      throw AVErrorUtils.sessionMissingException();
    }
    return followerQuery(getObjectId(), clazz);
  }

  /**
   * <p>
   * 创建followee查询。请确保传入的userObjectId不为空，否则会抛出IllegalArgumentException。
   * 创建followee查询后，您可以使用whereEqualTo("followee", userFollowee)查询特定的followee。 您也可以使用skip和limit支持分页操作。
   * 
   * </p>
   * 
   * @param userObjectId 待查询的用户objectId。
   * @param clazz AVUser类或者其子类。
   * 
   * @since 2.3.0
   */
  static public <T extends AVUser> AVQuery<T> followeeQuery(final String userObjectId,
      Class<T> clazz) {
    if (AVUtils.isBlankString(userObjectId)) {
      throw new IllegalArgumentException("Blank user objectId.");
    }
    AVFellowshipQuery query = new AVFellowshipQuery<T>("_Followee", clazz);
    query.whereEqualTo("user", AVUser.createWithoutData("_User", userObjectId));
    query.setFriendshipTag(AVUser.FOLLOWEE_TAG);
    return query;
  }

  /**
   * <p>
   * 创建followee查询。 创建followee查询后，您可以使用whereEqualTo("followee", userFollowee)查询特定的followee。
   * 您也可以使用skip和limit支持分页操作。
   * 
   * </p>
   * 
   * @param clazz AVUser类或者其子类。
   * 
   * @since 2.3.0
   */
  public <T extends AVUser> AVQuery<T> followeeQuery(Class<T> clazz) throws AVException {
    if (AVUtils.isBlankString(this.getObjectId())) {
      throw AVErrorUtils.sessionMissingException();
    }
    return followeeQuery(getObjectId(), clazz);
  }

  /**
   * 获取用户好友关系的查询条件，同时包括用户的关注和用户粉丝
   * 
   * @return
   */
  public AVFriendshipQuery friendshipQuery() {
    return this.friendshipQuery(subClazz == null ? AVUser.class : subClazz);
  }

  /**
   * 获取用户好友关系的查询条件，同时包括用户的关注和用户粉丝
   * 
   * @param clazz 最终返回的AVUser的子类
   * @param <T>
   * @return
   */
  public <T extends AVUser> AVFriendshipQuery friendshipQuery(Class<T> clazz) {
    return new AVFriendshipQuery(this.objectId, clazz);
  }

  /**
   * 获取用户好友关系的查询条件，同时包括用户的关注和用户粉丝
   * 
   * @param userId AVUser的objectId
   * @param <T>
   * @return
   */
  public static <T extends AVUser> AVFriendshipQuery<T> friendshipQuery(String userId) {
    return new AVFriendshipQuery(userId, subClazz == null ? AVUser.class : subClazz);
  }

  /**
   * 获取用户好友关系的查询条件，同时包括用户的关注和用户粉丝
   * 
   * @param userId AVUser的objectId
   * @param clazz 指定的AVUser或者其子类
   * @param <T>
   * @return
   */
  public static <T extends AVUser> AVFriendshipQuery friendshipQuery(String userId, Class<T> clazz) {
    return new AVFriendshipQuery(userId, clazz);
  }

  public <T extends AVUser> Map<String, List<T>> getFollowersAndFollowees() throws Exception {
    final Map<String, List<T>> result = new HashMap<String, List<T>>();
    getFollowersAndFolloweesInBackground(true, new FollowersAndFolloweesCallback<T>() {

      @Override
      public void done(Map parseObjects, AVException parseException) {
        if (parseException != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(parseException, null));
        } else {
          result.putAll(parseObjects);
        }
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return result;
  }

  private void getFollowersAndFolloweesInBackground(boolean sync,
      final FollowersAndFolloweesCallback callback) {
    if (!checkUserAuthentication(callback)) {
      return;
    }
    String endPoint = AVPowerfulUtils.getFollowersAndFollowees(getObjectId());
    PaasClient.storageInstance().getObject(endPoint, null, sync, null, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        super.onSuccess(content, e);
        Map<String, List<AVUser>> map = processFollowerAndFollowee(content);
        if (callback != null) {
          callback.internalDone(map, null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        super.onFailure(error, content);
        if (callback != null) {
          callback.internalDone(null, AVErrorUtils.createException(error, content));
        }
      }
    });
  }

  /*
   * 通过这个方法可以将AVUser对象强转为其子类对象
   */
  public static <T extends AVUser> T cast(AVUser user, Class<T> clazz) {
    try {
      T newUser = AVObject.cast(user, clazz);
      return newUser;
    } catch (Exception e) {
      LogUtil.log.e("ClassCast Exception", e);
    }
    return null;
  }

  /**
   * 
   * 通过设置此方法，所有关联对象中的AVUser对象都会被强转成注册的AVUser子类对象
   */

  public static void alwaysUseSubUserClass(Class<? extends AVUser> clazz) {
    subClazz = clazz;
  }

  private static Map<String, Object> authData(AVThirdPartyUserAuth userInfo) {
    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(accessTokenTag, userInfo.accessToken);
    map.put(expiresAtTag, userInfo.expiredAt);
    if (!AVUtils.isBlankString(userInfo.snsType)) {
      map.put(AVThirdPartyUserAuth.platformUserIdTag(userInfo.snsType), userInfo.userId);
    }
    result.put(userInfo.snsType, map);
    return result;
  }



  /**
   * 生成一个新的AarseUser，并且将AVUser与SNS平台获取的userInfo关联。
   * 
   * @param userInfo 包含第三方授权必要信息的内部类
   * @param callback 关联完成后，调用的回调函数。
   */
  static public void loginWithAuthData(AVThirdPartyUserAuth userInfo,
      final LogInCallback<AVUser> callback) {
    loginWithAuthData(AVUser.class, userInfo, callback);
  }

  /**
   * 生成一个新的AVUser子类化对象，并且将该对象与SNS平台获取的userInfo关联。
   * 
   * @param clazz 子类化的AVUer的class对象
   * @param userInfo 在SNS登录成功后，返回的userInfo信息。
   * @param callback 关联完成后，调用的回调函数。
   * @since 1.4.4
   */
  static public <T extends AVUser> void loginWithAuthData(final Class<T> clazz,
      final AVThirdPartyUserAuth userInfo, final LogInCallback<T> callback) {
    if (userInfo == null) {
      if (callback != null) {
        callback.internalDone(null,
            AVErrorUtils.createException(AVException.OTHER_CAUSE, "NULL userInfo."));
      }
      return;
    }

    Map<String, Object> data = new HashMap<String, Object>();
    data.put(authDataTag, authData(userInfo));
    String jsonString = JSON.toJSONString(data);
    PaasClient.storageInstance().postObject("users", jsonString, false, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (e == null) {
              T userObject = AVUser.newAVUser(clazz, callback);
              if (userObject == null) {
                return;
              }
              AVUtils.copyPropertiesFromJsonStringToAVObject(content, userObject);
              userObject.processAuthData(userInfo);
              AVUser.changeCurrentUser(userObject, true);
              if (callback != null) {
                callback.internalDone(userObject, null);
              }
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        }, null, null);
  }

  /**
   * 将现存的AVUser与从SNS平台获取的userInfo关联起来。
   * 
   * @param user AVUser 对象。
   * @param userInfo 在SNS登录成功后，返回的userInfo信息。
   * @throws AVException
   * @since 1.4.4
   */
  static public void associateWithAuthData(AVUser user, AVThirdPartyUserAuth userInfo)
      throws AVException {
    if (userInfo == null) {
      throw AVErrorUtils.createException(AVException.OTHER_CAUSE, "NULL userInfo.");
    }
    Map<String, Object> authData = authData(userInfo);
    if (user.get(authDataTag) != null && user.get(authDataTag) instanceof Map) {
      authData.putAll((Map<String, Object>) user.get(authDataTag));
    }
    user.put(authDataTag, authData);
    user.markAnonymousUserTransfer();
    user.save();
  }

  static public void dissociateAuthData(final AVUser user, final String type) throws AVException {
    Map<String, Object> authData = (Map<String, Object>) user.get(authDataTag);
    if (authData != null) {
      authData.remove(type);
    }
    user.put(authDataTag, authData);
    if (user.isAuthenticated() && !AVUtils.isBlankString(user.getObjectId())) {
      user.save();
      user.processAuthData(new AVThirdPartyUserAuth(null, null, type, null));

    } else {
      throw new AVException(AVException.SESSION_MISSING, "the user object missing a valid session");
    }
  }

  private static final String accessTokenTag = "access_token";
  private static final String expiresAtTag = "expires_at";
  private static final String authDataTag = "authData";
  private static final String anonymousTag = "anonymous";

  protected void processAuthData(AVThirdPartyUserAuth auth) {
    Map<String, Object> authData = (Map<String, Object>) this.get(authDataTag);
    // 匿名用户转化为正式用户
    if (needTransferFromAnonymousUser) {
      if (authData != null && authData.containsKey(anonymousTag)) {
        authData.remove(anonymousTag);
      } else {
        anonymous = false;
      }
      needTransferFromAnonymousUser = false;
    }
    if (authData != null) {
      if (authData.containsKey(AVThirdPartyUserAuth.SNS_SINA_WEIBO)) {
        Map<String, Object> sinaAuthData =
            (Map<String, Object>) authData.get(AVThirdPartyUserAuth.SNS_SINA_WEIBO);
        this.sinaWeiboToken = (String) sinaAuthData.get(accessTokenTag);
      } else {
        this.sinaWeiboToken = null;
      }
      if (authData.containsKey(AVThirdPartyUserAuth.SNS_TENCENT_WEIBO)) {
        Map<String, Object> qqAuthData =
            (Map<String, Object>) authData.get(AVThirdPartyUserAuth.SNS_TENCENT_WEIBO);
        this.qqWeiboToken = (String) qqAuthData.get(accessTokenTag);
      } else {
        this.qqWeiboToken = null;
      }
      if (authData.containsKey(anonymousTag)) {
        this.anonymous = true;
      } else {
        this.anonymous = false;
      }
    }
    if (auth != null) {
      if (auth.snsType.equals(AVThirdPartyUserAuth.SNS_SINA_WEIBO)) {
        sinaWeiboToken = auth.accessToken;
        return;
      }
      if (auth.snsType.equals(AVThirdPartyUserAuth.SNS_TENCENT_WEIBO)) {
        qqWeiboToken = auth.accessToken;
        return;
      }
    }
  }

  public static class AVThirdPartyUserAuth {
    String accessToken;
    String expiredAt;
    String snsType;
    String userId;

    public static final String SNS_TENCENT_WEIBO = "qq";
    public static final String SNS_SINA_WEIBO = "weibo";
    public static final String SNS_TENCENT_WEIXIN = "weixin";

    public AVThirdPartyUserAuth(String accessToken, String expiredAt, String snstype, String userId) {
      this.accessToken = accessToken;
      this.snsType = snstype;
      this.expiredAt = expiredAt;
      this.userId = userId;
    }

    protected static String platformUserIdTag(String type) {
      if (SNS_TENCENT_WEIBO.equalsIgnoreCase(type) || SNS_TENCENT_WEIXIN.equalsIgnoreCase(type)) {
        return "openid";
      } else {
        return "uid";
      }
    }

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getExpireAt() {
      return expiredAt;
    }

    public void setExpireAt(String expireAt) {
      this.expiredAt = expireAt;
    }

    public String getSnsType() {
      return snsType;
    }

    public void setSnsType(String snsType) {
      this.snsType = snsType;
    }
  }

  private void markAnonymousUserTransfer() {
    if (isAnonymous()) {
      needTransferFromAnonymousUser = true;
    }
  }
}
