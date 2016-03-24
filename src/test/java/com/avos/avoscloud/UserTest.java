package com.avos.avoscloud;

import java.util.List;
import java.util.Random;

import com.avos.avoscloud.data.SubUser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UserTest extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public UserTest(String testName) {
    super(testName);
  }

  @Override
  public void setUp() {
    TestApp.init();
    try {
      AVUser testUser = new AVUser();
      testUser.setUsername("zengzhu");
      testUser.setPassword("12345678");
      testUser.signUp();
    } catch (AVException e) {

    }
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(UserTest.class);
  }

  public void testASignUp() throws Exception {
    String username;
    String password;
    int min = 1;
    int max = 10000;
    int range = max - min + 1;
    AVUser user = new AVUser();
    Random random = new Random();
    user.setUsername(String.format("myusername%d", random.nextInt(range) + min));
    password = String.format("mypassword%d", random.nextInt(range) + min);
    user.setPassword(password);
    user.setEmail(String.format("myeamil%s@aaa.com", random.nextInt(range) + min));
    user.signUp();
    assertFalse(user.getObjectId().isEmpty());
    username = user.getUsername();
    String email = user.getEmail();

    // signup twice must fail
    AVUser anotherUser = new AVUser();
    anotherUser.setUsername(user.getUsername());
    anotherUser.setPassword(password);
    try {
      anotherUser.signUp();
    } catch (AVException e) {
      assertNotNull(e);
    }
  }

  public void testBLogout() {
    AVUser.enableAutomaticUser();
    AVUser userA = AVUser.getCurrentUser();
    AVUser.logOut();
    AVUser userB = AVUser.getCurrentUser();
    assertTrue(userA != userB);

    /*
     * test with parse sdk, it returns two different instances. AVUser user =
     * AVUser.getCurrentUser(); AVUser.logOut(); user = AVUser.getCurrentUser();
     */
  }

  public void testCLogIn() throws Exception {
    AVUser.logIn("zengzhu", "12345678");
    AVUser user = AVUser.getCurrentUser();
    assertTrue(!user.getObjectId().isEmpty());
    assertFalse(user.isMobilePhoneVerified());
    assertNull(user.getString("password"));
  }

  public void testUserChangeName() throws Exception {
    AVUser.logOut();
    AVAnonymousUtils.logIn();
    String username = "user";
    username += AVUtils.getRandomString(8);
    AVUser user = AVUser.getCurrentUser();
    user.setUsername(username);
    user.save();
    assertTrue(user.getUsername().equals(username));
    assertTrue(AVUser.getCurrentUser().getUsername().equals(username));

    String anotherName = "user2" + AVUtils.getRandomString(8);
    user.setUsername(anotherName);
    user.save();
    assertTrue(user.getUsername().equals(anotherName));
    assertTrue(AVUser.getCurrentUser().getUsername().equals(anotherName));
    user.delete();
  }

  public void testAnonymousLogIn() throws Exception {
    AVUser user = AVAnonymousUtils.logIn();
    assertNotNull(user);
    assertTrue(user.isAuthenticated());
    assertTrue(user.isAnonymous());

    user.fetch();
    assertTrue(user.isAnonymous());
  }

  public void testResetPassword() throws Exception {
    final String email = "test@zhuzeng.com";
    try {
      AVUser user = new AVUser();
      user.setUsername("zengzhu");
      user.setPassword("12345678");
      user.setEmail(email);
      user.signUp();
    } catch (Exception e) {
      // ignore
    }

    AVUser.requestPasswordReset(email);
  }

  public void testVerifyEmail() throws Exception {
    final String email = "test@zhuzeng.com";
    try {
      AVUser user = new AVUser();
      user.setUsername("zengzhu");
      user.setPassword("12345678");
      user.setEmail(email);
      user.signUp();
    } catch (AVException e) {

    }
    AVUser.requestEmailVerify(email);
  }

  public void testCurrentUserWithRelation() throws Exception {
    // testCurrentUser();
    AVObject post = new AVObject("userPost");
    post.put("key", "value");
    post.save();

    AVUser.logOut();
    AVUser.logIn("zengzhu", "12345678");
    AVUser user = AVUser.getCurrentUser();
    user.getRelation("likes").add(new AVQuery("userPost").getFirst());
    user.save();

    AVRelation relation = user.getRelation("likes");
    relation.setTargetClass("userPost");
    relation.remove(new AVQuery("userPost").getFirst());
    assertNotNull(relation);
    assertTrue(relation.getQuery().find().size() > 0);

    SubUser subUser = AVUser.getCurrentUser(SubUser.class);
    assertNotNull(subUser);
    SubUser castedUser = AVUser.cast(AVUser.getCurrentUser(), SubUser.class);
    assertNotNull(castedUser);
  }

  public void testUserAvatarA() throws Exception {
    AVUser.logOut();
    byte d[] = new byte[1024];
    final AVFile avatarFile = new AVFile("avavtar", d);

    avatarFile.save();

    final AVFile themeFile = new AVFile("theme", d);

    themeFile.save();

    // rm metadata for Exception test
    avatarFile.addMetaData("size", null);

    AVUser.logIn("zengzhu", "12345678");
    AVUser currentUser = AVUser.getCurrentUser();
    currentUser.put("androidAvatar", avatarFile);
    currentUser.put("androidTheme", themeFile);
    currentUser.save();

    AVUser.logOut();
    AVUser.logIn("zengzhu", "12345678");
    AVUser resultUser = AVUser.getCurrentUser();
    AVFile resultAvatarFile = (AVFile) resultUser.get("androidAvatar");
    AVFile resultThemeFile = (AVFile) resultUser.get("androidTheme");
    assertNotNull(resultAvatarFile);
    assertNotNull(resultThemeFile);
    assertEquals(resultAvatarFile.getObjectId(), avatarFile.getObjectId());
    assertEquals(resultAvatarFile.getUrl(), avatarFile.getUrl());
    assertEquals(resultThemeFile.getObjectId(), themeFile.getObjectId());
    assertEquals(resultThemeFile.getUrl(), themeFile.getUrl());
  }

  private boolean containsUser(List parseObjects, AVUser target) {
    assertNotNull(parseObjects);
    boolean found = false;
    for (int i = 0; i < parseObjects.size(); ++i) {
      AVUser user = (AVUser) parseObjects.get(i);
      assertTrue(user instanceof AVUser);
      if (user.getObjectId().equals(target.getObjectId())) {
        found = true;
        break;
      }
    }
    return found;
  }

  public void testUserFollow() throws Exception {
    // b follows a
    final AVUser userA = new AVUser();
    String passwordA = AVUtils.getRandomString(5);
    userA.setUsername(AVUtils.getRandomString(8));
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    String passwordB = AVUtils.getRandomString(5);
    userB.setUsername(AVUtils.getRandomString(8));
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);

    // try to follow aggin.
    try {
      userB.follow(userObjectId);
    } catch (AVException e) {
      assertTrue(e.getCode() == AVException.DUPLICATE_VALUE);
    }

    final AVUser userC = new AVUser();
    String passwordC = AVUtils.getRandomString(5);
    userC.setUsername(AVUtils.getRandomString(8));
    userC.setPassword(passwordC);
    userC.signUp();
    userC.follow(userObjectId);

    // Query again
    List<AVUser> parseObjects = AVUser.followerQuery(userA.getObjectId(), AVUser.class).find();

    assertTrue(parseObjects.size() > 0);
    assertTrue(containsUser(parseObjects, userB));

    // Query from
    parseObjects = AVUser.followeeQuery(userB.getObjectId(), AVUser.class).find();
    assertTrue(parseObjects.size() > 0);
    assertTrue(containsUser(parseObjects, userA));
    userC.delete();
    AVUser.logIn(userA.getUsername(), passwordA);
    AVUser.getCurrentUser().delete();
    AVUser.logIn(userB.getUsername(), passwordB);
    AVUser.getCurrentUser().delete();
  }

  public void testUserUnfollow() throws Exception {

    // setup follow relation at first. b follows a
    final int maxLimit = 20;
    String usernameA = AVUtils.getRandomString(maxLimit / 4) + System.currentTimeMillis();
    String passwordA = AVUtils.getRandomString(maxLimit);
    String usernameB = AVUtils.getRandomString(maxLimit / 4) + System.currentTimeMillis();
    String passwordB = AVUtils.getRandomString(maxLimit);
    final AVUser userA = new AVUser();
    userA.setUsername(usernameA);
    userA.setPassword(passwordA);
    userA.signUp();

    String userAObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(usernameB);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userAObjectId);

    // Query again
    List<AVUser> parseObjects = AVUser.followerQuery(userAObjectId, AVUser.class).find();

    assertTrue(parseObjects.size() > 0);
    assertTrue(containsUser(parseObjects, userB));

    // Query from
    parseObjects = userB.followeeQuery(AVUser.class).find();
    assertTrue(parseObjects.size() > 0);
    assertTrue(containsUser(parseObjects, userA));

    // try to remove.
    userB.unfollow(userA.getObjectId());


    // try to unfollow again
    userB.unfollow(userA.getObjectId());

    // Query from a's follower list.
    parseObjects = AVUser.followerQuery(userAObjectId, AVUser.class).find();
    assertTrue(parseObjects.size() <= 0);
    assertFalse(containsUser(parseObjects, userB));

    // from b followee list.
    parseObjects = userB.followeeQuery(AVUser.class).find();
    assertTrue(parseObjects.size() <= 0);
    assertFalse(containsUser(parseObjects, userA));

    AVUser.logIn(usernameA, passwordA);
    AVUser.getCurrentUser().delete();
    AVUser.logIn(usernameB, passwordB);
    AVUser.getCurrentUser().delete();
    AVUser.logOut();
  }

  public void testUpdateUserPassword() throws Exception {
    AVUser.logOut();
    AVUser zhulaoban = AVUser.createWithoutData(AVUser.class, "jakjdkfadf");
    try {
      zhulaoban.updatePassword("1234567890", "12345678");
    } catch (AVException e) {
      assertNotNull(e);
    }
    zhulaoban = AVUser.logIn("zengzhu", "12345678");
    zhulaoban.updatePassword("12345678", "12345678");

    // 测试是否更新了 sessionToken，若控制台设置“修改密码后 sessionToken失效”，此处仍应该成功
    zhulaoban.setEmail("hahahhahha@hahahah.com");
    zhulaoban.save();
  }

  public void testAVUserGeoPointer() throws Exception {
    AVGeoPoint userGeoPoint = new AVGeoPoint(41.0, -31.0);
    AVUser.logIn("zengzhu", "12345678");
    AVUser user = AVUser.getCurrentUser();
    user.put("location", userGeoPoint);
    user.save();
    AVUser u = AVUser.createWithoutData(AVUser.class, user.getObjectId());
    u.fetch();
    AVGeoPoint p = u.getAVGeoPoint("location");
    assertEquals(41.0, p.getLatitude());

    SubUser subUser = AVUser.getCurrentUser(SubUser.class);
    p = subUser.getAVGeoPoint("location");
    assertEquals(41.0, p.getLatitude());
  }

  public void testSubUserDeserialize() throws Exception {
    SubUser.logIn("zengzhu", "12345678");
    String str = AVUser.getCurrentUser(SubUser.class).toString();
    SubUser user = (SubUser) AVObject.parseAVObject(str);
    assertEquals("zengzhu", user.getUsername());
    assertTrue(user.isAuthenticated());
    user = AVUser.getCurrentUser(SubUser.class);
    assertEquals("zengzhu", user.getUsername());
    assertTrue(user.isAuthenticated());
  }
}
